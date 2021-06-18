package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

record BlockScope(@NotNull Label endLabel, @NotNull FunctionType type) implements Scope {}
