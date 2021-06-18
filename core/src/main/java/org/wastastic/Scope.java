package org.wastastic;

import org.jetbrains.annotations.NotNull;

sealed interface Scope permits BlockScope, IfScope, LoopScope {
    @NotNull FunctionType type();
}
