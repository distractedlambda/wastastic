package org.wastastic;

import org.jetbrains.annotations.NotNull;

record ExportedFunction(@NotNull String name, @NotNull FunctionType type) {}
