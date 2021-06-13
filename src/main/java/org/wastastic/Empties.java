package org.wastastic;

import jdk.incubator.foreign.MemorySegment;

import static jdk.incubator.foreign.ResourceScope.newImplicitScope;
import static org.objectweb.asm.Type.getInternalName;

final class Empties {
    private Empties() {}

    static final String INTERNAL_NAME = getInternalName(Empties.class);

    static final String EMPTY_DATA_NAME = "EMPTY_DATA";
    static final MemorySegment EMPTY_DATA = MemorySegment.allocateNative(1, newImplicitScope()).asSlice(0, 0);

    static final String EMPTY_ELEMENTS_NAME = "EMPTY_ELEMENTS";
    static final Object[] EMPTY_ELEMENTS = new Object[0];
}
