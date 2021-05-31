package org.wastastic.compiler;

enum ReferenceType {
    FUNCREF(ValueType.FUNCREF),
    EXTERNREF(ValueType.EXTERNREF);

    private final ValueType valueType;

    ReferenceType(ValueType valueType) {
        this.valueType = valueType;
    }

    public ValueType toValueType() {
        return valueType;
    }
}
