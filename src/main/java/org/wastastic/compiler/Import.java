package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

abstract class Import {
    private final String moduleName;
    private final String name;

    Import(String moduleName, String name) {
        this.moduleName = requireNonNull(moduleName);
        this.name = requireNonNull(name);
    }

    final String getModuleName() {
        return moduleName;
    }

    final String getName() {
        return name;
    }
}
