package org.wastastic;

import org.jetbrains.annotations.NotNull;

public class MissingImportException extends ModuleInstantiationException {
    MissingImportException(@NotNull QualifiedName name) {
        super("Missing value for import", null);
    }
}
