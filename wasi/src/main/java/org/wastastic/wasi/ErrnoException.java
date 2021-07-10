package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

final class ErrnoException extends Exception {
    private final int code;

    ErrnoException(int code) {
        super(null, null, false, false);
        this.code = code;
    }

    static final @NotNull MethodHandle CODE_HANDLE;

    static {
        try {
            CODE_HANDLE = MethodHandles.lookup().findGetter(ErrnoException.class, "code", int.class);
        }
        catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new UnsupportedOperationException(exception);
        }
    }
}
