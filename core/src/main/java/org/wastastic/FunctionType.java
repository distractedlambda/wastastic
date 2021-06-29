package org.wastastic;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.invoke.MethodType;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.wastastic.Names.MODULE_INSTANCE_DESCRIPTOR;
import static org.wastastic.Names.OBJECT_ARRAY_DESCRIPTOR;

final class FunctionType {
    private final @NotNull List<ValueType> parameterTypes;
    private final @NotNull List<ValueType> returnTypes;
    private @Nullable String descriptor;

    FunctionType(@NotNull List<ValueType> parameterTypes, @NotNull List<ValueType> returnTypes) {
        this.parameterTypes = requireNonNull(parameterTypes);
        this.returnTypes = requireNonNull(returnTypes);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<ValueType> parameterTypes() {
        return unmodifiableList(parameterTypes);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<ValueType> returnTypes() {
        return unmodifiableList(returnTypes);
    }

    int returnOpcode() {
        if (returnTypes.isEmpty()) {
            return RETURN;
        }
        else if (returnTypes.size() == 1) {
            return returnTypes.get(0).returnOpcode();
        }
        else {
            return ARETURN;
        }
    }

    @NotNull String descriptor() {
        var descriptor = this.descriptor;

        if (descriptor == null) {
            this.descriptor = descriptor = computeDescriptor();
        }

        return descriptor;
    }

    private @NotNull String computeDescriptor() {
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
            builder.append(OBJECT_ARRAY_DESCRIPTOR);
        }

        return builder.toString();
    }

    @NotNull MethodType methodType() {
        var argumentTypes = new Class<?>[parameterTypes.size() + 1];

        for (var i = 0; i < parameterTypes.size(); i++) {
            argumentTypes[i] = parameterTypes.get(i).jvmType();
        }

        argumentTypes[parameterTypes.size()] = ModuleInstance.class;

        Class<?> returnType;
        if (returnTypes.isEmpty()) {
            returnType = void.class;
        }
        else if (returnTypes.size() == 1) {
            returnType = returnTypes.get(0).jvmType();
        }
        else {
            returnType = Object[].class;
        }

        return MethodType.methodType(returnType, argumentTypes);
    }
}
