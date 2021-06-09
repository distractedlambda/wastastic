package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static java.lang.System.arraycopy;
import static java.util.Objects.checkFromIndexSize;

public final class Table {
    private final int maxSize;
    private @Nullable Object @NotNull[] storage;

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

    static final String INTERNAL_NAME = "org/wastastic/Table";
    static final String DESCRIPTOR = "Lorg/wastastic/Table;";

    static final String GET_NAME = "get";
    static final String GET_DESCRIPTOR = "(ILorg/wastastic/Table;)Ljava/lang/Object;";

    static @Nullable Object get(int index, @NotNull Table self) {
        try {
            return self.storage[index];
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    static final String SET_NAME = "set";
    static final String SET_DESCRIPTOR = "(ILjava/lang/Object;Lorg/wastastic/Table;)V";

    static void set(int index, @Nullable Object value, @NotNull Table self) {
        try {
            self.storage[index] = value;
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    static final String SIZE_NAME = "size";
    static final String SIZE_DESCRIPTOR = "(Lorg/wastastic/Table;)I";

    static int size(@NotNull Table self) {
        return self.storage.length;
    }

    static final String GROW_NAME = "grow";
    static final String GROW_DESCRIPTOR = "(Ljava/lang/Object;ILorg/wastastic/Table;)I";

    static int grow(@Nullable Object initialValue, int additionalEntries, @NotNull Table self) {
        var storage = self.storage;

        if (additionalEntries == 0) {
            return storage.length;
        }

        var newSize = Integer.toUnsignedLong(storage.length) + Integer.toUnsignedLong(additionalEntries);

        if (Long.compareUnsigned(newSize, Integer.toUnsignedLong(self.maxSize)) > 0) {
            return -1;
        }

        Object[] newStorage;
        try {
            newStorage = Arrays.copyOf(storage, (int) newSize);
        } catch (OutOfMemoryError ignored) {
            return -1;
        }

        Arrays.fill(newStorage, storage.length, newStorage.length, initialValue);

        self.storage = newStorage;
        return storage.length;
    }

    static final String FILL_NAME = "fill";
    static final String FILL_DESCRIPTOR = "(ILjava/lang/Object;ILorg/wastastic/Table;)V";

    static void fill(int startIndex, @Nullable Object fillValue, int count, @NotNull Table self) {
        var storage = self.storage;

        try {
            checkFromIndexSize(startIndex, count, storage.length);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }

        Arrays.fill(storage, startIndex, startIndex + count, fillValue);
    }

    static final String COPY_NAME = "copy";
    static final String COPY_DESCRIPTOR = "(IIILorg/wastastic/Table;Lorg/wastastic/Table;)V";

    static void copy(int dstIndex, int srcIndex, int count, @NotNull Table dst, @NotNull Table src) {
        try {
            arraycopy(src.storage, srcIndex, dst.storage, dstIndex, count);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }

    static final String INIT_NAME = "init";
    static final String INIT_DESCRIPTOR = "(III[Ljava/lang/Object;Lorg/wastastic/Table;)V";

    static void init(int dstIndex, int srcIndex, int count, @Nullable Object @NotNull[] src, @NotNull Table self) {
        try {
            arraycopy(src, srcIndex, self.storage, dstIndex, count);
        } catch (IndexOutOfBoundsException ignored) {
            throw new TrapException();
        }
    }
}
