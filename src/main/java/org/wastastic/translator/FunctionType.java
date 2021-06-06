package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;

final class FunctionType {
    private final @NotNull List<ValueType> parameterTypes;
    private final @NotNull List<ValueType> returnTypes;
    private final @NotNull String descriptor;

    private final @Nullable Class<?> returnTupleClass;

    FunctionType(@NotNull List<ValueType> parameterTypes, @NotNull List<ValueType> returnTypes) {
        this.parameterTypes = parameterTypes;
        this.returnTypes = returnTypes;

        var descriptorBuilder = new StringBuilder("(");

        for (var type : parameterTypes) {
            descriptorBuilder.append(type.getDescriptor());
        }

        descriptorBuilder.append("Lorg/wastastic/Module;");

        if (returnTypes.size() < 2) {
            returnTupleClass = null;
            if (returnTypes.isEmpty()) {
                descriptorBuilder.append('V');
            }
            else {
                descriptorBuilder.append(returnTypes.get(0).getDescriptor());
            }
        }
        else {
            returnTupleClass = Tuples.getTupleClass(returnTypes);
            descriptorBuilder.append(Type.getDescriptor(returnTupleClass));
        }

        descriptor = descriptorBuilder.toString();
    }

    @NotNull List<ValueType> getParameterTypes() {
        return parameterTypes;
    }

    @NotNull List<ValueType> getReturnTypes() {
        return returnTypes;
    }

    @Nullable Class<?> getReturnTupleClass() {
        return returnTupleClass;
    }

    @NotNull String getDescriptor() {
        return descriptor;
    }
}
