package org.wastastic;

import org.jetbrains.annotations.NotNull;

sealed interface Scope extends StackEntry permits BlockScope, IfScope, LoopScope {
    @NotNull FunctionType type();
}
