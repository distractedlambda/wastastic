package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

record GlobalType(ValueType valueType, Mutability mutability) {
    GlobalType {
        requireNonNull(valueType);
        requireNonNull(mutability);
    }
}
