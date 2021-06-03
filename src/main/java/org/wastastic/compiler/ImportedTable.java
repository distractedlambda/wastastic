package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;

final class ImportedTable extends Import {
    private final @NotNull TableType type;

    ImportedTable(@NotNull String moduleName, @NotNull String name, @NotNull TableType type) {
        super(moduleName, name);
        this.type = type;
    }

    @NotNull TableType getType() {
        return type;
    }
}
