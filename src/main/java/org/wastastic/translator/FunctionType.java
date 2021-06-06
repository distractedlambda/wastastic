package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.RETURN;

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
            descriptorBuilder.append(type.descriptor());
        }

        descriptorBuilder.append(ModuleTranslator.DESCRIPTOR);

        if (returnTypes.size() < 2) {
            returnTupleClass = null;
            if (returnTypes.isEmpty()) {
                descriptorBuilder.append('V');
            }
            else {
                descriptorBuilder.append(returnTypes.get(0).descriptor());
            }
        }
        else {
            returnTupleClass = Tuples.getTupleClass(returnTypes);
            descriptorBuilder.append(Type.getDescriptor(returnTupleClass));
        }

        descriptor = descriptorBuilder.toString();
    }

    @NotNull List<ValueType> parameterTypes() {
        return parameterTypes;
    }

    @NotNull List<ValueType> returnTypes() {
        return returnTypes;
    }

    @Nullable Class<?> returnTupleClass() {
        return returnTupleClass;
    }

    @NotNull String descriptor() {
        return descriptor;
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
}
