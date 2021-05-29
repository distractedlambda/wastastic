package org.wastastic;

import static java.util.Objects.requireNonNull;

public enum ValueType {
    I32("i32"),
    I64("i64"),
    F32("f32"),
    F64("f64"),
    FUNCREF("funcref"),
    EXTERNREF("externref");

    private final String name;

    ValueType(String name) {
        this.name = requireNonNull(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
