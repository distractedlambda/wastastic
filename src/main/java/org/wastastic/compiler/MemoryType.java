package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

record MemoryType(Limits limits) implements Memory {
    MemoryType {
        requireNonNull(limits);
    }

    @Override
    public MemoryType getType() {
        return this;
    }
}
