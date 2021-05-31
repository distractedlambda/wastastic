package org.wastastic.compiler;

import org.objectweb.asm.Label;

record LabelScope(
    Label targetLabel,
    Object parameterTypes,
    int operandStackSize,
    Label endLabel,
    Label elseLabel,
    Object elseParameterTypes
) {}
