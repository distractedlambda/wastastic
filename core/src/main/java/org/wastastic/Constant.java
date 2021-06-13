package org.wastastic;

sealed interface Constant permits
    F32Constant,
    F64Constant,
    FunctionRefConstant,
    I32Constant,
    I64Constant,
    NullConstant
{}
