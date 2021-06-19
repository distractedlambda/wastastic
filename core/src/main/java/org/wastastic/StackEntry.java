package org.wastastic;

sealed interface StackEntry permits Scope, ValueType {}
