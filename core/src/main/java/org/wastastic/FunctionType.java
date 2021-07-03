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
import static org.wastastic.ValueType.EXTERNREF;
import static org.wastastic.ValueType.F32;
import static org.wastastic.ValueType.F64;
import static org.wastastic.ValueType.FUNCREF;
import static org.wastastic.ValueType.I32;
import static org.wastastic.ValueType.I64;

final class FunctionType {
    private final @NotNull @Unmodifiable List<ValueType> parameterTypes;
    private final @NotNull @Unmodifiable List<ValueType> returnTypes;
    private @Nullable String descriptor;
    private @Nullable String indirectDescriptor;
    private @Nullable MethodType methodType;

    FunctionType(@NotNull List<ValueType> parameterTypes, @NotNull List<ValueType> returnTypes) {
        this.parameterTypes = List.copyOf(parameterTypes);
        this.returnTypes = List.copyOf(returnTypes);
    }

    static final @NotNull FunctionType RET_NONE = new FunctionType(List.of(), List.of()).initAll();
    static final @NotNull FunctionType RET_I32 = new FunctionType(List.of(), List.of(I32)).initAll();
    static final @NotNull FunctionType RET_I64 = new FunctionType(List.of(), List.of(I64)).initAll();
    static final @NotNull FunctionType RET_F32 = new FunctionType(List.of(), List.of(F32)).initAll();
    static final @NotNull FunctionType RET_F64 = new FunctionType(List.of(), List.of(F64)).initAll();
    static final @NotNull FunctionType RET_FUNCREF = new FunctionType(List.of(), List.of(FUNCREF)).initAll();
    static final @NotNull FunctionType RET_EXTERNREF = new FunctionType(List.of(), List.of(EXTERNREF)).initAll();

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
        var descriptor = this.indirectDescriptor;

        if (descriptor == null) {
            this.indirectDescriptor = descriptor = computeIndirectDescriptor();
        }

        return descriptor;
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

        builder.append(MODULE_INSTANCE_DESCRIPTOR).append(')');

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

    private @NotNull String computeIndirectDescriptor() {
        var builder = new StringBuilder("(");

        for (var parameterType : parameterTypes) {
            builder.append(parameterType.descriptor());
        }

        builder.append('I').append(MODULE_INSTANCE_DESCRIPTOR).append(')');

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

    private @NotNull FunctionType initAll() {
        descriptor();
        indirectDescriptor();
        methodType();
        return this;
    }
}
