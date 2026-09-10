package com.samo.game.server.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class VersionReport {
    private static final String RESOURCE = "META-INF/sherko-version.properties";

    private VersionReport() {
    }

    public static void print() {
        Properties properties = loadProperties();
        System.out.println("executable=game-server");
        System.out.println("engineCommit=" + required(properties, "engineCommit"));
        System.out.println("protocolVersion=" + required(properties, "protocolVersion"));
        System.out.println("assetVersion=" + required(properties, "assetVersion"));
        System.out.println("javaVersion=" + System.getProperty("java.version"));
        System.out.println("nativeLibraries=" + required(properties, "nativeLibraries"));
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = VersionReport.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing version metadata resource: " + RESOURCE);
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load version metadata resource: " + RESOURCE, exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required version metadata key: " + key);
        }
        return value;
    }
}
