package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;

record Local(@NotNull ValueType type, int index) {}
