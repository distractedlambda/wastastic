package org.wastastic;

import static java.util.Objects.requireNonNull;

public record GlobalType(ValueType valueType, Mutability mutability) implements ImportType {
    public GlobalType {
        requireNonNull(valueType);
        requireNonNull(mutability);
    }
}
