package org.wastastic;

import org.jetbrains.annotations.NotNull;

final class ImportedMemory extends Import {
    private final @NotNull MemoryType type;

    ImportedMemory(@NotNull QualifiedName qualifiedName, @NotNull MemoryType type) {
        super(qualifiedName);
        this.type = type;
    }

    @NotNull MemoryType getType() {
        return type;
    }
}
