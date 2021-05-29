package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

final class ImportedFunction extends Import implements Function {
    private final FunctionType type;

    ImportedFunction(String moduleName, String name, FunctionType type) {
        super(moduleName, name);
        this.type = requireNonNull(type);
    }

    @Override
    public FunctionType getType() {
        return type;
    }
}
