package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record ElementSegment(Constant @NotNull[] values, @NotNull Mode mode, int tableIndex, int tableOffset) {
    ElementSegment {
        requireNonNull(values);
        requireNonNull(mode);
    }

    enum Mode {
        PASSIVE,
        ACTIVE,
        DECLARATIVE,
    }

    static final Constant[] EMPTY_VALUES = new Constant[0];
}
