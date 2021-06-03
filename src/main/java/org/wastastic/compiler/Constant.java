package org.wastastic.compiler;

sealed interface Constant permits
    F32Constant,
    F64Constant,
    FunctionRefConstant,
    I32Constant,
    I64Constant,
    ImportedGlobalConstant,
    NullConstant
{}
