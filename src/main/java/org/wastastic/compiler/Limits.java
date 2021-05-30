package org.wastastic.compiler;

record Limits(int unsignedMinimum, int unsignedMaximum) {
    // public Limits {
    //     if (compareUnsigned(unsignedMinimum, unsignedMaximum) > 0) {
    //         throw new IllegalArgumentException(
    //             "Maximum ("
    //                 + Integer.toUnsignedString(unsignedMaximum)
    //                 + ") is not less than or equal to minimum ("
    //                 + Integer.toUnsignedString(unsignedMinimum)
    //                 + ')'
    //         );
    //     }
    // }

    Limits(int unsignedMinimum) {
        this(unsignedMinimum, -1);
    }
}