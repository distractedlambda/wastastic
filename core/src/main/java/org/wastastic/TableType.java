package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record TableType(@NotNull ValueType elementType, @NotNull Limits limits) {
    TableType {
        requireNonNull(elementType);
        requireNonNull(limits);
    }
}
