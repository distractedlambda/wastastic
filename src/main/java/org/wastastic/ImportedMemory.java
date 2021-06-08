package org.wastastic;

import org.jetbrains.annotations.NotNull;

final class ImportedMemory extends Import {
    private final @NotNull MemoryType type;

    ImportedMemory(@NotNull String moduleName, @NotNull String name, @NotNull MemoryType type) {
        super(moduleName, name);
        this.type = type;
    }

    @NotNull MemoryType getType() {
        return type;
    }
}
