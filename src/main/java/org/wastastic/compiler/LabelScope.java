package org.wastastic.compiler;

import org.objectweb.asm.Label;

record LabelScope(
    Label label,
    Object parameterTypes,
    int operandStackSize,
    Label elseLabel,
    Object elseParameterTypes
) {
    LabelScope(Label label, Object parameterTypes, int operandStackSize) {
        this(label, parameterTypes, operandStackSize, null, null);
    }
}
