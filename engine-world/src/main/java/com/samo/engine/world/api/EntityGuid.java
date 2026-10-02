package com.samo.engine.world.api;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Stable 128-bit authoring identity for one logical world entity.
 *
 * <p>
 * An {@link EntityGuid} survives save/reload boundaries and is deliberately independent of the
 * transient runtime {@link EntityId} allocated for any particular world instance.
 */
public record EntityGuid(long highBits, long lowBits) {
    private static final Pattern CANONICAL_TEXT = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    /**
     * Generates a new opaque authoring identity suitable for a newly-authored entity.
     *
     * @return a newly generated 128-bit entity GUID
     */
    public static EntityGuid generate() {

        UUID uuid = UUID.randomUUID();
        return new EntityGuid(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());

    }

    /**
     * Parses the canonical lowercase UUID-style textual representation of an entity GUID.
     *
     * @param text
     *            canonical 36-character GUID text
     * @return the parsed entity GUID
     * @throws NullPointerException
     *             if {@code text} is null
     * @throws IllegalArgumentException
     *             if {@code text} is not canonical GUID text
     */
    public static EntityGuid parse(String text) {

        Objects.requireNonNull(text, "text");
        if (!CANONICAL_TEXT.matcher(text).matches()) {
            throw new IllegalArgumentException("EntityGuid text must use canonical lowercase 8-4-4-4-12 hexadecimal form");
        }

        UUID uuid = UUID.fromString(text);
        return new EntityGuid(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());

    }

    /**
     * Returns the canonical lowercase UUID-style textual representation of this identity.
     *
     * @return canonical 36-character GUID text
     */
    @Override
    public String toString() {

        return new UUID(highBits, lowBits).toString();

    }
}
