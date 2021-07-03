package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.invoke.MethodType;
import java.util.List;

import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.wastastic.Names.MODULE_INSTANCE_DESCRIPTOR;
import static org.wastastic.Names.OBJECT_ARRAY_DESCRIPTOR;

final class FunctionType {
    private final @NotNull @Unmodifiable List<ValueType> parameterTypes;
    private final @NotNull @Unmodifiable List<ValueType> returnTypes;
    private @Nullable String descriptor;
    private @Nullable MethodType methodType;

    FunctionType(@NotNull List<ValueType> parameterTypes, @NotNull List<ValueType> returnTypes) {
        this.parameterTypes = List.copyOf(parameterTypes);
        this.returnTypes = List.copyOf(returnTypes);
    }

    @NotNull @Unmodifiable List<ValueType> parameterTypes() {
        return parameterTypes;
    }

    @NotNull @Unmodifiable List<ValueType> returnTypes() {
        return returnTypes;
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

    @NotNull String indirectDescriptor() {
        var builder = new StringBuilder("(");

        for (var parameterType : parameterTypes) {
            builder.append(parameterType.descriptor());
        }

        builder.append("I").append(MODULE_INSTANCE_DESCRIPTOR).append(")");

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
        var type = this.methodType;

        if (type == null) {
            this.methodType = type = computeMethodType();
        }

        return type;
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

    private @NotNull MethodType computeMethodType() {
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

    private static final FunctionType EMPTY = new FunctionType(List.of(), List.of());
    private static final FunctionType RETURNING_I32 = new FunctionType(List.of(), List.of(ValueType.I32));
    private static final FunctionType RETURNING_I64 = new FunctionType(List.of(), List.of(ValueType.I64));
    private static final FunctionType RETURNING_F32 = new FunctionType(List.of(), List.of(ValueType.F32));
    private static final FunctionType RETURNING_F64 = new FunctionType(List.of(), List.of(ValueType.F64));
    private static final FunctionType RETURNING_FUNCREF = new FunctionType(List.of(), List.of(ValueType.FUNCREF));
    private static final FunctionType RETURNING_EXTERNREF = new FunctionType(List.of(), List.of(ValueType.FUNCREF));
}
