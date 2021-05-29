package org.wastastic.compiler;

sealed interface Global permits DefinedGlobal, ImportedGlobal {
    GlobalType getType();
}
