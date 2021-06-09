package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static java.lang.System.arraycopy;
import static java.util.Objects.checkFromIndexSize;

public final class Table {
    final int maxSize;
    @Nullable Object @NotNull[] storage;

    static final String INTERNAL_NAME = "org/wastastic/Table";
    static final String DESCRIPTOR = "Lorg/wastastic/Table;";

    public Table(int initialSize, int maxSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException();
        }

        if (maxSize < initialSize) {
            throw new IllegalArgumentException();
        }

        this.maxSize = maxSize;
        this.storage = new Object[initialSize];
    }

    public Table(int initialSize) {
        this(initialSize, Integer.MAX_VALUE);
    }

    public @Nullable Object get(int index) {
        return storage[index];
    }

    public void set(int index, @Nullable Object value) {
        storage[index] = value;
    }

    public int size() {
        return storage.length;
    }

    public int grow(@Nullable Object initialValue, int additionalEntries) {
        var storage = this.storage;

        if (additionalEntries == 0) {
            return storage.length;
        }

        var newSize = Integer.toUnsignedLong(storage.length) + Integer.toUnsignedLong(additionalEntries);

        if (newSize > maxSize) {
            return -1;
        }

        Object[] newStorage;
        try {
            newStorage = Arrays.copyOf(storage, (int) newSize);
        } catch (OutOfMemoryError ignored) {
            return -1;
        }

        Arrays.fill(newStorage, storage.length, newStorage.length, initialValue);

        this.storage = newStorage;
        return storage.length;
    }

    public void fill(int startIndex, @Nullable Object fillValue, int count) {
        var storage = this.storage;
        checkFromIndexSize(startIndex, count, storage.length);
        Arrays.fill(storage, startIndex, startIndex + count, fillValue);
    }

    public void copy(int dstIndex, int srcIndex, int count, @NotNull Table src) {
        arraycopy(src.storage, srcIndex, storage, dstIndex, count);
    }

    public void init(int dstIndex, int srcIndex, int count, @Nullable Object @NotNull[] src, @NotNull Table self) {
        try {
            arraycopy(src, srcIndex, self.storage, dstIndex, count);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }
}
