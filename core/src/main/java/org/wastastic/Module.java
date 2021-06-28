package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public sealed interface Module permits ModuleClassLoader {
    static @NotNull Module read(@NotNull Path path) throws IOException, TranslationException {
        MemorySegment copy;

        try (var scope = ResourceScope.newConfinedScope()) {
            var mapped = MemorySegment.mapFile(path, 0, Files.size(path), FileChannel.MapMode.READ_ONLY, scope);
            copy = MemorySegment.allocateNative(mapped.byteSize(), 8, ResourceScope.newImplicitScope());
            copy.copyFrom(mapped);
        }

        return new ModuleClassLoader(new ModuleParser().parse(copy));
    }

    @NotNull MethodHandle instantiationHandle();

    @NotNull MethodHandle exportedFunctionHandle(@NotNull String name);

    @NotNull VarHandle exportedTableHandle(@NotNull String name);

    @NotNull VarHandle exportedMemoryHandle(@NotNull String name);
}
