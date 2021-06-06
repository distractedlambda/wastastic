package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;

final class ImportedFunction extends Import {
    private final @NotNull FunctionType type;

    ImportedFunction(@NotNull String moduleName, @NotNull String name, @NotNull FunctionType type) {
        super(moduleName, name);
        this.type = type;
    }

    @NotNull FunctionType getType() {
        return type;
    }
}
