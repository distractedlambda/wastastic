package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.objectweb.asm.Opcodes.RETURN;

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
        // TODO
        throw new UnsupportedOperationException("TODO");
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
