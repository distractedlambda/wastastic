package org.wastastic.translator;

record ElementSegment(Constant[] values, int tableIndex, int tableOffset) {
    ElementSegment(Constant[] values) {
        this(values, -1, -1);
    }
}
