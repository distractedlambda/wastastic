package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

enum ValueType {
    I32("I"),
    I64("J"),
    F32("F"),
    F64("D"),
    FUNCREF("TODO"),
    EXTERNREF("TODO");

    private final String descriptor;

    ValueType(String descriptor) {
        this.descriptor = requireNonNull(descriptor);
    }

    public String getDescriptor() {
        return descriptor;
    }
}
