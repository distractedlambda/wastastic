package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;

record Local(@NotNull ValueType type, int index) {}
