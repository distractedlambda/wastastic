package org.wastastic;

record Limits(int unsignedMinimum, int unsignedMaximum) {
    Limits(int unsignedMinimum) {
        this(unsignedMinimum, -1);
    }
}
