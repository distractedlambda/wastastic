package org.wastastic.compiler;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class ImportedMemory extends Import implements Memory {
    private final MemoryType type;

    ImportedMemory(String moduleName, String name, MemoryType type) {
        super(moduleName, name);
        this.type = requireNonNull(type);
    }

    @Override
    public MemoryType getType() {
        return type;
    }
}
