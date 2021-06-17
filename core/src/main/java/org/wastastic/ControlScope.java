package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

import java.util.List;

import static org.objectweb.asm.Opcodes.RETURN;
import static org.wastastic.Lists.single;

final class ControlScope {
    private final @NotNull Label branchTargetLabel;
    private final @NotNull List<ValueType> branchTargetParameterTypes;
    private final int baseOperandStackSize;
    private final boolean isLoop;

    private boolean isBranchTargetUsed = false;

    private @Nullable Label elseLabel;
    private @Nullable List<ValueType> elseOperandStack;

    ControlScope(
        @NotNull Label branchTargetLabel,
        @NotNull List<ValueType> branchTargetParameterTypes,
        int baseOperandStackSize,
        boolean isLoop,
        @Nullable Label elseLabel,
        @Nullable List<ValueType> elseOperandStack
    ) {
        this.branchTargetLabel = branchTargetLabel;
        this.branchTargetParameterTypes = branchTargetParameterTypes;
        this.baseOperandStackSize = baseOperandStackSize;
        this.isLoop = isLoop;
        this.elseLabel = elseLabel;
        this.elseOperandStack = elseOperandStack;
    }

    @NotNull Label branchTargetLabel() {
        return branchTargetLabel;
    }

    @NotNull List<ValueType> branchTargetParameterTypes() {
        return branchTargetParameterTypes;
    }

    int baseOperandStackSize() {
        return baseOperandStackSize;
    }

    boolean isLoop() {
        return isLoop;
    }

    @Nullable Label elseLabel() {
        return elseLabel;
    }

    @Nullable List<ValueType> elseOperandStack() {
        return elseOperandStack;
    }

    boolean isBranchTargetUsed() {
        return isBranchTargetUsed;
    }

    void markBranchTargetUsed() {
        isBranchTargetUsed = true;
    }

    void dropElse() {
        elseLabel = null;
        elseOperandStack = null;
    }

    int returnOpcode() {
        if (branchTargetParameterTypes.size() != 0) {
            return single(branchTargetParameterTypes).returnOpcode();
        }
        else {
            return RETURN;
        }
    }
}
