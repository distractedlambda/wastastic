package org.wastastic.compiler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class ResultTypes {
    private ResultTypes() {}

    static int length(Object types) {
        if (types == null) {
            return 0;
        } else if (types instanceof ValueType) {
            return 1;
        } else {
            return ((ValueType[]) types).length;
        }
    }

    static List<ValueType> asList(Object types) {
        if (types == null) {
            return Collections.emptyList();
        } else if (types instanceof ValueType type) {
            return Collections.singletonList(type);
        } else {
            return Arrays.asList((ValueType[]) types);
        }
    }
}
