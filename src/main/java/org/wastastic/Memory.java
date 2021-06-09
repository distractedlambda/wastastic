package org.wastastic;

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.VarHandle;

import static java.lang.Integer.compareUnsigned;
import static java.lang.Integer.toUnsignedLong;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static jdk.incubator.foreign.ResourceScope.newImplicitScope;

public final class Memory {
    static final int PAGE_SIZE = 65536;
    static final int MAX_MAX_PAGE_COUNT = (int) (toUnsignedLong(-1) / PAGE_SIZE);

    static final String INTERNAL_NAME = "org/wastastic/Memory";
    static final String DESCRIPTOR = "Lorg/wastastic/Memory;";

    static final VarHandle VH_BYTE = MemoryHandles.varHandle(byte.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_SHORT = MemoryHandles.varHandle(short.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_INT = MemoryHandles.varHandle(int.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_LONG = MemoryHandles.varHandle(long.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_FLOAT = MemoryHandles.varHandle(float.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_DOUBLE = MemoryHandles.varHandle(double.class, 1, LITTLE_ENDIAN);

    final int unsignedMaxPageCount;
    @NotNull MemorySegment segment;

    public Memory(int minPageCount, int unsignedMaxPageCount) {
        if (compareUnsigned(unsignedMaxPageCount, minPageCount) < 0) {
            throw new IllegalArgumentException();
        }

        if (compareUnsigned(unsignedMaxPageCount, MAX_MAX_PAGE_COUNT) > 0) {
            throw new IllegalArgumentException();
        }

        this.segment = MemorySegment.allocateNative(toUnsignedLong(minPageCount) * PAGE_SIZE, 8, newImplicitScope());
        this.unsignedMaxPageCount = unsignedMaxPageCount;
    }

    public Memory(int minPageCount) {
        this(minPageCount, MAX_MAX_PAGE_COUNT);
    }
}
