package org.wastastic;

import org.jetbrains.annotations.NotNull;

final class ImportedTable extends Import {
    private final @NotNull TableType type;

    ImportedTable(@NotNull QualifiedName qualifiedName, @NotNull TableType type) {
        super(qualifiedName);
        this.type = type;
    }

    @NotNull TableType getType() {
        return type;
    }
}
