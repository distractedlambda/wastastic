package org.wastastic.compiler;

final class NullConstant implements Constant {
    private NullConstant() {}

    static final NullConstant INSTANCE = new NullConstant();
}
