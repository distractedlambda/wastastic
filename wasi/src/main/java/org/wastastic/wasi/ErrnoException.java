package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;

public final class ErrnoException extends Exception {
    private final @NotNull Errno error;

    public ErrnoException(@NotNull Errno error) {
        super(null, null, false, false);
        this.error = requireNonNull(error);
    }

    static final @NotNull MethodHandle CODE_HANDLE;

    static {
        var lookup = MethodHandles.lookup();
        try {
            var errnoHandle = lookup.findGetter(ErrnoException.class, "error", Errno.class);
            var errnoOrdinalHandle = lookup.findVirtual(Errno.class, "ordinal", methodType(int.class));
            CODE_HANDLE = filterReturnValue(errnoHandle, errnoOrdinalHandle);
        }
        catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException exception) {
            throw new UnsupportedOperationException(exception);
        }
    }
}
