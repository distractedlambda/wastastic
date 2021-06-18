package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

record LoopScope(@NotNull Label startLabel, @NotNull FunctionType type) implements Scope {}
