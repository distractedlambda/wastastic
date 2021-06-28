package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record ImportedTable(@NotNull QualifiedName name, @NotNull TableType type) {
    ImportedTable {
        requireNonNull(name);
        requireNonNull(type);
    }
}
