package org.wastastic;

import org.jetbrains.annotations.NotNull;

abstract class Import {
    private final @NotNull QualifiedName name;

    Import(@NotNull QualifiedName name) {
        this.name = name;
    }

    final @NotNull QualifiedName qualifiedName() {
        return name;
    }
}
