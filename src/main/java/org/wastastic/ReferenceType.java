package org.wastastic;

enum ReferenceType {
    FUNCREF(ValueType.FUNCREF),
    EXTERNREF(ValueType.EXTERNREF);

    private final ValueType valueType;

    ReferenceType(ValueType valueType) {
        this.valueType = valueType;
    }

    ValueType toValueType() {
        return valueType;
    }
}
