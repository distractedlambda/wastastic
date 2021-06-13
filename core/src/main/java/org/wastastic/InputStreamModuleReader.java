package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.lang.Integer.toUnsignedLong;
import static java.util.Objects.requireNonNull;

public final class InputStreamModuleReader implements ModuleReader {
    private final @NotNull InputStream stream;

    public InputStreamModuleReader(@NotNull InputStream stream) {
        this.stream = requireNonNull(stream);
    }

    public @NotNull InputStream getStream() {
        return stream;
    }

    @Override public byte nextByte() throws IOException {
        var value = stream.read();

        if (value == -1) {
            throw new EOFException();
        }

        return (byte) value;
    }

    @Override public void skip(int unsignedCount) throws IOException {
        stream.skipNBytes(toUnsignedLong(unsignedCount));
    }

    @Override public @NotNull String nextUtf8(int length) throws IOException {
        var bytes = stream.readNBytes(length);

        if (bytes.length != length) {
            throw new EOFException();
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override public @NotNull MemorySegment nextBytes(long length, @NotNull ResourceScope scope) throws IOException {
        if (length > Integer.MAX_VALUE) {
            throw new UnsupportedOperationException();
        }

        var segment = MemorySegment.allocateNative(length, 8, scope);
        segment.copyFrom(MemorySegment.ofArray(stream.readNBytes((int) length)));
        return segment;
    }
}
