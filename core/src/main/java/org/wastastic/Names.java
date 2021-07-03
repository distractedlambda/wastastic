package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Map;

import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;

final class Names {
    private Names() {}

    static @NotNull String functionName(int index) {
        return "function-" + index;
    }

    static @NotNull String memoryName(int index) {
        return "memory-" + index;
    }

    static @NotNull String tableName(int index) {
        return "table-" + index;
    }

    static @NotNull String globalName(int index) {
        return "global-" + index;
    }

    static @NotNull String dataSegmentName(int index) {
        return "data-" + index;
    }

    static @NotNull String elementSegmentName(int index) {
        return "element-" + index;
    }

    static @NotNull String methodDescriptor(@NotNull Class<?> returnType, @NotNull Class<?> @NotNull... argumentTypes) {
        var builder = new StringBuilder("(");

        for (var argumentType : argumentTypes) {
            builder.append(getDescriptor(argumentType));
        }

        return builder.append(")").append(getDescriptor(returnType)).toString();
    }

    static final String DOUBLE_INTERNAL_NAME = getInternalName(Double.class);
    static final String FLOAT_INTERNAL_NAME = getInternalName(Float.class);
    static final String FUNCTION_CLASS_ENTRY_NAME = "entry";
    static final String GENERATED_FUNCTION_INTERNAL_NAME = "org/wastastic/GeneratedFunction";
    static final String GENERATED_INSTANCE_INTERNAL_NAME = "org/wastastic/GeneratedModuleInstance";
    static final String INTEGER_INTERNAL_NAME = getInternalName(Integer.class);
    static final String LONG_INTERNAL_NAME = getInternalName(Long.class);
    static final String MATH_INTERNAL_NAME = getInternalName(Math.class);
    static final String METHOD_HANDLE_INTERNAL_NAME = getInternalName(MethodHandle.class);
    static final String MODULE_INSTANCE_INTERNAL_NAME = getInternalName(ModuleInstance.class);
    static final String OBJECT_INTERNAL_NAME = getInternalName(Object.class);
    static final String WRONG_METHOD_TYPE_EXCEPTION_INTERNAL_NAME = getInternalName(WrongMethodTypeException.class);

    static final String MEMORY_SEGMENT_DESCRIPTOR = getDescriptor(MemorySegment.class);
    static final String METHOD_HANDLE_DESCRIPTOR = getDescriptor(MethodHandle.class);
    static final String MODULE_INSTANCE_DESCRIPTOR = getDescriptor(ModuleInstance.class);
    static final String OBJECT_ARRAY_DESCRIPTOR = getDescriptor(Object[].class);

    static final String GENERATED_INSTANCE_CONSTRUCTOR_DESCRIPTOR = methodDescriptor(void.class, Map.class);
}
