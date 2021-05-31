package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

enum ValueType {
    I32("I", false),
    I64("J", true),
    F32("F", false),
    F64("D", true),
    FUNCREF("java/lang/invoke/MethodHandle", false),
    EXTERNREF("java/lang/Object", false);

    private final String descriptor;
    private final boolean doubleWidth;

    ValueType(String descriptor, boolean doubleWidth) {
        this.descriptor = requireNonNull(descriptor);
        this.doubleWidth = doubleWidth;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public boolean isDoubleWidth() {
        return doubleWidth;
    }
}
