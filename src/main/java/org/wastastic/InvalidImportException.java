package org.wastastic;

import org.jetbrains.annotations.Nullable;

public class InvalidImportException extends ModuleInstantiationException {
    InvalidImportException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
