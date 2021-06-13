package org.wastastic;

import org.jetbrains.annotations.NotNull;

record ElementSegment(Constant @NotNull[] values, @NotNull Mode mode, int tableIndex, int tableOffset) {
    enum Mode {
        PASSIVE,
        ACTIVE,
        DECLARATIVE,
    }

    static final Constant[] EMPTY_VALUES = new Constant[0];
}
