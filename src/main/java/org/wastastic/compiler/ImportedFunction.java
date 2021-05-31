package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

final class ImportedFunction extends Import {
    private final FunctionType type;

    ImportedFunction(String moduleName, String name, FunctionType type) {
        super(moduleName, name);
        this.type = requireNonNull(type);
    }

    FunctionType getType() {
        return type;
    }
}
