package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;

record Export(@NotNull String name, @NotNull ExportKind kind, int index) {}
