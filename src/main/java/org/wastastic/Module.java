package org.wastastic;

import jdk.incubator.foreign.MemorySegment;

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

    protected static final Object[] EMPTY_ELEMENT = new Object[0];

    protected static final MemorySegment EMPTY_DATA = MemorySegment.allocateNative(1, newImplicitScope()).asSlice(0, 0);

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

    protected static int i32Load(int address, Module self) {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load(int address, byte offset, Module self) {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load(int address, short offset, Module self) {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load(int address, int offset, Module self) {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load(int address, Module self) {
        try {
            return (long) VH_LONG.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load(int address, byte offset, Module self) {
        try {
            return (long) VH_LONG.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load(int address, short offset, Module self) {
        try {
            return (long) VH_LONG.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load(int address, int offset, Module self) {
        try {
            return (long) VH_LONG.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static float f32Load(int address, Module self) {
        try {
            return (float) VH_FLOAT.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static float f32Load(int address, byte offset, Module self) {
        try {
            return (float) VH_FLOAT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static float f32Load(int address, short offset, Module self) {
        try {
            return (float) VH_FLOAT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static float f32Load(int address, int offset, Module self) {
        try {
            return (float) VH_FLOAT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static double f64Load(int address, Module self) {
        try {
            return (double) VH_DOUBLE.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static double f64Load(int address, byte offset, Module self) {
        try {
            return (double) VH_DOUBLE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static double f64Load(int address, short offset, Module self) {
        try {
            return (double) VH_DOUBLE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static double f64Load(int address, int offset, Module self) {
        try {
            return (double) VH_DOUBLE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8S(int address, Module self) {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8S(int address, byte offset, Module self) {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8S(int address, short offset, Module self) {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8S(int address, int offset, Module self) {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8U(int address, Module self) {
        try {
            return Byte.toUnsignedInt((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8U(int address, byte offset, Module self) {
        try {
            return Byte.toUnsignedInt((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8U(int address, short offset, Module self) {
        try {
            return Byte.toUnsignedInt((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load8U(int address, int offset, Module self) {
        try {
            return Byte.toUnsignedInt((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16S(int address, Module self) {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16S(int address, byte offset, Module self) {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16S(int address, short offset, Module self) {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16S(int address, int offset, Module self) {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16U(int address, Module self) {
        try {
            return Short.toUnsignedInt((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16U(int address, byte offset, Module self) {
        try {
            return Short.toUnsignedInt((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16U(int address, short offset, Module self) {
        try {
            return Short.toUnsignedInt((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int i32Load16U(int address, int offset, Module self) {
        try {
            return Short.toUnsignedInt((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8S(int address, Module self) {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8S(int address, byte offset, Module self) {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8S(int address, short offset, Module self) {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8S(int address, int offset, Module self) {
        try {
            return (byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8U(int address, Module self) {
        try {
            return Byte.toUnsignedLong((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8U(int address, byte offset, Module self) {
        try {
            return Byte.toUnsignedLong((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8U(int address, short offset, Module self) {
        try {
            return Byte.toUnsignedLong((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load8U(int address, int offset, Module self) {
        try {
            return Byte.toUnsignedLong((byte) VH_BYTE.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16S(int address, Module self) {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16S(int address, byte offset, Module self) {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16S(int address, short offset, Module self) {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16S(int address, int offset, Module self) {
        try {
            return (short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16U(int address, Module self) {
        try {
            return Short.toUnsignedLong((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16U(int address, byte offset, Module self) {
        try {
            return Short.toUnsignedLong((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16U(int address, short offset, Module self) {
        try {
            return Short.toUnsignedLong((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load16U(int address, int offset, Module self) {
        try {
            return Short.toUnsignedLong((short) VH_SHORT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32S(int address, Module self) {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32S(int address, byte offset, Module self) {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32S(int address, short offset, Module self) {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32S(int address, int offset, Module self) {
        try {
            return (int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32U(int address, Module self) {
        try {
            return Integer.toUnsignedLong((int) VH_INT.get(self.memory.segment, effectiveAddress(address)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32U(int address, byte offset, Module self) {
        try {
            return Integer.toUnsignedLong((int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32U(int address, short offset, Module self) {
        try {
            return Integer.toUnsignedLong((int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static long i64Load32U(int address, int offset, Module self) {
        try {
            return Integer.toUnsignedLong((int) VH_INT.get(self.memory.segment, effectiveAddress(address, offset)));
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store(int address, int value, Module self) {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store(int address, int value, byte offset, Module self) {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store(int address, int value, short offset, Module self) {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store(int address, int value, int offset, Module self) {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store(int address, long value, Module self) {
        try {
            VH_LONG.set(self.memory.segment, effectiveAddress(address), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store(int address, long value, byte offset, Module self) {
        try {
            VH_LONG.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store(int address, long value, short offset, Module self) {
        try {
            VH_LONG.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store(int address, long value, int offset, Module self) {
        try {
            VH_LONG.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f32Store(int address, float value, Module self) {
        try {
            VH_FLOAT.set(self.memory.segment, effectiveAddress(address), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f32Store(int address, float value, byte offset, Module self) {
        try {
            VH_FLOAT.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f32Store(int address, float value, short offset, Module self) {
        try {
            VH_FLOAT.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f32Store(int address, float value, int offset, Module self) {
        try {
            VH_FLOAT.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f64Store(int address, double value, Module self) {
        try {
            VH_DOUBLE.set(self.memory.segment, effectiveAddress(address), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f64Store(int address, double value, byte offset, Module self) {
        try {
            VH_DOUBLE.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f64Store(int address, double value, short offset, Module self) {
        try {
            VH_DOUBLE.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void f64Store(int address, double value, int offset, Module self) {
        try {
            VH_DOUBLE.set(self.memory.segment, effectiveAddress(address, offset), value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store8(int address, int value, Module self) {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store8(int address, int value, byte offset, Module self) {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address, offset), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store8(int address, int value, short offset, Module self) {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address, offset), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store8(int address, int value, int offset, Module self) {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address, offset), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store16(int address, int value, Module self) {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store16(int address, int value, byte offset, Module self) {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address, offset), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store16(int address, int value, short offset, Module self) {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address, offset), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i32Store16(int address, int value, int offset, Module self) {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address, offset), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store8(int address, long value, Module self) {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store8(int address, long value, byte offset, Module self) {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address, offset), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store8(int address, long value, short offset, Module self) {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address, offset), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store8(int address, long value, int offset, Module self) {
        try {
            VH_BYTE.set(self.memory.segment, effectiveAddress(address, offset), (byte) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store16(int address, long value, Module self) {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store16(int address, long value, byte offset, Module self) {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address, offset), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store16(int address, long value, short offset, Module self) {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address, offset), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store16(int address, long value, int offset, Module self) {
        try {
            VH_SHORT.set(self.memory.segment, effectiveAddress(address, offset), (short) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store32(int address, long value, Module self) {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address), (int) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store32(int address, long value, byte offset, Module self) {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address, offset), (int) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store32(int address, long value, short offset, Module self) {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address, offset), (int) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void i64Store32(int address, long value, int offset, Module self) {
        try {
            VH_INT.set(self.memory.segment, effectiveAddress(address, offset), (int) value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static int memorySize(Module self) {
        return (int) (self.memory.segment.byteSize() / PAGE_SIZE);
    }

    protected static int memoryGrow(int additionalPages, Module self) {
        var currentPageCount = self.memory.segment.byteSize() / PAGE_SIZE;
        var newPageCount = currentPageCount + Integer.toUnsignedLong(additionalPages);

        if (Long.compareUnsigned(newPageCount, Integer.toUnsignedLong(self.memory.unsignedMaxPageCount)) > 0) {
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

    protected static void memoryInit(int dstAddress, int srcAddress, int size, Module self, MemorySegment src) {
        var longSize = Integer.toUnsignedLong(size);
        try {
            var dstSlice = self.memory.segment.asSlice(Integer.toUnsignedLong(dstAddress), longSize);
            var srcSlice = src.asSlice(Integer.toUnsignedLong(srcAddress), longSize);
            dstSlice.copyFrom(srcSlice);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void memoryFill(int dstAddress, byte fillValue, int size, Module self) {
        try {
            var dst = self.memory.segment.asSlice(Integer.toUnsignedLong(dstAddress), Integer.toUnsignedLong(size));
            dst.fill(fillValue);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    protected static void memoryCopy(int dstAddress, int srcAddress, int size, Module self) {
        try {
            var longSize = Integer.toUnsignedLong(size);
            var dstSegment = self.memory.segment.asSlice(Integer.toUnsignedLong(dstAddress));
            var srcSegment = self.memory.segment.asSlice(Integer.toUnsignedLong(srcAddress), longSize);
            dstSegment.copyFrom(srcSegment);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }
}
