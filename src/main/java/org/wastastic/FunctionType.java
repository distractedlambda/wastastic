package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Type.getMethodType;

final class FunctionType {
    private final @NotNull List<ValueType> parameterTypes;
    private final @NotNull List<ValueType> returnTypes;

    FunctionType(@NotNull List<ValueType> parameterTypes, @NotNull List<ValueType> returnTypes) throws TranslationException {
        if (returnTypes.size() > 1) {
            throw new TranslationException("TODO implement multiple return values");
        }

        this.parameterTypes = parameterTypes;
        this.returnTypes = returnTypes;
    }

    @NotNull List<ValueType> parameterTypes() {
        return parameterTypes;
    }

    @NotNull List<ValueType> returnTypes() {
        return returnTypes;
    }

    @NotNull String descriptor() {
        var builder = new StringBuilder("(");

        for (var parameterType : parameterTypes) {
            builder.append(parameterType.descriptor());
        }

        builder.append(ModuleTranslator.GENERATED_DESCRIPTOR);
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
}
