package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static java.lang.System.arraycopy;
import static java.util.Objects.checkFromIndexSize;
import static org.objectweb.asm.Type.getDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.Names.methodDescriptor;

public final class Table {
    final int maxSize;
    @Nullable Object @NotNull[] storage;

    static final String INTERNAL_NAME = getInternalName(Table.class);
    static final String DESCRIPTOR = getDescriptor(Table.class);

    public Table(int initialSize, int maxSize) {
        if (Integer.compareUnsigned(initialSize, maxSize) > 0) {
            throw new IllegalArgumentException();
        }

        this.maxSize = maxSize;
        this.storage = new Object[initialSize];
    }

    public Table(int initialSize) {
        this(initialSize, -1);
    }

    static final String GET_NAME = "get";
    static final String GET_DESCRIPTOR = methodDescriptor(Object.class, int.class, Table.class);

    @SuppressWarnings("unused")
    static @Nullable Object get(int index, @NotNull Table self) throws TrapException {
        try {
            return self.storage[index];
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String SET_NAME = "set";
    static final String SET_DESCRIPTOR = methodDescriptor(void.class, int.class, Object.class, Table.class);

    @SuppressWarnings("unused")
    static void set(int index, @Nullable Object value, @NotNull Table self) throws TrapException {
        try {
            self.storage[index] = value;
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String SIZE_METHOD_NAME = "size";
    static final String SIZE_METHOD_DESCRIPTOR = methodDescriptor(int.class, Table.class);

    @SuppressWarnings("unused")
    static int size(@NotNull Table self) {
        return self.storage.length;
    }

    static final String GROW_NAME = "grow";
    static final String GROW_DESCRIPTOR = methodDescriptor(int.class, Object.class, int.class, Table.class);

    @SuppressWarnings("unused")
    static int grow(@Nullable Object initialValue, int additionalEntries, @NotNull Table self) {
        var storage = self.storage;

        if (additionalEntries == 0) {
            return storage.length;
        }

        var newSize = Integer.toUnsignedLong(storage.length) + Integer.toUnsignedLong(additionalEntries);

        if (newSize > Integer.toUnsignedLong(self.maxSize)) {
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
    static final String FILL_DESCRIPTOR = methodDescriptor(void.class, int.class, Object.class, int.class, Table.class);

    @SuppressWarnings("unused")
    static void fill(int startIndex, @Nullable Object fillValue, int count, @NotNull Table self) throws TrapException {
        var storage = self.storage;
        try {
            checkFromIndexSize(startIndex, count, storage.length);
            Arrays.fill(storage, startIndex, startIndex + count, fillValue);
        }
        catch (IndexOutOfBoundsException | IllegalArgumentException exception) {
            throw new TrapException(exception);
        }
    }

    static final String COPY_NAME = "copy";
    static final String COPY_DESCRIPTOR = methodDescriptor(void.class, int.class, int.class, int.class, Table.class, Table.class);

    @SuppressWarnings("unused")
    static void copy(int dstIndex, int srcIndex, int count, @NotNull Table src, @NotNull Table dst) throws TrapException {
        try {
            arraycopy(src.storage, srcIndex, dst.storage, dstIndex, count);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String INIT_NAME = "init";
    static final String INIT_DESCRIPTOR = methodDescriptor(void.class, int.class, int.class, int.class, Object[].class, Table.class);

    @SuppressWarnings("unused")
    static void init(int dstIndex, int srcIndex, int count, @Nullable Object @NotNull[] src, @NotNull Table self) throws TrapException {
        try {
            arraycopy(src, srcIndex, self.storage, dstIndex, count);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }

    static final String INIT_FROM_ACTIVE_NAME = "initFromActive";
    static final String INIT_FROM_ACTIVE_DESCRIPTOR = methodDescriptor(void.class, Object[].class, int.class, Table.class);

    @SuppressWarnings("unused")
    static void initFromActive(@Nullable Object @NotNull[] element, int offset, @NotNull Table self) throws TrapException {
        try {
            arraycopy(element, 0, self.storage, offset, element.length);
        }
        catch (IndexOutOfBoundsException exception) {
            throw new TrapException(exception);
        }
    }
}
