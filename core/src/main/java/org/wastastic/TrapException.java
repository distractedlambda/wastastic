package org.wastastic;

import org.jetbrains.annotations.Nullable;

public final class TrapException extends Exception {
    TrapException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    @Override public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
