package org.wastastic;

import org.jetbrains.annotations.NotNull;

record ImportedMemory(@NotNull QualifiedName name, @NotNull MemoryType type) {}
