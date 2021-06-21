package org.wastastic;

import org.jetbrains.annotations.NotNull;

record ImportedTable(@NotNull QualifiedName name, @NotNull TableType type) {}
