package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record MemoryType(@NotNull Limits limits) {
    MemoryType {
        requireNonNull(limits);
    }
}
