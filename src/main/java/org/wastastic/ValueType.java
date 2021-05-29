package org.wastastic;

import static java.util.Objects.requireNonNull;

public enum ValueType {
    I32("i32", "I"),
    I64("i64", "J"),
    F32("f32", "F"),
    F64("f64", "D"),
    FUNCREF("funcref", "TODO"),
    EXTERNREF("externref", "TODO");

    private final String name;
    private final String descriptor;

    ValueType(String name, String descriptor) {
        this.name = requireNonNull(name);
        this.descriptor = requireNonNull(descriptor);
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return name;
    }
}
