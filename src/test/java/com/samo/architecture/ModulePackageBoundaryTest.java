package com.samo.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ModulePackageBoundaryTest {
    private static final List<String> MODULES = List.of(
        "engine-core",
        "engine-platform-lwjgl",
        "engine-render-opengl",
        "engine-ui",
        "engine-assets",
        "engine-world",
        "engine-physics-jolt",
        "engine-audio-openal",
        "engine-network-api",
        "engine-network-ip",
        "engine-steam",
        "engine-editor",
        "game-sandbox",
        "game-client",
        "game-server",
        "test-support"
    );
    private static final Pattern PACKAGE_PATTERN = Pattern.compile(
        "^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;"
    );
    private static final Pattern IMPORT_PATTERN = Pattern.compile(
        "^\\s*import\\s+(?:static\\s+)?([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$*]*)+)\\s*;"
    );

    @Test
    void everyDeclaredModuleHasValidPackageBoundaryRoots() throws IOException {
        Map<String, Boundary> boundaries = loadBoundaries();

        assertThat(boundaries.keySet()).containsExactlyElementsOf(MODULES);
        boundaries.forEach((module, boundary) -> {
            assertThat(boundary.apiRoot())
                .as("%s API root", module)
                .startsWith(boundary.moduleRoot() + ".");
            assertThat(boundary.internalRoot())
                .as("%s internal root", module)
                .startsWith(boundary.moduleRoot() + ".");
            assertThat(boundary.apiRoot())
                .as("%s API/internal roots must differ", module)
                .isNotEqualTo(boundary.internalRoot());
        });
    }

    @Test
    void modulesOnlyImportOtherModulesThroughDeclaredApiRoots() throws IOException {
        Map<String, Boundary> boundaries = loadBoundaries();
        List<Violation> violations = new ArrayList<>();

        for (String module : MODULES) {
            Path sourceRoot = repositoryRoot().resolve(module).resolve("src/main/java");
            scanSourceTree(module, sourceRoot, boundaries, violations);
        }

        if (Boolean.getBoolean("architecture.includeInvalidFixture")) {
            Path fixture = repositoryRoot().resolve(
                "config/architecture/fixtures/ForbiddenGameToPlatformShortcut.java"
            );
            scanSourceFile("game-client", fixture, boundaries, violations);
        }

        assertThat(violations)
            .as("Cross-module implementation imports must use the target module API root")
            .isEmpty();
    }

    private static Map<String, Boundary> loadBoundaries() throws IOException {
        Properties properties = new Properties();
        Path registry = repositoryRoot().resolve("config/architecture/module-boundaries.properties");
        try (Reader reader = Files.newBufferedReader(registry)) {
            properties.load(reader);
        }

        Map<String, Boundary> boundaries = new LinkedHashMap<>();
        for (String module : MODULES) {
            boundaries.put(module, new Boundary(
                required(properties, module + ".root"),
                required(properties, module + ".api"),
                required(properties, module + ".internal")
            ));
        }

        assertThat(properties.stringPropertyNames())
            .containsExactlyInAnyOrderElementsOf(expectedPropertyNames());
        return boundaries;
    }

    private static List<String> expectedPropertyNames() {
        List<String> names = new ArrayList<>();
        for (String module : MODULES) {
            names.add(module + ".root");
            names.add(module + ".api");
            names.add(module + ".internal");
        }
        return names;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        assertThat(value).as("Missing package-boundary property %s", key).isNotBlank();
        return value.trim();
    }

    private static void scanSourceTree(
        String sourceModule,
        Path sourceRoot,
        Map<String, Boundary> boundaries,
        List<Violation> violations
    ) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return;
        }

        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                .sorted()
                .forEach(path -> scanUnchecked(sourceModule, path, boundaries, violations));
        }
    }

    private static void scanUnchecked(
        String sourceModule,
        Path sourceFile,
        Map<String, Boundary> boundaries,
        List<Violation> violations
    ) {
        try {
            scanSourceFile(sourceModule, sourceFile, boundaries, violations);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan " + sourceFile, exception);
        }
    }

    private static void scanSourceFile(
        String sourceModule,
        Path sourceFile,
        Map<String, Boundary> boundaries,
        List<Violation> violations
    ) throws IOException {
        List<String> lines = Files.readAllLines(sourceFile);
        validateOwnedPackage(sourceModule, sourceFile, lines, boundaries.get(sourceModule));

        for (int index = 0; index < lines.size(); index++) {
            Matcher matcher = IMPORT_PATTERN.matcher(lines.get(index));
            if (!matcher.find()) {
                continue;
            }

            String importedName = matcher.group(1).replace(".*", "");
            BoundaryTarget target = findTarget(importedName, sourceModule, boundaries);
            if (target != null && !isWithin(importedName, target.boundary().apiRoot())) {
                violations.add(new Violation(
                    sourceModule,
                    target.module(),
                    repositoryRoot().relativize(sourceFile).toString(),
                    index + 1,
                    importedName,
                    target.boundary().apiRoot()
                ));
            }
        }
    }

    private static void validateOwnedPackage(
        String sourceModule,
        Path sourceFile,
        List<String> lines,
        Boundary boundary
    ) {
        for (String line : lines) {
            Matcher matcher = PACKAGE_PATTERN.matcher(line);
            if (matcher.find()) {
                assertThat(isWithin(matcher.group(1), boundary.moduleRoot()))
                    .as("%s source %s must stay under package root %s",
                        sourceModule,
                        repositoryRoot().relativize(sourceFile),
                        boundary.moduleRoot())
                    .isTrue();
                return;
            }
        }
    }

    private static BoundaryTarget findTarget(
        String importedName,
        String sourceModule,
        Map<String, Boundary> boundaries
    ) {
        return boundaries.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(sourceModule))
            .filter(entry -> isWithin(importedName, entry.getValue().moduleRoot()))
            .sorted(Comparator.comparingInt(
                (Map.Entry<String, Boundary> entry) -> entry.getValue().moduleRoot().length()
            ).reversed())
            .map(entry -> new BoundaryTarget(entry.getKey(), entry.getValue()))
            .findFirst()
            .orElse(null);
    }

    private static boolean isWithin(String packageOrType, String packageRoot) {
        return packageOrType.equals(packageRoot) || packageOrType.startsWith(packageRoot + ".");
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private record Boundary(String moduleRoot, String apiRoot, String internalRoot) {
    }

    private record BoundaryTarget(String module, Boundary boundary) {
    }

    private record Violation(
        String sourceModule,
        String targetModule,
        String file,
        int line,
        String importedName,
        String requiredApiRoot
    ) {
        @Override
        public String toString() {
            return sourceModule + " -> " + targetModule + " at " + file + ":" + line
                + " imports " + importedName + "; expected target API root " + requiredApiRoot;
        }
    }
}
