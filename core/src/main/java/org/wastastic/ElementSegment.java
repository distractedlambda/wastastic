package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

import static java.util.Objects.requireNonNull;

record ElementSegment(
    @NotNull @Unmodifiable List<Constant> values,
    @NotNull Mode mode,
    int tableIndex,
    int tableOffset
) {
    ElementSegment {
        requireNonNull(values);
        requireNonNull(mode);
    }

    enum Mode {
        PASSIVE,
        ACTIVE,
        DECLARATIVE,
    }
}
