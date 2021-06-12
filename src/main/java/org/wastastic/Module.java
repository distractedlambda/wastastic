package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public sealed interface Module permits ModuleImpl {
    @NotNull MethodHandle instantiationHandle();

    @NotNull MethodHandle exportedFunctionHandle(@NotNull String name);

    @NotNull VarHandle exportedTableHandle(@NotNull String name);

    @NotNull VarHandle exportedMemoryHandle(@NotNull String name);
}
