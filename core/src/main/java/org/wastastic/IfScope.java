package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

import java.util.List;

import static java.util.Objects.requireNonNull;

final class IfScope extends ControlScope {
    private final @NotNull Label elseLabel;
    private final @NotNull Label endLabel;

    IfScope(@NotNull Label elseLabel, @NotNull Label endLabel, @NotNull FunctionType type, int operandStackSize) {
        super(type, operandStackSize);
        this.elseLabel = requireNonNull(elseLabel);
        this.endLabel = requireNonNull(endLabel);
    }

    @NotNull Label elseLabel() {
        return elseLabel;
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
