package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;

public record DefinedGlobal(@NotNull GlobalType type, @NotNull Constant initialValue) {}
