package org.wastastic;

import static java.util.Objects.requireNonNull;

public record TableType(ReferenceType elementType, Limits limits) implements ImportType {
    public TableType {
        requireNonNull(elementType);
        requireNonNull(limits);
    }
}
