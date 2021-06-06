package org.wastastic.translator;

final class NullConstant implements Constant {
    private NullConstant() {}

    static final NullConstant INSTANCE = new NullConstant();
}
