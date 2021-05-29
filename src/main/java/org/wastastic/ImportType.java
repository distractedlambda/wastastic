package org.wastastic;

public sealed interface ImportType permits FunctionType, GlobalType, MemoryType, TableType {}
