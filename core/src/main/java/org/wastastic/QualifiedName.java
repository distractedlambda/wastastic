package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public record QualifiedName(@NotNull String moduleName, @NotNull String name) {
    public QualifiedName {
        requireNonNull(moduleName);
        requireNonNull(name);
    }
}
