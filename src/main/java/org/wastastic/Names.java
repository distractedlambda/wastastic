package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Map;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

final class Names {
    private Names() {}

    static @NotNull String methodDescriptor(@NotNull Class<?> returnType, @NotNull Class<?> @NotNull... argumentTypes) {
        var builder = new StringBuilder("(");

        for (var argumentType : argumentTypes) {
            builder.append(getDescriptor(argumentType));
        }

        return builder.append(")").append(getDescriptor(returnType)).toString();
    }

    static @NotNull String tableFieldName(int index) {
        return "t" + index;
    }

    static @NotNull String functionMethodName(int index) {
        return "f" + index;
    }

    static @NotNull String functionMethodHandleFieldName(int index) {
        return "h" + index;
    }

    static @NotNull String memoryFieldName(int index) {
        return "m" + index;
    }

    static @NotNull String dataFieldName(int index) {
        return "d" + index;
    }

    static @NotNull String elementFieldName(int index) {
        return "e" + index;
    }

    static final String MATH_INTERNAL_NAME = getInternalName(Math.class);
    static final String FLOAT_INTERNAL_NAME = getInternalName(Float.class);
    static final String DOUBLE_INTERNAL_NAME = getInternalName(Double.class);

    static final String INTEGER_INTERNAL_NAME = getInternalName(Integer.class);
    static final String INTEGER_COMPARE_UNSIGNED_NAME = "compareUnsigned";
    static final String INTEGER_COMPARE_UNSIGNED_DESCRIPTOR = methodDescriptor(int.class, int.class, int.class);
    static final String INTEGER_NUMBER_OF_LEADING_ZEROS_NAME = "numberOfLeadingZeros";
    static final String INTEGER_NUMBER_OF_LEADING_ZEROS_DESCRIPTOR = methodDescriptor(int.class, int.class);

    static final String LONG_INTERNAL_NAME = getInternalName(Long.class);
    static final String LONG_COMPARE_UNSIGNED_NAME = "compareUnsigned";
    static final String LONG_COMPARE_UNSIGNED_DESCRIPTOR = methodDescriptor(int.class, long.class, long.class);

    static final String VAR_HANDLE_INTERNAL_NAME = getInternalName(VarHandle.class);
    static final String VAR_HANDLE_DESCRIPTOR = getDescriptor(VarHandle.class);

    static final String METHOD_HANDLE_INTERNAL_NAME = getInternalName(MethodHandle.class);
    static final String METHOD_HANDLE_DESCRIPTOR = getDescriptor(MethodHandle.class);

    static final String MEMORY_SEGMENT_DESCRIPTOR = getDescriptor(MemorySegment.class);
    static final String OBJECT_ARRAY_DESCRIPTOR = getDescriptor(Object[].class);

    static final String GENERATED_INSTANCE_INTERNAL_NAME = "org/wastastic/GeneratedModuleInstance";
    static final String GENERATED_INSTANCE_DESCRIPTOR = "Lorg/wastastic/GeneratedModuleInstance;";
    static final String GENERATED_INSTANCE_CONSTRUCTOR_DESCRIPTOR = methodDescriptor(void.class, Map.class);
}
