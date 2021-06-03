package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;

final class FunctionType {
    private final @NotNull List<ValueType> parameterTypes;
    private final @NotNull List<ValueType> returnTypes;

    private @Nullable String signatureString;

    FunctionType(@NotNull List<ValueType> parameterTypes, @NotNull List<ValueType> returnTypes) {
        this.parameterTypes = parameterTypes;
        this.returnTypes = returnTypes;
    }

    @NotNull List<ValueType> getParameterTypes() {
        return parameterTypes;
    }

    @NotNull List<ValueType> getReturnTypes() {
        return returnTypes;
    }

    @NotNull String getSignatureString() {
        if (signatureString != null) {
            return signatureString;
        } else {
            return (signatureString = makeSignatureString());
        }
    }

    private @NotNull String makeSignatureString() {
        var builder = new StringBuilder("(");

        for (var type : parameterTypes) {
            builder.append(type.getDescriptor());
        }

        builder.append(')');

        if (returnTypes.size() == 0) {
            builder.append('V');
        }
        else if (returnTypes.size() == 1) {
            builder.append(returnTypes.get(0).getDescriptor());
        }
        else {
            var tupleSignatureChars = new char[returnTypes.size()];

            for (var i = 0; i < returnTypes.size(); i++) {
                 tupleSignatureChars[i] = returnTypes.get(i).getTupleSignatureCharacter();
            }

            var descriptor = Type.getDescriptor(Tuples.getTupleClass(new String(tupleSignatureChars)));
            builder.append(descriptor);
        }

        return builder.toString();
    }
}
