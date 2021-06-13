package org.wastastic;

import org.jetbrains.annotations.NotNull;

record TableType(@NotNull ValueType elementType, @NotNull Limits limits) {}
