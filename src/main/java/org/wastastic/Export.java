package org.wastastic;

import org.jetbrains.annotations.NotNull;

record Export(@NotNull String name, @NotNull ExportKind kind, int index) {}
