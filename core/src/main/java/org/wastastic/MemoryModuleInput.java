package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;

final class MemoryModuleInput implements ModuleInput {
    private final @NotNull MemorySegment segment;
    private long offset = 0;

    MemoryModuleInput(@NotNull MemorySegment segment) {
        this.segment = segment;
    }

    @Override public byte nextByte() throws IOException {
        byte value;

        try {
            value = (byte) Memory.VH_BYTE.get(segment, offset);
        }
        catch (IndexOutOfBoundsException ignored) {
            throw new EOFException();
        }

        offset++;
        return value;
    }

    @Override public byte peekByte() throws IOException {
        try {
            return (byte) Memory.VH_BYTE.get(segment, offset);
        }
        catch (IndexOutOfBoundsException ignored) {
            throw new EOFException();
        }
    }

    @Override public void skip(long count) throws IOException {
        if (count > segment.byteSize() - offset) {
            throw new EOFException();
        }
    }

    @Override public @NotNull ModuleInput defer(long count) throws IOException {
        MemoryModuleInput deferred;

        try {
            deferred = new MemoryModuleInput(segment.asSlice(offset, count));
        }
        catch (IndexOutOfBoundsException ignored) {
            throw new EOFException();
        }

        offset += count;
        return deferred;
    }

    @Override public float nextFloat32() throws IOException {
        float value;

        try {
            value = (float) Memory.VH_FLOAT.get(segment, offset);
        }
        catch (IndexOutOfBoundsException ignored) {
            throw new EOFException();
        }

        offset += 4;
        return value;
    }

    @Override public double nextFloat64() throws IOException {
        double value;

        try {
            value = (double) Memory.VH_DOUBLE.get(segment, offset);
        }
        catch (IndexOutOfBoundsException ignored) {
            throw new EOFException();
        }

        offset += 8;
        return value;
    }

    @Override public @NotNull String nextUtf8(int length) throws IOException {
        var bytes = new byte[length];

        try {
            MemorySegment.ofArray(bytes).copyFrom(segment.asSlice(offset, length));
        }
        catch (IndexOutOfBoundsException ignored) {
            throw new EOFException();
        }

        offset += length;
        return new String(bytes);
    }

    @Override public @NotNull MemorySegment nextBytes(long length, @NotNull ResourceScope scope) throws IOException {
        // FIXME: what should alignment on this be?
        var newSegment = MemorySegment.allocateNative(length, 8, scope);

        try {
            newSegment.copyFrom(segment.asSlice(offset, length));
        }
        catch (IndexOutOfBoundsException ignored) {
            throw new EOFException();
        }

        offset += length;
        return newSegment;
    }
}
