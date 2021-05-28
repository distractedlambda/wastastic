package org.wastastic;

public interface Memory {
    int PAGE_SIZE = 65536;

    byte loadByte(int address) throws TrapException;

    short loadShort(int address) throws TrapException;

    int loadInt(int address) throws TrapException;

    long loadLong(int address) throws TrapException;

    float loadFloat(int address) throws TrapException;

    double loadDouble(int address) throws TrapException;

    void storeByte(int address, byte value) throws TrapException;

    void storeShort(int address, short value) throws TrapException;

    void storeInt(int address, int value) throws TrapException;

    void storeLong(int address, long value) throws TrapException;

    void storeFloat(int address, float value) throws TrapException;

    void storeDouble(int address, double value) throws TrapException;

    int getSize();

    void fill(int destinationAddress, int size, byte fillValue) throws TrapException;

    void copy(int destinationAddress, int sourceAddress, int size) throws TrapException;

    void copy(int destinationAddress, byte[] source, int sourceOffset, int size) throws TrapException;

    void copy(byte[] destination, int destinationOffset, int sourceAddress, int size) throws TrapException;

    int grow(int additionalPages);
}
