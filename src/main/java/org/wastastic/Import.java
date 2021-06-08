package org.wastastic;

import org.jetbrains.annotations.NotNull;

abstract class Import {
    private final @NotNull String moduleName;
    private final @NotNull String name;

    Import(@NotNull String moduleName, @NotNull String name) {
        this.moduleName = moduleName;
        this.name = name;
    }

    final @NotNull String getModuleName() {
        return moduleName;
    }

    final @NotNull String getName() {
        return name;
    }
}
