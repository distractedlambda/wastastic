package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Handle;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.Names.methodDescriptor;

record GeneratedInstanceClassData(
    @NotNull String @NotNull[] functionNames,
    @NotNull FunctionType @NotNull[] functionTypes,
    @NotNull MemorySegment @NotNull[] data,
    @NotNull Constant @NotNull[] @NotNull[] elements
) {

}
