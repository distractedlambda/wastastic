package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;

record GlobalType(@NotNull ValueType valueType, @NotNull Mutability mutability) {}
