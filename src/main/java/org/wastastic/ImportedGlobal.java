package org.wastastic;

import org.jetbrains.annotations.NotNull;

final class ImportedGlobal extends Import {
    private final @NotNull GlobalType type;

    ImportedGlobal(@NotNull QualifiedName qualifiedName, @NotNull GlobalType type) {
        super(qualifiedName);
        this.type = type;
    }

    @NotNull GlobalType getType() {
        return type;
    }
}
