package org.wastastic.translator;

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
