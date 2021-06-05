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
    static final int PAGE_SIZE = 65536;
    static final int MAX_MAX_PAGE_COUNT = (int) (toUnsignedLong(-1) / PAGE_SIZE);

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

    // public static byte loadByte(int address, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         return (byte) VH_BYTE.get(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static short loadShort(int address, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         return (short) VH_SHORT.get(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static int loadInt(int address, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         return (int) VH_INT.get(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static long loadLong(int address, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         return (long) VH_LONG.get(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static float loadFloat(int address, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         return (float) VH_FLOAT.get(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static double loadDouble(int address, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         return (double) VH_DOUBLE.get(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset));
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static void storeByte(int address, byte value, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         VH_BYTE.set(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static void storeShort(int address, short value, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         VH_SHORT.set(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static void storeInt(int address, int value, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         VH_INT.set(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public static void storeLong(int address, long value, int unsignedOffset, @NotNull Memory memory) throws TrapException {
    //     try {
    //         VH_LONG.set(memory.segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public void storeFloat(int address, float value, int unsignedOffset) throws TrapException {
    //     try {
    //         VH_FLOAT.set(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public void storeDouble(int address, double value, int unsignedOffset) throws TrapException {
    //     try {
    //         VH_DOUBLE.set(segment, toUnsignedLong(address) + toUnsignedLong(unsignedOffset), value);
    //     } catch (IndexOutOfBoundsException ignored) {
    //         throw new TrapException();
    //     }
    // }

    // public int loadByteAsSignedInt(int address, int unsignedOffset) throws TrapException {
    //     return loadByte(address, unsignedOffset);
    // }

    // public int loadByteAsUnsignedInt(int address, int unsignedOffset) throws TrapException {
    //     return toUnsignedInt(loadByte(address, unsignedOffset));
    // }

    // public int loadShortAsSignedInt(int address, int unsignedOffset) throws TrapException {
    //     return loadShort(address, unsignedOffset);
    // }

    // public int loadShortAsUnsignedInt(int address, int unsignedOffset) throws TrapException {
    //     return toUnsignedInt(loadShort(address, unsignedOffset));
    // }

    // public long loadByteAsSignedLong(int address, int unsignedOffset) throws TrapException {
    //     return loadByte(address, unsignedOffset);
    // }

    // public long loadByteAsUnsignedLong(int address, int unsignedOffset) throws TrapException {
    //     return toUnsignedLong(loadByte(address, unsignedOffset));
    // }

    // public long loadShortAsSignedLong(int address, int unsignedOffset) throws TrapException {
    //     return loadShort(address, unsignedOffset);
    // }

    // public long loadShortAsUnsignedLong(int address, int unsignedOffset) throws TrapException {
    //     return toUnsignedLong(loadShort(address, unsignedOffset));
    // }

    // public long loadIntAsSignedLong(int address, int unsignedOffset) throws TrapException {
    //     return loadInt(address, unsignedOffset);
    // }

    // public long loadIntAsUnsignedLong(int address, int unsignedOffset) throws TrapException {
    //     return toUnsignedLong(loadInt(address, unsignedOffset));
    // }

    // public void storeIntAsByte(int address, int value, int unsignedOffset) throws TrapException {
    //     storeByte(address, (byte) value, unsignedOffset);
    // }

    // public void storeIntAsShort(int address, int value, int unsignedOffset) throws TrapException {
    //     storeShort(address, (short) value, unsignedOffset);
    // }

    // public void storeLongAsByte(int address, long value, int unsignedOffset) throws TrapException {
    //     storeByte(address, (byte) value, unsignedOffset);
    // }

    // public void storeLongAsShort(int address, long value, int unsignedOffset) throws TrapException {
    //     storeShort(address, (short) value, unsignedOffset);
    // }

    // public void storeLongAsInt(int address, int unsignedOffset, long value) throws TrapException {
    //     storeInt(address, unsignedOffset, (int) value);
    // }

    // public int getUnsignedPageCount() {
    //     return (int) (segment.byteSize() / PAGE_SIZE);
    // }
}
