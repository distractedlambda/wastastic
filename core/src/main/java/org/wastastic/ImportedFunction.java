package org.wastastic;

import org.jetbrains.annotations.NotNull;

record ImportedFunction(@NotNull QualifiedName qualifiedName, @NotNull FunctionType type) {}
