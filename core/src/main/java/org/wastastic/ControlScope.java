package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

sealed abstract class ControlScope permits BlockScope, IfScope, LoopScope {
    private final @NotNull FunctionType type;
    private final int baseOperandStackSize;
    private boolean unreachable = false;

    ControlScope(@NotNull FunctionType type, int operandStackSize) {
        this.type = requireNonNull(type);
        this.baseOperandStackSize = operandStackSize - type.parameterTypes().size();
    }

    final @NotNull FunctionType type() {
        return type;
    }

    final int baseOperandStackSize() {
        return baseOperandStackSize;
    }

    final boolean restUnreachable() {
        return unreachable;
    }

    final void markRestUnreachable() {
        unreachable = true;
    }
}
