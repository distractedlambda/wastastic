package org.wastastic.compiler;

record Limits(int unsignedMinimum, int unsignedMaximum) {
    Limits(int unsignedMinimum) {
        this(unsignedMinimum, -1);
    }
}
