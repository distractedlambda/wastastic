package org.wastastic.compiler;

import org.objectweb.asm.Label;

import java.util.List;

record BranchTarget(Label label, Object parameterTypes, int operandStackDepth) {
    int getParameterCount() {
        return ResultTypes.length(parameterTypes);
    }

    List<ValueType> getParameterTypeList() {
        return ResultTypes.asList(parameterTypes);
    }
}
