package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;

record TableType(@NotNull ReferenceType elementType, @NotNull Limits limits) {}
