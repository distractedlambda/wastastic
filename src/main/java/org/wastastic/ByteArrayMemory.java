package org.wastastic;

import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

import static java.lang.Integer.toUnsignedLong;
import static java.lang.System.arraycopy;
import static java.lang.invoke.MethodHandles.byteArrayViewVarHandle;
import static java.util.Arrays.copyOf;

public final class ByteArrayMemory implements Memory {
    private static final VarHandle VH_SHORT = byteArrayViewVarHandle(short.class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_INT = byteArrayViewVarHandle(int.class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_LONG = byteArrayViewVarHandle(long.class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_FLOAT = byteArrayViewVarHandle(float.class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle VH_DOUBLE = byteArrayViewVarHandle(double.class, ByteOrder.LITTLE_ENDIAN);

    private byte[] array;

    public ByteArrayMemory(int initialPageCount) {
        array = new byte[initialPageCount * PAGE_SIZE];
    }

    @Override
    public byte loadByte(int address) throws TrapException {
        try {
            return array[address];
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public short loadShort(int address) throws TrapException {
        try {
            return (short) VH_SHORT.get(array, address);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public int loadInt(int address) throws TrapException {
        try {
            return (int) VH_INT.get(array, address);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public long loadLong(int address) throws TrapException {
        try {
            return (long) VH_LONG.get(array, address);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public float loadFloat(int address) throws TrapException {
        try {
            return (float) VH_FLOAT.get(array, address);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public double loadDouble(int address) throws TrapException {
        try {
            return (double) VH_DOUBLE.get(array, address);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void storeByte(int address, byte value) throws TrapException {
        try {
            array[address] = value;
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void storeShort(int address, short value) throws TrapException {
        try {
            VH_SHORT.set(array, address, value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void storeInt(int address, int value) throws TrapException {
        try {
            VH_INT.set(array, address, value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void storeLong(int address, long value) throws TrapException {
        try {
            VH_LONG.set(array, address, value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void storeFloat(int address, float value) throws TrapException {
        try {
            VH_FLOAT.set(array, address, value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void storeDouble(int address, double value) throws TrapException {
        try {
            VH_DOUBLE.set(array, address, value);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public int getSize() {
        return array.length / PAGE_SIZE;
    }

    @Override
    public void fill(int destinationAddress, int size, byte fillValue) throws TrapException {
        try {
            Arrays.fill(array, destinationAddress, destinationAddress + size, fillValue);
        } catch (IndexOutOfBoundsException | IllegalArgumentException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void copy(int destinationAddress, int sourceAddress, int size) throws TrapException {
        try {
            arraycopy(array, sourceAddress, array, destinationAddress, size);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void copy(int destinationAddress, byte[] source, int sourceOffset, int size) throws TrapException {
        try {
            arraycopy(source, sourceOffset, array, destinationAddress, size);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public void copy(byte[] destination, int destinationOffset, int sourceAddress, int size) throws TrapException {
        try {
            arraycopy(array, sourceAddress, destination, destinationOffset, size);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    @Override
    public int grow(int additionalPages) {
        var oldPageCount = getSize();
        var newPageCount = toUnsignedLong(oldPageCount) + toUnsignedLong(additionalPages);
        var newArrayLength = newPageCount * PAGE_SIZE;

        if (newArrayLength > Integer.MAX_VALUE) {
            return -1;
        }

        byte[] newArray;
        try {
            newArray = copyOf(array, (int) newArrayLength);
        } catch (OutOfMemoryError ignored) {
            return -1;
        }

        array = newArray;
        return oldPageCount;
    }
}
