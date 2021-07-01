package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.Names.methodDescriptor;

public final class TrapException extends Exception {
    TrapException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        setStackTrace(ModuleImpl.recoverStackTrace());
    }

    static final String INTERNAL_NAME = getInternalName(TrapException.class);

    static final String UNREACHABLE_NAME = "unreachable";
    static final String UNREACHABLE_DESCRIPTOR = methodDescriptor(TrapException.class);

    static @NotNull TrapException unreachable() {
        return new TrapException("unreachable instruction executed", null);
    }

    @Override public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
