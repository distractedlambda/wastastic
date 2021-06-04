package org.wastastic;

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.VarHandle;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Byte.toUnsignedLong;
import static java.lang.Integer.compareUnsigned;
import static java.lang.Integer.toUnsignedLong;
import static java.lang.Long.compareUnsigned;
import static java.lang.Short.toUnsignedInt;
import static java.lang.Short.toUnsignedLong;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static jdk.incubator.foreign.ResourceScope.newImplicitScope;

public final class Memory {
    public static final int PAGE_SIZE = 65536;
    public static final int MAX_MAX_PAGE_COUNT = (int) (toUnsignedLong(-1) / PAGE_SIZE);

    private static final VarHandle VH_BYTE = MemoryHandles.varHandle(byte.class, 1, LITTLE_ENDIAN);
    private static final VarHandle VH_SHORT = MemoryHandles.varHandle(short.class, 1, LITTLE_ENDIAN);
    private static final VarHandle VH_INT = MemoryHandles.varHandle(int.class, 1, LITTLE_ENDIAN);
    private static final VarHandle VH_LONG = MemoryHandles.varHandle(long.class, 1, LITTLE_ENDIAN);
    private static final VarHandle VH_FLOAT = MemoryHandles.varHandle(float.class, 1, LITTLE_ENDIAN);
    private static final VarHandle VH_DOUBLE = MemoryHandles.varHandle(double.class, 1, LITTLE_ENDIAN);

    private final int unsignedMaxPageCount;
    private @NotNull MemorySegment segment;

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

    public byte loadByte(int address, int unsignedOffset) throws TrapException {
        try {
            return (byte) VH_BYTE.get(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public short loadShort(int address, int unsignedOffset) throws TrapException {
        try {
            return (short) VH_SHORT.get(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public int loadInt(int address, int unsignedOffset) throws TrapException {
        try {
            return (int) VH_INT.get(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public long loadLong(int address, int unsignedOffset) throws TrapException {
        try {
            return (long) VH_LONG.get(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public float loadFloat(int address, int unsignedOffset) throws TrapException {
        try {
            return (float) VH_FLOAT.get(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public double loadDouble(int address, int unsignedOffset) throws TrapException {
        try {
            return (double) VH_DOUBLE.get(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public void storeByte(int address, int unsignedOffset, byte value) throws TrapException {
        try {
            VH_BYTE.set(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public void storeShort(int address, int unsignedOffset, short value) throws TrapException {
        try {
            VH_SHORT.set(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public void storeInt(int address, int unsignedOffset, int value) throws TrapException {
        try {
            VH_INT.set(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public void storeLong(int address, int unsignedOffset, long value) throws TrapException {
        try {
            VH_LONG.set(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public void storeFloat(int address, int unsignedOffset, float value) throws TrapException {
        try {
            VH_FLOAT.set(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public void storeDouble(int address, int unsignedOffset, double value) throws TrapException {
        try {
            VH_DOUBLE.set(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public int loadByteAsSignedInt(int address, int unsignedOffset) throws TrapException {
        return loadByte(address, unsignedOffset);
    }

    public int loadByteAsUnsignedInt(int address, int unsignedOffset) throws TrapException {
        return toUnsignedInt(loadByte(address, unsignedOffset));
    }

    public int loadShortAsSignedInt(int address, int unsignedOffset) throws TrapException {
        return loadShort(address, unsignedOffset);
    }

    public int loadShortAsUnsignedInt(int address, int unsignedOffset) throws TrapException {
        return toUnsignedInt(loadShort(address, unsignedOffset));
    }

    public long loadByteAsSignedLong(int address, int unsignedOffset) throws TrapException {
        return loadByte(address, unsignedOffset);
    }

    public long loadByteAsUnsignedLong(int address, int unsignedOffset) throws TrapException {
        return toUnsignedLong(loadByte(address, unsignedOffset));
    }

    public long loadShortAsSignedLong(int address, int unsignedOffset) throws TrapException {
        return loadShort(address, unsignedOffset);
    }

    public long loadShortAsUnsignedLong(int address, int unsignedOffset) throws TrapException {
        return toUnsignedLong(loadShort(address, unsignedOffset));
    }

    public long loadIntAsSignedLong(int address, int unsignedOffset) throws TrapException {
        return loadInt(address, unsignedOffset);
    }

    public long loadIntAsUnsignedLong(int address, int unsignedOffset) throws TrapException {
        return toUnsignedLong(loadInt(address, unsignedOffset));
    }

    public void storeIntAsByte(int address, int unsignedOffset, int value) throws TrapException {
        storeByte(address, unsignedOffset, (byte) value);
    }

    public void storeIntAsShort(int address, int unsignedOffset, int value) throws TrapException {
        storeShort(address, unsignedOffset, (short) value);
    }

    public void storeLongAsByte(int address, int unsignedOffset, long value) throws TrapException {
        storeByte(address, unsignedOffset, (byte) value);
    }

    public void storeLongAsShort(int address, int unsignedOffset, long value) throws TrapException {
        storeShort(address, unsignedOffset, (short) value);
    }

    public void storeLongAsInt(int address, int unsignedOffset, long value) throws TrapException {
        storeInt(address, unsignedOffset, (int) value);
    }

    public int getUnsignedPageCount() {
        return (int) (segment.byteSize() / PAGE_SIZE);
    }

    public int grow(int unsignedAdditionalPageCount) {
        var currentPageCount = segment.byteSize() / PAGE_SIZE;
        var newPageCount = currentPageCount + toUnsignedLong(unsignedAdditionalPageCount);

        if (compareUnsigned(newPageCount, toUnsignedLong(unsignedMaxPageCount)) > 0) {
            return -1;
        }

        MemorySegment newSegment;
        try {
            newSegment = MemorySegment.allocateNative(newPageCount * PAGE_SIZE, 8, newImplicitScope());
        } catch (OutOfMemoryError ignored) {
            return -1;
        }

        newSegment.copyFrom(segment);
        segment = newSegment;

        return (int) currentPageCount;
    }

    public void fill(int dstAddress, byte fillValue, int unsignedSize) throws TrapException {
        try {
            segment.asSlice(toUnsignedLong(dstAddress), toUnsignedLong(unsignedSize)).fill(fillValue);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    public void copy(int dstAddress, int srcAddress, int unsignedSize) throws TrapException {
        try {
            var longSize = toUnsignedLong(unsignedSize);
            var dstSegment = segment.asSlice(toUnsignedLong(dstAddress));
            var srcSegment = segment.asSlice(toUnsignedLong(srcAddress), longSize);
            dstSegment.copyFrom(srcSegment);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }
}
