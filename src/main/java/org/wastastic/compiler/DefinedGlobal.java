package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

final class DefinedGlobal implements Global {
    private final GlobalType type;

    DefinedGlobal(GlobalType type) {
        this.type = requireNonNull(type);
    }

    @Override
    public GlobalType getType() {
        return type;
    }
}
