package org.wastastic;

import org.wastastic.compiler.ModuleReader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.lang.Integer.toUnsignedLong;
import static java.util.Objects.requireNonNull;

public final class InputStreamModuleReader implements ModuleReader {
    private final InputStream stream;

    public InputStreamModuleReader(InputStream stream) {
        this.stream = requireNonNull(stream);
    }

    public InputStream getStream() {
        return stream;
    }

    @Override
    public byte nextByte() throws IOException {
        var value = stream.read();

        if (value == -1) {
            throw new EOFException();
        }

        return (byte) value;
    }

    @Override
    public void skip(int unsignedCount) throws IOException {
        stream.skipNBytes(toUnsignedLong(unsignedCount));
    }

    @Override
    public String nextUtf8(int length) throws IOException {
        var bytes = stream.readNBytes(length);

        if (bytes.length != length) {
            throw new EOFException();
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }
}
