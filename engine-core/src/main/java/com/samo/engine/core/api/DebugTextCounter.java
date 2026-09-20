package com.samo.engine.core.api;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable bounded diagnostic label/value pair; not general runtime text. */
public record DebugTextCounter(String label, long value) {
    public static final int MAX_LABEL_LENGTH = 32;
    private static final Pattern LABEL_PATTERN = Pattern.compile("[A-Za-z0-9_\\-./:]+");

    public DebugTextCounter {

        Objects.requireNonNull(label, "label");
        if (label.isEmpty() || label.length() > MAX_LABEL_LENGTH || !LABEL_PATTERN.matcher(label).matches()) {
            throw new IllegalArgumentException("label must contain 1-" + MAX_LABEL_LENGTH + " allowed ASCII diagnostic characters");
        }

    }
}
