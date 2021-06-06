package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import java.util.List;

record LabelScope(
    @NotNull Label targetLabel,
    @NotNull List<ValueType> parameterTypes,
    int operandStackSize,
    @Nullable Label endLabel,
    @Nullable Label elseLabel,
    @Nullable List<ValueType> elseParameterTypes
) {}
