package org.wastastic;

import org.jetbrains.annotations.NotNull;

record ExportedFunction(int index, @NotNull FunctionType type) {}
