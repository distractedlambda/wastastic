package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

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
}
