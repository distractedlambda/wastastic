package org.wastastic;

import org.jetbrains.annotations.Nullable;

public class ModuleInstantiationException extends Exception {
    ModuleInstantiationException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
