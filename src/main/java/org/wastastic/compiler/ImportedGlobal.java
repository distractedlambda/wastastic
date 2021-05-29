package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

final class ImportedGlobal extends Import implements Global {
    private final GlobalType type;

    ImportedGlobal(String moduleName, String name, GlobalType type) {
        super(moduleName, name);
        this.type = requireNonNull(type);
    }

    @Override
    public GlobalType getType() {
        return type;
    }
}
