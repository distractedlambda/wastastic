package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;

final class ImportedGlobal extends Import {
    private final @NotNull GlobalType type;

    ImportedGlobal(@NotNull String moduleName, @NotNull String name, @NotNull GlobalType type) {
        super(moduleName, name);
        this.type = type;
    }

    @NotNull GlobalType getType() {
        return type;
    }
}
