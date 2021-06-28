package org.wastastic;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record ImportedMemory(@NotNull QualifiedName name, @NotNull MemoryType type) {
    ImportedMemory {
        requireNonNull(name);
        requireNonNull(type);
    }
}
