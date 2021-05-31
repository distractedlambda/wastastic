package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

record TableType(ReferenceType elementType, Limits limits) {
    TableType {
        requireNonNull(elementType);
        requireNonNull(limits);
    }
}
