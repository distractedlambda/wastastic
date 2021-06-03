package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;

record GlobalType(@NotNull ValueType valueType, @NotNull Mutability mutability) {}
