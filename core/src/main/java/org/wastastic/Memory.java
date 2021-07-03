package org.wastastic;

import jdk.incubator.foreign.MemoryHandles;
import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.VarHandle;

import static java.lang.Integer.compareUnsigned;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static jdk.incubator.foreign.ResourceScope.newImplicitScope;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.Names.methodDescriptor;

public final class Memory {
    private static final long PAGE_SIZE = 65536;

    static final String INTERNAL_NAME = getInternalName(Memory.class);
    static final String DESCRIPTOR = getDescriptor(Memory.class);

    static final VarHandle VH_BYTE = MemoryHandles.varHandle(byte.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_SHORT = MemoryHandles.varHandle(short.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_INT = MemoryHandles.varHandle(int.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_LONG = MemoryHandles.varHandle(long.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_FLOAT = MemoryHandles.varHandle(float.class, 1, LITTLE_ENDIAN);
    static final VarHandle VH_DOUBLE = MemoryHandles.varHandle(double.class, 1, LITTLE_ENDIAN);

    private final int maxPageCount;
    private @NotNull MemorySegment segment;

    public Memory(int minPageCount, int maxPageCount) {
        if (compareUnsigned(minPageCount, maxPageCount) > 0) {
            throw new IllegalArgumentException();
        }

        this.segment = MemorySegment.allocateNative(minPageCount * PAGE_SIZE, 8, newImplicitScope());
        this.maxPageCount = maxPageCount;
    }

    public Memory(int minPageCount) {
        this(minPageCount, -1);
    }

    public long byteSize() {
        return segment.byteSize();
    }

    public byte getByte(int address) {
        return (byte) VH_BYTE.get(segment, Integer.toUnsignedLong(address));
    }

    public short getShort(int address) {
        return (short) VH_SHORT.get(segment, Integer.toUnsignedLong(address));
    }

    public int getInt(int address) {
        return (int) VH_INT.get(segment, Integer.toUnsignedLong(address));
    }

    public long getLong(int address) {
        return (long) VH_LONG.get(segment, Integer.toUnsignedLong(address));
    }

    public float getFloat(int address) {
        return (float) VH_FLOAT.get(segment, Integer.toUnsignedLong(address));
    }

    public double getDouble(int address) {
        return (double) VH_DOUBLE.get(segment, Integer.toUnsignedLong(address));
    }

    public byte @NotNull[] getBytes(int address, byte @NotNull[] destination, int start, int length) {
        MemorySegment
            .ofArray(destination)
            .asSlice(start)
            .copyFrom(segment.asSlice(Integer.toUnsignedLong(address), length));

        return destination;
    }

    public byte @NotNull[] getBytes(int address, byte @NotNull[] destination, int start) {
        MemorySegment
            .ofArray(destination)
            .asSlice(start)
            .copyFrom(segment.asSlice(Integer.toUnsignedLong(address), destination.length - start));

        return destination;
    }

    public byte @NotNull[] getBytes(int address, byte @NotNull[] destination) {
        MemorySegment
            .ofArray(destination)
            .copyFrom(segment.asSlice(Integer.toUnsignedLong(address), destination.length));

        return destination;
    }

    public void setByte(int address, byte value) {
        VH_BYTE.set(segment, Integer.toUnsignedLong(address), value);
    }

    public void setShort(int address, short value) {
        VH_SHORT.set(segment, Integer.toUnsignedLong(address), value);
    }

    public void setInt(int address, int value) {
        VH_INT.set(segment, Integer.toUnsignedLong(address), value);
    }

    public void setLong(int address, long value) {
        VH_LONG.set(segment, Integer.toUnsignedLong(address), value);
    }

    public void setFloat(int address, float value) {
        VH_FLOAT.set(segment, Integer.toUnsignedLong(address), value);
    }

    public void setDouble(int address, double value) {
        VH_DOUBLE.set(segment, Integer.toUnsignedLong(address), value);
    }

    public void setBytes(int address, byte @NotNull[] bytes, int start, int length) {
        segment
            .asSlice(Integer.toUnsignedLong(address))
            .copyFrom(MemorySegment.ofArray(bytes).asSlice(start, length));
    }

    public void setBytes(int address, byte @NotNull[] bytes, int start) {
        segment
            .asSlice(Integer.toUnsignedLong(address))
            .copyFrom(MemorySegment.ofArray(bytes).asSlice(start));
    }

    public void setBytes(int address, byte @NotNull[] bytes) {
        segment
            .asSlice(Integer.toUnsignedLong(address))
            .copyFrom(MemorySegment.ofArray(bytes));
    }

    private static long effectiveAddress(int address, int offset) {
        return Integer.toUnsignedLong(address) + Integer.toUnsignedLong(offset);
    }

    static final String I32_LOAD_NAME = "i32Load";

    @SuppressWarnings("unused")
    static int i32Load(int address, int offset, @NotNull Memory self) throws TrapException {
        try {
            return (int) VH_INT.get(self.segment, effectiveAddress(address, offset));
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String I64_LOAD_NAME = "i64Load";

    @SuppressWarnings("unused")
    static long i64Load(int address, int offset, @NotNull Memory self) throws TrapException {
        try {
            return (long) VH_LONG.get(self.segment, effectiveAddress(address, offset));
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String F32_LOAD_NAME = "f32Load";

    @SuppressWarnings("unused")
    static float f32Load(int address, int offset, @NotNull Memory self) throws TrapException {
        try {
            return (float) VH_FLOAT.get(self.segment, effectiveAddress(address, offset));
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String F64_LOAD_NAME = "f64Load";

    @SuppressWarnings("unused")
    static double f64Load(int address, int offset, @NotNull Memory self) throws TrapException {
        try {
            return (double) VH_DOUBLE.get(self.segment, effectiveAddress(address, offset));
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String I32_LOAD_8_S_NAME = "i32Load8S";

    @SuppressWarnings("unused")
    static byte i32Load8S(int address, int offset, @NotNull Memory self) throws TrapException {
        try {
            return (byte) VH_BYTE.get(self.segment, effectiveAddress(address, offset));
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String I32_LOAD_8_U_NAME = "i32Load8U";

    @SuppressWarnings("unused")
    static int i32Load8U(int address, int offset, @NotNull Memory self) throws TrapException {
        return Byte.toUnsignedInt(i32Load8S(address, offset, self));
    }

    static final String I32_LOAD_16_S_NAME = "i32Load16S";

    @SuppressWarnings("unused")
    static short i32Load16S(int address, int offset, @NotNull Memory self) throws TrapException {
        try {
            return (short) VH_SHORT.get(self.segment, effectiveAddress(address, offset));
        }
        catch (IndexOutOfBoundsException exception){
            throw new TrapException(exception);
        }
    }

    static final String I32_LOAD_16_U_NAME = "i32Load16U";

    @SuppressWarnings("unused")
    static int i32Load16U(int address, int offset, @NotNull Memory self) throws TrapException {
        return Short.toUnsignedInt(i32Load16S(address, offset, self));
    }

    static final String I64_LOAD_8_S_NAME = "i64Load8S";

    @SuppressWarnings("unused")
    static long i64Load8S(int address, int offset, @NotNull Memory self) throws TrapException {
        return i32Load8S(address, offset, self);
    }

    static final String I64_LOAD_8_U_NAME = "i64Load8U";

    @SuppressWarnings("unused")
    static long i64Load8U(int address, int offset, @NotNull Memory self) throws TrapException {
        return Byte.toUnsignedLong(i32Load8S(address, offset, self));
    }

    static final String I64_LOAD_16_S_NAME = "i64Load16S";

    @SuppressWarnings("unused")
    static long i64Load16S(int address, int offset, @NotNull Memory self) throws TrapException {
        return i32Load16S(address, offset, self);
    }

    static final String I64_LOAD_16_U_NAME = "i64Load16U";

    @SuppressWarnings("unused")
    static long i64Load16U(int address, int offset, @NotNull Memory self) throws TrapException {
        return Short.toUnsignedLong(i32Load16S(address, offset, self));
    }

    static final String I64_LOAD_32_S_NAME = "i64Load32S";

    @SuppressWarnings("unused")
    static long i64Load32S(int address, int offset, @NotNull Memory self) throws TrapException {
        return i32Load(address, offset, self);
    }

    static final String I64_LOAD_32_U_NAME = "i64Load32U";

    @SuppressWarnings("unused")
    static long i64Load32U(int address, int offset, @NotNull Memory self) throws TrapException {
        return Integer.toUnsignedLong(i32Load(address, offset, self));
    }

    static final String I32_STORE_NAME = "i32Store";
    static final String I32_STORE_DESCRIPTOR = methodDescriptor(void.class, int.class, int.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void i32Store(int address, int value, int offset, @NotNull Memory self) throws TrapException {
        try {
            VH_INT.set(self.segment, effectiveAddress(address, offset), value);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String I64_STORE_NAME = "i64Store";
    static final String I64_STORE_DESCRIPTOR = methodDescriptor(void.class, int.class, long.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void i64Store(int address, long value, int offset, @NotNull Memory self) throws TrapException {
        try {
            VH_LONG.set(self.segment, effectiveAddress(address, offset), value);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String F32_STORE_NAME = "f32Store";
    static final String F32_STORE_DESCRIPTOR = methodDescriptor(void.class, int.class, float.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void f32Store(int address, float value, int offset, @NotNull Memory self) throws TrapException {
        try {
            VH_FLOAT.set(self.segment, effectiveAddress(address, offset), value);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String F64_STORE_NAME = "f64Store";
    static final String F64_STORE_DESCRIPTOR = methodDescriptor(void.class, int.class, double.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void f64Store(int address, double value, int offset, @NotNull Memory self) throws TrapException {
        try {
            VH_DOUBLE.set(self.segment, effectiveAddress(address, offset), value);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String I32_STORE_8_NAME = "i32Store8";
    static final String I32_STORE_8_DESCRIPTOR = methodDescriptor(void.class, int.class, byte.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void i32Store8(int address, byte value, int offset, @NotNull Memory self) throws TrapException {
        try {
            VH_BYTE.set(self.segment, effectiveAddress(address, offset), value);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String I32_STORE_16_NAME = "i32Store16";
    static final String I32_STORE_16_DESCRIPTOR = methodDescriptor(void.class, int.class, short.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void i32Store16(int address, short value, int offset, @NotNull Memory self) throws TrapException {
        try {
            VH_SHORT.set(self.segment, effectiveAddress(address, offset), value);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String I64_STORE_8_NAME = "i64Store8";
    static final String I64_STORE_8_DESCRIPTOR = methodDescriptor(void.class, int.class, long.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void i64Store8(int address, long value, int offset, @NotNull Memory self) throws TrapException {
        i32Store8(address, (byte) value, offset, self);
    }

    static final String I64_STORE_16_NAME = "i64Store16";
    static final String I64_STORE_16_DESCRIPTOR = methodDescriptor(void.class, int.class, long.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void i64Store16(int address, long value, int offset, @NotNull Memory self) throws TrapException {
        i32Store16(address, (short) value, offset, self);
    }

    static final String I64_STORE_32_NAME = "i64Store32";
    static final String I64_STORE_32_DESCRIPTOR = methodDescriptor(void.class, int.class, long.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void i64Store32(int address, long value, int offset, @NotNull Memory self) throws TrapException {
        i32Store(address, (int) value, offset, self);
    }

    static final String SIZE_METHOD_NAME = "size";
    static final String SIZE_METHOD_DESCRIPTOR = methodDescriptor(int.class, Memory.class);

    @SuppressWarnings("unused")
    static int size(@NotNull Memory self) {
        return (int) (self.segment.byteSize() / PAGE_SIZE);
    }

    static final String GROW_METHOD_NAME = "grow";
    static final String GROW_METHOD_DESCRIPTOR = methodDescriptor(int.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static int grow(int additionalPages, @NotNull Memory self) {
        var segment = self.segment;
        var currentPageCount = segment.byteSize() / PAGE_SIZE;

        if (additionalPages == 0) {
            return (int) currentPageCount;
        }

        var newPageCount = currentPageCount + Integer.toUnsignedLong(additionalPages);

        if (newPageCount > Integer.toUnsignedLong(self.maxPageCount)) {
            return -1;
        }

        MemorySegment newSegment;
        try {
            newSegment = MemorySegment.allocateNative(newPageCount * PAGE_SIZE, 8, newImplicitScope());
        } catch (OutOfMemoryError ignored) {
            return -1;
        }

        newSegment.copyFrom(segment);
        self.segment = newSegment;

        return (int) currentPageCount;
    }

    static final String INIT_METHOD_NAME = "init";
    static final String INIT_METHOD_DESCRIPTOR = methodDescriptor(void.class, int.class, int.class, int.class, MemorySegment.class, Memory.class);

    @SuppressWarnings("unused")
    static void init(int dstAddress, int srcAddress, int size, @NotNull MemorySegment src, @NotNull Memory self) throws TrapException {
        var longSize = Integer.toUnsignedLong(size);
        try {
            var dstSlice = self.segment.asSlice(Integer.toUnsignedLong(dstAddress), longSize);
            var srcSlice = src.asSlice(Integer.toUnsignedLong(srcAddress), longSize);
            dstSlice.copyFrom(srcSlice);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String INIT_FROM_ACTIVE_NAME = "initFromActive";
    static final String INIT_FROM_ACTIVE_DESCRIPTOR = methodDescriptor(void.class, MemorySegment.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void initFromActive(@NotNull MemorySegment data, int dstAddress, @NotNull Memory self) throws TrapException {
        try {
            self.segment.asSlice(Integer.toUnsignedLong(dstAddress)).copyFrom(data);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String FILL_METHOD_NAME = "fill";
    static final String FILL_METHOD_DESCRIPTOR = methodDescriptor(void.class, int.class, byte.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void fill(int dstAddress, byte fillValue, int size, @NotNull Memory self) throws TrapException {
        try {
            self.segment.asSlice(Integer.toUnsignedLong(dstAddress), Integer.toUnsignedLong(size)).fill(fillValue);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String COPY_METHOD_NAME = "copy";
    static final String COPY_METHOD_DESCRIPTOR = methodDescriptor(void.class, int.class, int.class, int.class, Memory.class);

    @SuppressWarnings("unused")
    static void copy(int dstAddress, int srcAddress, int size, @NotNull Memory dst, @NotNull Memory src) throws TrapException {
        try {
            var dstSegment = dst.segment.asSlice(Integer.toUnsignedLong(dstAddress));
            var srcSegment = src.segment.asSlice(Integer.toUnsignedLong(srcAddress), Integer.toUnsignedLong(size));
            dstSegment.copyFrom(srcSegment);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }
}
