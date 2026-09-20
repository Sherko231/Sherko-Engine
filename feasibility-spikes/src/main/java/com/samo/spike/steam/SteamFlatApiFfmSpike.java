package com.samo.spike.steam;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class SteamFlatApiFfmSpike {
    private static final String REDISTRIBUTABLE_RESOURCE = "/steam_api64.dll";
    private static final int STEAM_ERROR_MESSAGE_BYTES = 1024;
    private static final int STEAM_API_INIT_OK = 0;

    private static final Set<Integer> KNOWN_AVAILABILITY_RESULTS = Set.of(-102, // CannotTry
        -101, // Failed
        -100, // Previously
        -10, // Retrying
        0, // Unknown / sentinel
        1, // NeverTried
        2, // Waiting
        3, // Attempting
        100 // Current
    );

    private SteamFlatApiFfmSpike() {

    }

    public static void main(String[] args) throws Throwable {

        requireWindowsX64();

        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path redistributable = resolveRedistributable(workingDirectory);

        System.out.println("P0-T09 Steam flat API FFM spike");
        System.out.println("Steam redistributable : " + redistributable);
        System.out.println("FFM implementation    : java.lang.foreign (Java " + Runtime.version().feature() + ")");

        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup steamApi = SymbolLookup.libraryLookup(redistributable, arena);
            Linker linker = Linker.nativeLinker();

            MethodHandle steamInitFlat = linker.downcallHandle(steamApi.findOrThrow("SteamAPI_InitFlat"), FunctionDescriptor.of(JAVA_INT, ADDRESS));
            MethodHandle steamShutdown = linker.downcallHandle(steamApi.findOrThrow("SteamAPI_Shutdown"), FunctionDescriptor.ofVoid());

            MemorySegment errorMessage = arena.allocate(STEAM_ERROR_MESSAGE_BYTES);
            errorMessage.fill((byte) 0);

            int initResult = (int) steamInitFlat.invokeExact(errorMessage);
            if (initResult != STEAM_API_INIT_OK) {
                throw new IllegalStateException("SteamAPI_InitFlat failed with result=" + initResult + " (" + initResultName(initResult) + "). "
                    + "Make sure the Steam desktop client is running and logged in.");
            }

            System.out.println("SteamAPI_InitFlat    : OK");

            try {
                Accessor accessor = findNetworkingSocketsAccessor(steamApi);
                MethodHandle getNetworkingSockets = linker.downcallHandle(accessor.symbol(), FunctionDescriptor.of(ADDRESS));

                MemorySegment networkingSockets = (MemorySegment) getNetworkingSockets.invokeExact();
                if (networkingSockets.address() == 0L) {
                    throw new IllegalStateException(accessor.name() + " returned a null ISteamNetworkingSockets pointer");
                }

                System.out.println("Sockets accessor     : " + accessor.name());
                System.out.printf("Sockets pointer      : 0x%x%n", networkingSockets.address());

                MethodHandle initAuthentication = linker.downcallHandle(steamApi.findOrThrow("SteamAPI_ISteamNetworkingSockets_InitAuthentication"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));

                int availability = (int) initAuthentication.invokeExact(networkingSockets);
                String availabilityName = availabilityName(availability);

                System.out.printf("InitAuthentication   : %d (%s)%n", availability, availabilityName);

                if (!KNOWN_AVAILABILITY_RESULTS.contains(availability)) {
                    throw new IllegalStateException("Flat API call returned an unknown ESteamNetworkingAvailability value: " + availability);
                }
            } finally {
                steamShutdown.invokeExact();
                System.out.println("SteamAPI_Shutdown    : complete");
            }
        }

        System.out.println("P0-T09 passed: Java FFM loaded the official Steam redistributable and called ISteamNetworkingSockets without authored C/C++ glue.");

    }

    private static Path resolveRedistributable(Path workingDirectory) throws IOException {

        String explicitPath = System.getProperty("spike.steamApi64Path");
        if (explicitPath != null && !explicitPath.isBlank()) {
            Path path = Path.of(explicitPath).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("spike.steamApi64Path does not point to a file: " + path);
            }
            return path;
        }

        Path extracted = workingDirectory.resolve("steam_api64.dll");
        try (InputStream input = SteamFlatApiFfmSpike.class.getResourceAsStream(REDISTRIBUTABLE_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Official steam_api64.dll resource was not found on the runtime classpath. "
                    + "Use -Dspike.steamApi64Path=<path> to point at the Steamworks SDK redistributable.");
            }
            Files.copy(input, extracted, StandardCopyOption.REPLACE_EXISTING);
        }

        return extracted.toAbsolutePath().normalize();

    }

    private static Accessor findNetworkingSocketsAccessor(SymbolLookup lookup) {

        for (int version = 13; version >= 9; version--) {
            String suffix = String.format(Locale.ROOT, "v%03d", version);
            String[] candidates = {"SteamAPI_SteamNetworkingSockets_" + suffix, "SteamAPI_SteamNetworkingSockets_SteamAPI_" + suffix};

            for (String candidate : candidates) {
                var symbol = lookup.find(candidate);
                if (symbol.isPresent()) {
                    return new Accessor(candidate, symbol.get());
                }
            }
        }

        throw new IllegalStateException("No supported SteamNetworkingSockets flat-API accessor symbol was found in steam_api64.dll");

    }

    private static String initResultName(int value) {

        return switch (value) {
            case 0 -> "OK";
            case 1 -> "FailedGeneric";
            case 2 -> "NoSteamClient";
            case 3 -> "VersionMismatch";
            default -> "Unrecognized";
        };

    }

    private static String availabilityName(int value) {

        return switch (value) {
            case -102 -> "CannotTry";
            case -101 -> "Failed";
            case -100 -> "Previously";
            case -10 -> "Retrying";
            case 0 -> "Unknown";
            case 1 -> "NeverTried";
            case 2 -> "Waiting";
            case 3 -> "Attempting";
            case 100 -> "Current";
            default -> "Unrecognized";
        };

    }

    private static void requireWindowsX64() {

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        if (!osName.contains("windows")) {
            throw new IllegalStateException("P0-T09 currently targets the engine's Windows x64 baseline only");
        }
        if (!(architecture.contains("amd64") || architecture.contains("x86_64"))) {
            throw new IllegalStateException("P0-T09 requires a 64-bit x86 JVM; detected os.arch=" + architecture);
        }

    }

    private record Accessor(String name, MemorySegment symbol) {
    }
}
