package org.wastastic;

import org.jetbrains.annotations.NotNull;

record DefinedGlobal(@NotNull GlobalType type, @NotNull Constant initialValue) {}
