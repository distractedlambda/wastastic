package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record ExportedFunction(@NotNull String name, @NotNull FunctionType type) {
    ExportedFunction {
        requireNonNull(name);
        requireNonNull(type);
    }
}
