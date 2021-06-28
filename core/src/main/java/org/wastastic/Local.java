package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record Local(@NotNull ValueType type, int index) {
    Local {
        requireNonNull(type);
    }
}
