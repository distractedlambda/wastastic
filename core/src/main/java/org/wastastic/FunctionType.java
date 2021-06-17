package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodType;
import java.util.List;

import static java.lang.invoke.MethodType.methodType;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Type.getMethodType;
import static org.wastastic.Names.MODULE_INSTANCE_DESCRIPTOR;

record FunctionType(@NotNull List<ValueType> parameterTypes, @NotNull List<ValueType> returnTypes) {
    FunctionType {
        if (returnTypes.size() > 1) {
            throw new IllegalArgumentException();
        }
    }

    @NotNull String descriptor() {
        var builder = new StringBuilder("(");

        for (var parameterType : parameterTypes) {
            builder.append(parameterType.descriptor());
        }

        builder.append(MODULE_INSTANCE_DESCRIPTOR);
        builder.append(')');

        if (returnTypes.isEmpty()) {
            builder.append('V');
        }
        else if (returnTypes.size() == 1) {
            builder.append(returnTypes.get(0).descriptor());
        }
        else {
            throw new AssertionError();
        }

        return builder.toString();
    }

    @NotNull Type asmType() {
        return getMethodType(descriptor());
    }

    int returnOpcode() {
        if (returnTypes.isEmpty()) {
            return RETURN;
        }
        else if (returnTypes.size() == 1) {
            return returnTypes.get(0).returnOpcode();
        }
        else {
            throw new AssertionError();
        }
    }

    @NotNull MethodType jvmType() {
        var jvmParameterTypes = new Class<?>[parameterTypes().size() + 1];

        for (var i = 0; i < parameterTypes.size(); i++) {
            jvmParameterTypes[i] = parameterTypes.get(i).jvmType();
        }

        jvmParameterTypes[parameterTypes.size()] = ModuleInstance.class;

        return methodType(!returnTypes.isEmpty() ? returnTypes.get(0).jvmType() : void.class, jvmParameterTypes);
    }
}
