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
    private final @NotNull List<ValueType> baseOperandStack;
    private final @NotNull List<ValueType> returnTypes;
    private final boolean isLoop;

    private boolean isBranchTargetUsed = false;

    private @Nullable Label elseLabel;
    private @Nullable List<ValueType> elseOperandStack;

    ControlScope(
        @NotNull Label branchTargetLabel,
        @NotNull List<ValueType> branchTargetParameterTypes,
        @NotNull List<ValueType> baseOperandStack,
        @NotNull List<ValueType> returnTypes,
        boolean isLoop,
        @Nullable Label elseLabel,
        @Nullable List<ValueType> elseOperandStack
    ) {
        this.branchTargetLabel = branchTargetLabel;
        this.branchTargetParameterTypes = branchTargetParameterTypes;
        this.baseOperandStack = baseOperandStack;
        this.returnTypes = returnTypes;
        this.isLoop = isLoop;
        this.elseLabel = elseLabel;
        this.elseOperandStack = elseOperandStack;
    }

    @NotNull List<ValueType> returnTypes() {
        return returnTypes;
    }

    @NotNull Label branchTargetLabel() {
        return branchTargetLabel;
    }

    @NotNull List<ValueType> branchTargetParameterTypes() {
        return branchTargetParameterTypes;
    }

    @NotNull List<ValueType> baseOperandStack() {
        return baseOperandStack;
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
