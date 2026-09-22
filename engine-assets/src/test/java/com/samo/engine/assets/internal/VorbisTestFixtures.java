package com.samo.engine.assets.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Objects;

final class VorbisTestFixtures {
    private VorbisTestFixtures() {

    }

    static byte[] read(String resourceName) {

        try (InputStream input = Objects.requireNonNull(VorbisTestFixtures.class.getClassLoader().getResourceAsStream(resourceName), "Missing test resource " + resourceName)) {
            byte[] bytes = input.readAllBytes();
            return resourceName.endsWith(".b64") ? Base64.getMimeDecoder().decode(bytes) : bytes;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read test resource " + resourceName, exception);
        }

    }
}
