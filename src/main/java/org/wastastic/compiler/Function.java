package org.wastastic.compiler;

sealed interface Function permits FunctionType, ImportedFunction {
    FunctionType getType();
}
