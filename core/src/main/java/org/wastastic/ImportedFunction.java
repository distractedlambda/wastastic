package org.wastastic;

import org.jetbrains.annotations.NotNull;

final class ImportedFunction extends Import {
    private final @NotNull FunctionType type;

    ImportedFunction(@NotNull QualifiedName qualifiedName, @NotNull FunctionType type) {
        super(qualifiedName);
        this.type = type;
    }

    @NotNull FunctionType type() {
        return type;
    }
}
