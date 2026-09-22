package com.samo.engine.assets.api;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stable 128-bit asset identity independent of source-file location.
 *
 * <p>The two record components are the complete identity value. Source paths, metadata paths, and
 * runtime cache locations are deliberately not part of this type.
 */
public record AssetId(long highBits, long lowBits) {
    private static final Pattern CANONICAL_TEXT = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    /**
     * Generates a new opaque asset identity suitable for newly-authored asset metadata.
     *
     * @return a newly generated 128-bit asset identity
     */
    public static AssetId generate() {

        UUID uuid = UUID.randomUUID();
        return new AssetId(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());

    }

    /**
     * Parses the canonical lowercase UUID-style textual representation of an asset identity.
     *
     * @param text canonical 36-character identity text
     * @return the parsed asset identity
     * @throws NullPointerException if {@code text} is null
     * @throws IllegalArgumentException if {@code text} is not canonical identity text
     */
    public static AssetId parse(String text) {

        Objects.requireNonNull(text, "text");
        if (!CANONICAL_TEXT.matcher(text).matches()) {
            throw new IllegalArgumentException("AssetId text must use canonical lowercase 8-4-4-4-12 hexadecimal form");
        }

        UUID uuid = UUID.fromString(text);
        return new AssetId(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());

    }

    /**
     * Returns the canonical lowercase UUID-style textual representation of this identity.
     *
     * @return canonical 36-character identity text
     */
    @Override
    public String toString() {

        return new UUID(highBits, lowBits).toString();

    }
}
