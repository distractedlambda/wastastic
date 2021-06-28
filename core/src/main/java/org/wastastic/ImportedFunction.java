package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record ImportedFunction(@NotNull QualifiedName qualifiedName, @NotNull FunctionType type) {
    ImportedFunction {
        requireNonNull(qualifiedName);
        requireNonNull(type);
    }
}
