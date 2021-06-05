package org.wastastic;

import jdk.incubator.foreign.MemorySegment;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Byte.toUnsignedLong;
import static java.lang.Integer.toUnsignedLong;
import static java.lang.Long.compareUnsigned;
import static java.lang.Short.toUnsignedInt;
import static java.lang.Short.toUnsignedLong;
import static jdk.incubator.foreign.ResourceScope.newImplicitScope;
import static org.wastastic.Memory.PAGE_SIZE;
import static org.wastastic.Memory.VH_BYTE;
import static org.wastastic.Memory.VH_DOUBLE;
import static org.wastastic.Memory.VH_FLOAT;
import static org.wastastic.Memory.VH_INT;
import static org.wastastic.Memory.VH_LONG;
import static org.wastastic.Memory.VH_SHORT;

public abstract class Module {
    private final Memory memory;

    public Module(Memory memory) {
        this.memory = memory;
    }

    private static long effectiveAddress(int address) {
        return Integer.toUnsignedLong(address);
    }

    private static long effectiveAddress(int address, byte offset) {
        return Integer.toUnsignedLong(address) + Byte.toUnsignedLong(offset);
    }

    private static long effectiveAddress(int address, short offset) {
        return Integer.toUnsignedLong(address) + Short.toUnsignedLong(offset);
    }

    private static long effectiveAddress(int address, int offset) {
        return Integer.toUnsignedLong(address) + Integer.toUnsignedLong(offset);
    }

    protected static int i32Load(int address, Module self) throws TrapException {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load(int address, byte offset, Module self) throws TrapException {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load(int address, short offset, Module self) throws TrapException {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load(int address, int offset, Module self) throws TrapException {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load(int address, Module self) throws TrapException {
        try {
            return (long) VH_LONG.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load(int address, byte offset, Module self) throws TrapException {
        try {
            return (long) VH_LONG.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load(int address, short offset, Module self) throws TrapException {
        try {
            return (long) VH_LONG.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load(int address, int offset, Module self) throws TrapException {
        try {
            return (long) VH_LONG.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static float f32Load(int address, Module self) throws TrapException {
        try {
            return (float) VH_FLOAT.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static float f32Load(int address, byte offset, Module self) throws TrapException {
        try {
            return (float) VH_FLOAT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static float f32Load(int address, short offset, Module self) throws TrapException {
        try {
            return (float) VH_FLOAT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static float f32Load(int address, int offset, Module self) throws TrapException {
        try {
            return (float) VH_FLOAT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static double f64Load(int address, Module self) throws TrapException {
        try {
            return (double) VH_DOUBLE.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static double f64Load(int address, byte offset, Module self) throws TrapException {
        try {
            return (double) VH_DOUBLE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static double f64Load(int address, short offset, Module self) throws TrapException {
        try {
            return (double) VH_DOUBLE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static double f64Load(int address, int offset, Module self) throws TrapException {
        try {
            return (double) VH_DOUBLE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8S(int address, Module self) throws TrapException {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8S(int address, byte offset, Module self) throws TrapException {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8S(int address, short offset, Module self) throws TrapException {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8S(int address, int offset, Module self) throws TrapException {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8U(int address, int offset, Module self) throws TrapException {
        try {
            return toUnsignedInt((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16S(int address, int offset, Module self) throws TrapException {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16U(int address, int offset, Module self) throws TrapException {
        try {
            return toUnsignedInt((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8S(int address, int offset, Module self) throws TrapException {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8U(int address, int offset, Module self) throws TrapException {
        try {
            return toUnsignedLong((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16S(int address, int offset, Module self) throws TrapException {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16U(int address, int offset, Module self) throws TrapException {
        try {
            return toUnsignedLong((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32S(int address, int offset, Module self) throws TrapException {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32U(int address, int offset, Module self) throws TrapException {
        try {
            return toUnsignedLong((int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store(int address, int value, int offset, Module self) throws TrapException {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store(int address, long value, int offset, Module self) throws TrapException {
        try {
            VH_LONG.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f32Store(int address, float value, int offset, Module self) throws TrapException {
        try {
            VH_FLOAT.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f64Store(int address, double value, int offset, Module self) throws TrapException {
        try {
            VH_DOUBLE.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store8(int address, int value, int offset, Module self) throws TrapException {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address, offset), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store16(int address, int value, int offset, Module self) throws TrapException {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address, offset), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store8(int address, long value, int offset, Module self) throws TrapException {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address, offset), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store16(int address, long value, int offset, Module self) throws TrapException {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address, offset), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store32(int address, long value, int offset, Module self) throws TrapException {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address, offset), (int) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int memorySize(Module self) {
        return (int) (self.memory.segment.byteSize() / PAGE_SIZE);
    }

    protected static int memoryGrow(int unsignedAdditionalPageCount, Module self) {
        var currentPageCount = self.memory.segment.byteSize() / PAGE_SIZE;
        var newPageCount = currentPageCount + toUnsignedLong(unsignedAdditionalPageCount);

        if (compareUnsigned(newPageCount, toUnsignedLong(self.memory.unsignedMaxPageCount)) > 0) {
            return -1;
        }

        MemorySegment newSegment;
        try {
            newSegment = MemorySegment.allocateNative(newPageCount * PAGE_SIZE, 8, newImplicitScope());
        } catch (OutOfMemoryError ignored) {
            return -1;
        }

        newSegment.copyFrom(self.memory.segment);
        self.memory.segment = newSegment;

        return (int) currentPageCount;
    }

    protected static void fill(int dstAddress, byte fillValue, int unsignedSize, Module self) throws TrapException {
        try {
            self.memory.segment.asSlice(toUnsignedLong(dstAddress), toUnsignedLong(unsignedSize)).fill(fillValue);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void copy(int dstAddress, int srcAddress, int unsignedSize, Module self) throws TrapException {
        try {
            var longSize = toUnsignedLong(unsignedSize);
            var dstSegment = self.memory.segment.asSlice(toUnsignedLong(dstAddress));
            var srcSegment = self.memory.segment.asSlice(toUnsignedLong(srcAddress), longSize);
            dstSegment.copyFrom(srcSegment);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }
}
