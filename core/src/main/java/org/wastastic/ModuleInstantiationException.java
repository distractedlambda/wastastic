package org.wastastic;

import org.jetbrains.annotations.Nullable;

import static org.objectweb.asm.Type.getInternalName;

public class ModuleInstantiationException extends Exception {
    ModuleInstantiationException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    static final String INTERNAL_NAME = getInternalName(ModuleInstantiationException.class);
}
