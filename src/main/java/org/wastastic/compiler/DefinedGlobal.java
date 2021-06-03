package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;

public record DefinedGlobal(@NotNull GlobalType type, @NotNull Constant initialValue) {}
