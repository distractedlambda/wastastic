package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

import java.util.List;

import static java.util.Objects.requireNonNull;

final class LoopScope extends ControlScope {
    private final @NotNull Label startLabel;

    LoopScope(@NotNull Label startLabel, @NotNull FunctionType type, int operandStackSize) {
        super(type, operandStackSize);
        this.startLabel = requireNonNull(startLabel);
    }

    @NotNull Label startLabel() {
        return startLabel;
    }

    @Override @NotNull Label branchTargetLabel() {
        return startLabel;
    }

    @Override @NotNull List<ValueType> branchTargetParameterTypes() {
        return type().parameterTypes();
    }
}
