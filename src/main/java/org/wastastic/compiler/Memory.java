package org.wastastic.compiler;

sealed interface Memory permits MemoryType, ImportedMemory {
    MemoryType getType();
}
