package org.wastastic;

import org.jetbrains.annotations.NotNull;

record ImportedGlobal(@NotNull QualifiedName name, @NotNull GlobalType type) {}
