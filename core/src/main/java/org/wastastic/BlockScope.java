package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

import java.util.List;

import static java.util.Objects.requireNonNull;

final class BlockScope extends ControlScope {
    private final @NotNull Label endLabel;

    BlockScope(@NotNull Label endLabel, @NotNull FunctionType type, int operandStackSize) {
        super(type, operandStackSize);
        this.endLabel = requireNonNull(endLabel);
    }

    @NotNull Label endLabel() {
        return endLabel;
    }

    @Override @NotNull Label branchTargetLabel() {
        return endLabel;
    }

    @Override @NotNull List<ValueType> branchTargetParameterTypes() {
        return type().returnTypes();
    }
}
