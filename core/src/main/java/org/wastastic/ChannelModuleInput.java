package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;

import static java.lang.Thread.onSpinWait;

final class ChannelModuleInput implements ModuleInput {
    private final @NotNull ReadableByteChannel channel;
    private final @NotNull ByteBuffer buffer;

    ChannelModuleInput(@NotNull ReadableByteChannel channel, int bufferSize) {
        this.channel = channel;
        this.buffer = ByteBuffer.allocateDirect(bufferSize).limit(0).order(ByteOrder.LITTLE_ENDIAN);
    }

    private void fetchInput() throws IOException {
        buffer.clear();

        while (true) {
            var bytesRead = channel.read(buffer);

            if (bytesRead == -1) {
                throw new EOFException();
            }

            if (bytesRead != 0) {
                break;
            }

            onSpinWait();
        }

        buffer.flip();
    }

    @Override public byte nextByte() throws IOException {
        if (!buffer.hasRemaining()) {
            fetchInput();
        }

        return buffer.get();
    }

    @Override public byte peekByte() throws IOException {
        if (!buffer.hasRemaining()) {
            fetchInput();
        }

        return buffer.get(buffer.position());
    }

    @Override public void skip(long count) throws IOException {
        if (count <= buffer.remaining()) {
            buffer.position(buffer.position() + (int) count);
        }
        else {
            count -= buffer.remaining();
            buffer.position(buffer.limit());

            if (channel instanceof SeekableByteChannel seekableChannel) {
                seekableChannel.position(seekableChannel.position() + count);
            }
            else {
                while (true) {
                    fetchInput();

                    if (count <= buffer.remaining()) {
                        buffer.position((int) count);
                        return;
                    }

                    count -= buffer.position();
                }
            }
        }
    }

    @Override public float nextFloat32() throws IOException {
        if (buffer.remaining() >= 4) {
            return buffer.getFloat();
        }
        else {
            return ModuleInput.super.nextFloat32();
        }
    }

    @Override public double nextFloat64() throws IOException {
        if (buffer.remaining() >= 8) {
            return buffer.getDouble();
        }
        else {
            return ModuleInput.super.nextFloat64();
        }
    }

    @Override public @NotNull String nextUtf8(int length) throws IOException {
        var bytes = new byte[length];
        var offset = 0;

        while (offset != length) {
            if (!buffer.hasRemaining()) {
                fetchInput();
            }

            var copySize = Integer.min(length - offset, buffer.remaining());
            buffer.get(bytes, offset, copySize);
            offset += copySize;
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override public @NotNull MemorySegment nextBytes(long length, @NotNull ResourceScope scope) throws IOException {
        var segment = MemorySegment.allocateNative(length, 8, scope);

        var initialCopySize = Long.min(segment.byteSize(), buffer.remaining());
        segment.copyFrom(MemorySegment.ofByteBuffer(buffer).asSlice(0, initialCopySize));
        buffer.position(buffer.position() + (int) initialCopySize);

        if (initialCopySize == length) {
            return segment;
        }

        if (channel instanceof ScatteringByteChannel scatteringChannel) {
            // FIXME: is Integer.MAX_VALUE allowable?
            var buffersForSegment = (segment.byteSize() - initialCopySize + Integer.MAX_VALUE - 1) / Integer.MAX_VALUE;
            var buffers = new ByteBuffer[(int) buffersForSegment + 1];

            for (var i = 0; i < buffersForSegment; i++) {
                var offset = initialCopySize + (long) i * Integer.MAX_VALUE;
                buffers[i] = segment
                    .asSlice(offset, Long.min(segment.byteSize() - offset, Integer.MAX_VALUE))
                    .asByteBuffer();
            }

            buffer.clear();
            buffers[(int) buffersForSegment] = buffer;

            while (buffers[(int) buffersForSegment].hasRemaining()) {
                scatteringChannel.read(buffers);
            }

            buffer.flip();
        }
        else {
            for (var offset = initialCopySize; offset < segment.byteSize(); offset += Integer.MAX_VALUE) {
                var segmentAsBuffer = segment
                    .asSlice(offset, Long.min(segment.byteSize() - offset, Integer.MAX_VALUE))
                    .asByteBuffer();

                while (segmentAsBuffer.hasRemaining()) {
                    channel.read(segmentAsBuffer);
                }
            }
        }

        return segment;
    }
}
