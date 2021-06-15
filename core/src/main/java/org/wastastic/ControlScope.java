package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import java.util.List;

final class ControlScope {
    private final @NotNull Label targetLabel;
    private final @NotNull List<ValueType> parameterTypes;
    private final int operandStackSize;
    private @Nullable Label endLabel;
    private @Nullable Label elseLabel;
    private @Nullable List<ValueType> elseParameterTypes;
    private boolean terminated;

    ControlScope(
        @NotNull Label targetLabel,
        @NotNull List<ValueType> parameterTypes,
        int operandStackSize,
        @Nullable Label endLabel,
        @Nullable Label elseLabel,
        @Nullable List<ValueType> elseParameterTypes
    ) {
        this.targetLabel = targetLabel;
        this.parameterTypes = parameterTypes;
        this.operandStackSize = operandStackSize;
        this.endLabel = endLabel;
        this.elseLabel = elseLabel;
        this.elseParameterTypes = elseParameterTypes;
    }

    @NotNull Label targetLabel() {
        return targetLabel;
    }

    @NotNull List<ValueType> parameterTypes() {
        return parameterTypes;
    }

    int operandStackSize() {
        return operandStackSize;
    }

    @Nullable Label endLabel() {
        return endLabel;
    }

    @Nullable Label elseLabel() {
        return elseLabel;
    }

    @Nullable List<ValueType> elseParameterTypes() {
        return elseParameterTypes;
    }

    boolean isTerminated() {
        return terminated;
    }

    void markTerminated() {
        terminated = true;
    }
}
