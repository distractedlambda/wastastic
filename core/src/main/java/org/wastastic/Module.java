package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public sealed interface Module permits ModuleImpl {
    static @NotNull Module compile(@NotNull MemorySegment segment) throws TranslationException {
        return new ModuleImpl(ModuleIndex.of(segment.asReadOnly()));
    }

    @NotNull ResourceScope scope();

    @NotNull MethodHandle instantiationHandle();

    @NotNull MethodHandle exportedFunctionHandle(@NotNull String name) throws TranslationException;

    @NotNull VarHandle exportedTableHandle(@NotNull String name);

    @NotNull VarHandle exportedMemoryHandle(@NotNull String name);
}
