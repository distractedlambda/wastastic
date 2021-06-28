package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record ImportedGlobal(@NotNull QualifiedName name, @NotNull GlobalType type) {
    ImportedGlobal {
        requireNonNull(name);
        requireNonNull(type);
    }
}
