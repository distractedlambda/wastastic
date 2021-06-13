package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public sealed interface Module permits ModuleImpl {
    static @NotNull Module read(@NotNull ModuleReader reader) throws TranslationException, IOException {
        return new ModuleTranslator(reader).translate();
    }

    static @NotNull Module read(@NotNull InputStream source) throws TranslationException, IOException {
        return read(new InputStreamModuleReader(source));
    }

    static @NotNull Module read(@NotNull File file) throws IOException, TranslationException {
        return read(new BufferedInputStream(new FileInputStream(file)));
    }

    @NotNull MethodHandle instantiationHandle();

    @NotNull MethodHandle exportedFunctionHandle(@NotNull String name);

    @NotNull VarHandle exportedTableHandle(@NotNull String name);

    @NotNull VarHandle exportedMemoryHandle(@NotNull String name);
}
