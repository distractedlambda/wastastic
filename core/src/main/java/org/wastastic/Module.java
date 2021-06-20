package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public sealed interface Module permits ModuleImpl {
    static @NotNull Module read(@NotNull ReadableByteChannel channel) throws TranslationException, IOException {
        return new ModuleTranslator(new ChannelModuleInput(channel, 256)).translate();
    }

    static @NotNull Module read(@NotNull Path path) throws IOException, TranslationException {
        return read(Files.newByteChannel(path, StandardOpenOption.READ));
    }

    @NotNull MethodHandle instantiationHandle();

    @NotNull MethodHandle exportedFunctionHandle(@NotNull String name);

    @NotNull VarHandle exportedTableHandle(@NotNull String name);

    @NotNull VarHandle exportedMemoryHandle(@NotNull String name);
}
