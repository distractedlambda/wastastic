package org.wastastic;

import org.jetbrains.annotations.NotNull;

record GlobalType(@NotNull ValueType valueType, @NotNull Mutability mutability) {}
