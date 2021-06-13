package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Handle;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.Names.functionMethodName;
import static org.wastastic.Names.methodDescriptor;

record GeneratedInstanceClassData(
    @NotNull FunctionType @NotNull[] functionTypes,
    @NotNull MemorySegment @NotNull[] data,
    @NotNull Constant @NotNull[] @NotNull[] elements
) {
    static final String INTERNAL_NAME = getInternalName(GeneratedInstanceClassData.class);

    static final Handle DATA_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "dataBootstrap", methodDescriptor(MemorySegment.class, Lookup.class, int.class), false);

    static @NotNull MemorySegment dataBootstrap(@NotNull Lookup lookup, int index) throws IllegalAccessException {
        var classData = MethodHandles.classData(lookup, ConstantDescs.DEFAULT_NAME, GeneratedInstanceClassData.class);
        return classData.data[index];
    }

    static final Handle ELEMENT_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "elementBootstrap", methodDescriptor(Object[].class, Lookup.class, int.class), false);

    static @NotNull Object[] elementBootstrap(@NotNull Lookup lookup, int index) throws IllegalAccessException, NoSuchMethodException {
        var classData = MethodHandles.classData(lookup, ConstantDescs.DEFAULT_NAME, GeneratedInstanceClassData.class);
        var constantValues = classData.elements[index];
        var resolvedValues = new Object[constantValues.length];
        for (var i = 0; i < resolvedValues.length; i++) {
            if (constantValues[i] instanceof FunctionRefConstant functionRefConstant) {
                resolvedValues[i] = lookup.findStatic(lookup.lookupClass(), functionMethodName(index), classData.functionTypes[functionRefConstant.index()].jvmType(lookup.lookupClass()));
            }
            else if (constantValues[i] != NullConstant.INSTANCE) {
                throw new ClassCastException();
            }
        }
        return resolvedValues;
    }
}
