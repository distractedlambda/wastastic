package org.wastastic;

import org.jetbrains.annotations.NotNull;

enum ReferenceType {
    FUNCREF(ValueType.FUNCREF),
    EXTERNREF(ValueType.EXTERNREF);

    private final @NotNull ValueType valueType;

    ReferenceType(@NotNull ValueType valueType) {
        this.valueType = valueType;
    }

    @NotNull ValueType toValueType() {
        return valueType;
    }
}
