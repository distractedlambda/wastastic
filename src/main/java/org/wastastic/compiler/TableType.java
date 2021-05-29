package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

record TableType(ReferenceType elementType, Limits limits) implements Table {
    TableType {
        requireNonNull(elementType);
        requireNonNull(limits);
    }

    @Override
    public TableType getType() {
        return this;
    }
}
