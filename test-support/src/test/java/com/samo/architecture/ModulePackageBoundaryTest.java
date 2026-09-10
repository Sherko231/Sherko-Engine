package com.samo.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModulePackageBoundaryTest {
    private static final String MODULES_PROPERTY = "repository.modules";

    @Test
    void everyDeclaredModuleHasValidPackageBoundaryRoots() throws IOException {
        List<String> modules = declaredModules();
        Map<String, Boundary> boundaries = loadBoundaries(modules);

        assertEquals(new LinkedHashSet<>(modules), boundaries.keySet());
        modules.forEach(module -> assertTrue(
            Files.isDirectory(repositoryRoot().resolve(module)),
            module + " from " + MODULES_PROPERTY + " must resolve to a project directory"
        ));
        boundaries.forEach((module, boundary) -> {
            assertTrue(
                boundary.apiRoot().startsWith(boundary.moduleRoot() + "."),
                module + " API root must be below its module root"
            );
            assertTrue(
                boundary.internalRoot().startsWith(boundary.moduleRoot() + "."),
                module + " internal root must be below its module root"
            );
            assertFalse(
                boundary.apiRoot().equals(boundary.internalRoot()),
                module + " API/internal roots must differ"
            );
        });
    }

    @Test
    void modulesOnlyReferenceOtherModulesThroughDeclaredApiRoots() throws IOException {
        List<String> modules = declaredModules();
        Map<String, Boundary> boundaries = loadBoundaries(modules);
        List<Violation> violations = new ArrayList<>();

        for (String module : modules) {
            Path moduleRoot = repositoryRoot().resolve(module);
            scanSourceTree(
                module,
                moduleRoot.resolve("src/main/java"),
                true,
                boundaries,
                violations
            );
            scanSourceTree(
                module,
                moduleRoot.resolve("src/test/java"),
                false,
                boundaries,
                violations
            );
        }

        if (Boolean.getBoolean("architecture.includeInvalidFixture")) {
            Path fixture = repositoryRoot().resolve(
                "config/architecture/fixtures/ForbiddenGameToPlatformShortcut.java"
            );
            scanSourceFile("game-client", fixture, true, boundaries, violations);
        }

        assertTrue(
            violations.isEmpty(),
            () -> "Cross-module source references must use the target module API root:\n"
                + violations.stream().map(Violation::toString).reduce("", (left, right) -> left + right + "\n")
        );
    }

    @Test
    void fullyQualifiedImplementationReferenceIsRejected(@TempDir Path tempDirectory) throws IOException {
        Path sourceFile = tempDirectory.resolve("FullyQualifiedShortcut.java");
        Files.writeString(sourceFile, """
            package example.source.api;

            final class FullyQualifiedShortcut {
                private example.target.internal.Secret secret;
            }
            """);

        List<Violation> violations = new ArrayList<>();
        scanSourceFile("source", sourceFile, true, sampleBoundaries(), violations);

        assertEquals(1, violations.size());
        Violation violation = violations.getFirst();
        assertEquals("target", violation.targetModule());
        assertEquals("example.target.internal.Secret", violation.referencedName());
    }

    @Test
    void productionSourceWithoutPackageIsRejected(@TempDir Path tempDirectory) throws IOException {
        Path sourceFile = tempDirectory.resolve("MissingPackage.java");
        Files.writeString(sourceFile, "final class MissingPackage {}\n");

        AssertionError error = assertThrows(
            AssertionError.class,
            () -> scanSourceFile("source", sourceFile, true, sampleBoundaries(), new ArrayList<>())
        );

        assertTrue(error.getMessage().contains("must declare a package"));
    }

    @Test
    void mostSpecificModuleRootOwnsNestedNetworkPackage() {
        Map<String, Boundary> boundaries = new LinkedHashMap<>();
        boundaries.put("engine-network-api", new Boundary(
            "com.samo.engine.network",
            "com.samo.engine.network.api",
            "com.samo.engine.network.internal"
        ));
        boundaries.put("engine-network-ip", new Boundary(
            "com.samo.engine.network.ip",
            "com.samo.engine.network.ip.api",
            "com.samo.engine.network.ip.internal"
        ));

        BoundaryTarget target = findTarget(
            "com.samo.engine.network.ip.internal.UdpTransport",
            "game-client",
            boundaries
        );

        assertNotNull(target);
        assertEquals("engine-network-ip", target.module());
    }

    private static List<String> declaredModules() {
        String configuredModules = System.getProperty(MODULES_PROPERTY);
        assertNotNull(
            configuredModules,
            MODULES_PROPERTY + " must be supplied by the test-support Gradle test task"
        );

        List<String> modules = Arrays.stream(configuredModules.split(","))
            .map(String::trim)
            .filter(module -> !module.isEmpty())
            .toList();

        assertFalse(modules.isEmpty(), MODULES_PROPERTY + " must contain at least one module");
        assertEquals(
            modules.size(),
            new LinkedHashSet<>(modules).size(),
            MODULES_PROPERTY + " must not contain duplicate module names"
        );
        return modules;
    }

    private static Map<String, Boundary> loadBoundaries(List<String> modules) throws IOException {
        Properties properties = new Properties();
        Path registry = repositoryRoot().resolve("config/architecture/module-boundaries.properties");
        try (Reader reader = Files.newBufferedReader(registry)) {
            properties.load(reader);
        }

        Map<String, Boundary> boundaries = new LinkedHashMap<>();
        for (String module : modules) {
            boundaries.put(module, new Boundary(
                required(properties, module + ".root"),
                required(properties, module + ".api"),
                required(properties, module + ".internal")
            ));
        }

        assertEquals(expectedPropertyNames(modules), properties.stringPropertyNames());
        return boundaries;
    }

    private static Set<String> expectedPropertyNames(List<String> modules) {
        Set<String> names = new LinkedHashSet<>();
        for (String module : modules) {
            names.add(module + ".root");
            names.add(module + ".api");
            names.add(module + ".internal");
        }
        return names;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        assertNotNull(value, "Missing package-boundary property " + key);
        assertFalse(value.isBlank(), "Blank package-boundary property " + key);
        return value.trim();
    }

    private static void scanSourceTree(
        String sourceModule,
        Path sourceRoot,
        boolean validatePackageOwnership,
        Map<String, Boundary> boundaries,
        List<Violation> violations
    ) throws IOException {
        if (!Files.isDirectory(sourceRoot)) {
            return;
        }

        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                .sorted()
                .forEach(path -> scanUnchecked(
                    sourceModule,
                    path,
                    validatePackageOwnership,
                    boundaries,
                    violations
                ));
        }
    }

    private static void scanUnchecked(
        String sourceModule,
        Path sourceFile,
        boolean validatePackageOwnership,
        Map<String, Boundary> boundaries,
        List<Violation> violations
    ) {
        try {
            scanSourceFile(
                sourceModule,
                sourceFile,
                validatePackageOwnership,
                boundaries,
                violations
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan " + sourceFile, exception);
        }
    }

    private static void scanSourceFile(
        String sourceModule,
        Path sourceFile,
        boolean validatePackageOwnership,
        Map<String, Boundary> boundaries,
        List<Violation> violations
    ) throws IOException {
        ParsedSource source = parseSource(sourceFile);
        if (validatePackageOwnership) {
            validateOwnedPackage(sourceModule, sourceFile, source.packageName(), boundaries.get(sourceModule));
        }

        for (SourceReference reference : source.references()) {
            BoundaryTarget target = findTarget(reference.name(), sourceModule, boundaries);
            if (target != null && !isWithin(reference.name(), target.boundary().apiRoot())) {
                violations.add(new Violation(
                    sourceModule,
                    target.module(),
                    displayPath(sourceFile),
                    reference.line(),
                    reference.name(),
                    target.boundary().apiRoot()
                ));
            }
        }
    }

    private static ParsedSource parseSource(Path sourceFile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Architecture verification requires a JDK compiler");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavTaskFactory.create(
                compiler,
                fileManager,
                diagnostics,
                sourceFile
            ));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);

            String parseErrors = diagnostics.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .map(Diagnostic::toString)
                .reduce("", (left, right) -> left + right + "\n");
            assertTrue(
                parseErrors.isEmpty(),
                () -> "Unable to parse " + displayPath(sourceFile) + ":\n" + parseErrors
            );
            assertEquals(1, units.size(), "Expected one compilation unit for " + displayPath(sourceFile));

            CompilationUnitTree unit = units.getFirst();
            Trees trees = Trees.instance(task);
            SourcePositions positions = trees.getSourcePositions();
            Set<SourceReference> references = new LinkedHashSet<>();

            for (ImportTree imported : unit.getImports()) {
                String name = imported.getQualifiedIdentifier().toString().replaceAll("\\.\\*$", "");
                references.add(new SourceReference(name, lineNumber(unit, positions, imported)));
            }

            TreeScanner<Void, Void> scanner = new TreeScanner<>() {
                @Override
                public Void visitMemberSelect(MemberSelectTree selected, Void unused) {
                    String name = qualifiedName(selected);
                    if (name != null) {
                        references.add(new SourceReference(
                            name,
                            lineNumber(unit, positions, selected)
                        ));
                        return null;
                    }
                    return super.visitMemberSelect(selected, unused);
                }
            };
            for (Tree declaration : unit.getTypeDecls()) {
                scanner.scan(declaration, null);
            }

            String packageName = unit.getPackageName() == null
                ? null
                : unit.getPackageName().toString();
            return new ParsedSource(packageName, List.copyOf(references));
        }
    }

    private static int lineNumber(
        CompilationUnitTree unit,
        SourcePositions positions,
        Tree sourceTree
    ) {
        long position = positions.getStartPosition(unit, sourceTree);
        if (position == Diagnostic.NOPOS) {
            return 0;
        }
        return (int) unit.getLineMap().getLineNumber(position);
    }

    private static String qualifiedName(Tree tree) {
        if (tree instanceof IdentifierTree identifier) {
            return identifier.getName().toString();
        }
        if (tree instanceof MemberSelectTree selected) {
            String expression = qualifiedName(selected.getExpression());
            if (expression != null) {
                return expression + "." + selected.getIdentifier();
            }
        }
        return null;
    }

    private static void validateOwnedPackage(
        String sourceModule,
        Path sourceFile,
        String packageName,
        Boundary boundary
    ) {
        assertNotNull(
            packageName,
            sourceModule + " production source " + displayPath(sourceFile)
                + " must declare a package under " + boundary.moduleRoot()
        );
        assertTrue(
            isWithin(packageName, boundary.moduleRoot()),
            sourceModule + " production source " + displayPath(sourceFile)
                + " must stay under package root " + boundary.moduleRoot()
        );
    }

    private static BoundaryTarget findTarget(
        String referencedName,
        String sourceModule,
        Map<String, Boundary> boundaries
    ) {
        return boundaries.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(sourceModule))
            .filter(entry -> isWithin(referencedName, entry.getValue().moduleRoot()))
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

    private static Map<String, Boundary> sampleBoundaries() {
        Map<String, Boundary> boundaries = new LinkedHashMap<>();
        boundaries.put("source", new Boundary(
            "example.source",
            "example.source.api",
            "example.source.internal"
        ));
        boundaries.put("target", new Boundary(
            "example.target",
            "example.target.api",
            "example.target.internal"
        ));
        return boundaries;
    }

    private static String displayPath(Path sourceFile) {
        Path normalized = sourceFile.toAbsolutePath().normalize();
        Path root = repositoryRoot();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString();
        }
        return normalized.toString();
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("repository.root")).toAbsolutePath().normalize();
    }

    private record Boundary(String moduleRoot, String apiRoot, String internalRoot) {
    }

    private record BoundaryTarget(String module, Boundary boundary) {
    }

    private record ParsedSource(String packageName, List<SourceReference> references) {
    }

    private record SourceReference(String name, int line) {
    }

    private record Violation(
        String sourceModule,
        String targetModule,
        String file,
        int line,
        String referencedName,
        String requiredApiRoot
    ) {
        @Override
        public String toString() {
            return sourceModule + " -> " + targetModule + " at " + file + ":" + line
                + " references " + referencedName + "; expected target API root " + requiredApiRoot;
        }
    }

    private static final class JavTaskFactory {
        private JavTaskFactory() {
        }

        private static JavacTask create(
            JavaCompiler compiler,
            StandardJavaFileManager fileManager,
            DiagnosticCollector<JavaFileObject> diagnostics,
            Path sourceFile
        ) {
            return (JavacTask) compiler.getTask(
                null,
                fileManager,
                diagnostics,
                List.of("-proc:none"),
                null,
                fileManager.getJavaFileObjects(sourceFile.toFile())
            );
        }
    }
}
