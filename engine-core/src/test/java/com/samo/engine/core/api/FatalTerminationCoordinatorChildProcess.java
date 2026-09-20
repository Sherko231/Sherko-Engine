package com.samo.engine.core.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** Child-JVM harness for the real {@link System#exit(int)} fatal path. */
public final class FatalTerminationCoordinatorChildProcess {
    private FatalTerminationCoordinatorChildProcess() {

    }

    public static void main(String[] args) {

        Path marker = Path.of(args[0]);
        NativeResourceRegistry registry = new NativeResourceRegistry();
        MarkerSubsystem subsystem = new MarkerSubsystem(marker, registry);
        subsystem.initialize();
        subsystem.start();

        EngineLogger logger = new EngineLogger(new EngineLogger.Sink() {
            @Override
            public void write(EngineLogger.Event event) {

                if (event.level() == EngineLogger.Level.FATAL) {
                    append(marker, "fatal");
                }

            }

            @Override
            public void flush() {

                append(marker, "flush");

            }
        });

        new FatalTerminationCoordinator(logger).terminate("child fatal", EngineLogger.Context.empty(), List.of(subsystem), registry);

    }

    private static void append(Path marker, String value) {

        try {
            Files.writeString(marker, value + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to write fatal child marker", failure);
        }

    }

    private static final class MarkerSubsystem extends EngineSubsystem {
        private final Path marker;
        private final NativeResourceRegistry registry;
        private NativeResourceRegistry.Registration resource;

        MarkerSubsystem(Path marker, NativeResourceRegistry registry) {

            this.marker = marker;
            this.registry = registry;

        }

        @Override
        protected void onInitialize() {

            resource = registry.register("child resource", 101L, () -> append(marker, "resource-close"));

        }

        @Override
        protected void onStart() {

        }

        @Override
        protected void onStop() {

            append(marker, "stop");

        }

        @Override
        protected void onClose() {

            append(marker, "close");
            resource.close();

        }
    }
}
