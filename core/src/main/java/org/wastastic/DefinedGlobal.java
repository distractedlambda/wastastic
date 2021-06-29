package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record DefinedGlobal(@NotNull GlobalType type, @NotNull Constant initialValue) {
    DefinedGlobal {
        requireNonNull(type);
        requireNonNull(initialValue);
    }
}
