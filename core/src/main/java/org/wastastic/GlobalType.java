package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record GlobalType(@NotNull ValueType valueType, @NotNull Mutability mutability) {
    GlobalType {
        requireNonNull(valueType);
        requireNonNull(mutability);
    }
}
