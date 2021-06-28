package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record Export(@NotNull String name, @NotNull ExportKind kind, int index) {
    Export {
        requireNonNull(name);
        requireNonNull(kind);
    }
}
