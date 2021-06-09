package org.wastastic;

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.VarHandle;

import static java.lang.Integer.toUnsignedLong;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static jdk.incubator.foreign.ResourceScope.newImplicitScope;

public final class Memory {
    private static final long PAGE_SIZE = 65536;
    private static final int MAX_MAX_PAGE_COUNT = (int) (toUnsignedLong(-1) / PAGE_SIZE);

    static final String INTERNAL_NAME = "org/wastastic/Memory";
    static final String DESCRIPTOR = "Lorg/wastastic/Memory;";

    static final VarHandle VH_BYTE = MemoryHandles.varHandle(byte.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_SHORT = MemoryHandles.varHandle(short.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_INT = MemoryHandles.varHandle(int.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_LONG = MemoryHandles.varHandle(long.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_FLOAT = MemoryHandles.varHandle(float.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_DOUBLE = MemoryHandles.varHandle(double.class, 1, LITTLE_ENDIAN);

    final int maxPageCount;
    @NotNull MemorySegment segment;

    public Memory(int minPageCount, int maxPageCount) {
        if (minPageCount > maxPageCount) {
            throw new IllegalArgumentException();
        }

        if (maxPageCount > MAX_MAX_PAGE_COUNT) {
            throw new IllegalArgumentException();
        }

        this.segment = MemorySegment.allocateNative(minPageCount * PAGE_SIZE, 8, newImplicitScope());
        this.maxPageCount = maxPageCount;
    }

    public Memory(int minPageCount) {
        this(minPageCount, MAX_MAX_PAGE_COUNT);
    }

    public byte loadByte(long address) {
        return (byte) VH_BYTE.get(segment, address);
    }

    public short loadShort(long address) {
        return (short) VH_SHORT.get(segment, address);
    }

    public int loadInt(long address) {
        return (int) VH_INT.get(segment, address);
    }

    public long loadLong(long address) {
        return (long) VH_LONG.get(segment, address);
    }

    public float loadFloat(long address) {
        return (float) VH_FLOAT.get(segment, address);
    }

    public double loadDouble(long address) {
        return (double) VH_DOUBLE.get(segment, address);
    }

    public void storeByte(long address, byte value) {
        VH_BYTE.set(segment, address, value);
    }

    public void storeShort(long address, short value) {
        VH_SHORT.set(segment, address, value);
    }

    public void storeInt(long address, int value) {
        VH_INT.set(segment, address, value);
    }

    public void storeLong(long address, long value) {
        VH_LONG.set(segment, address, value);
    }

    public void storeFloat(long address, float value) {
        VH_FLOAT.set(segment, address, value);
    }

    public void storeDouble(long address, double value) {
        VH_DOUBLE.set(segment, address, value);
    }

    public int size() {
        return (int) (segment.byteSize() / PAGE_SIZE);
    }

    public int grow(int additionalPages) {
        var segment = this.segment;
        var currentPageCount = segment.byteSize() / PAGE_SIZE;

        if (additionalPages == 0) {
            return (int) currentPageCount;
        }

        var newPageCount = currentPageCount + Integer.toUnsignedLong(additionalPages);

        if (newPageCount > maxPageCount) {
            return -1;
        }

        MemorySegment newSegment;
        try {
            newSegment = MemorySegment.allocateNative(newPageCount * PAGE_SIZE, 8, newImplicitScope());
        } catch (OutOfMemoryError ignored) {
            return -1;
        }

        newSegment.copyFrom(segment);
        this.segment = newSegment;

        return (int) currentPageCount;
    }

    public void init(long dstAddress, long srcAddress, long size, @NotNull MemorySegment src) {
        var dstSlice = segment.asSlice(dstAddress, size);
        var srcSlice = src.asSlice(srcAddress, size);
        dstSlice.copyFrom(srcSlice);
    }

    public void fill(long dstAddress, byte fillValue, long size) {
        var dst = segment.asSlice(dstAddress, size);
        dst.fill(fillValue);
    }

    public void copy(long dstAddress, long srcAddress, long size) {
        var dstSegment = segment.asSlice(dstAddress);
        var srcSegment = segment.asSlice(srcAddress, size);
        dstSegment.copyFrom(srcSegment);
    }
}
