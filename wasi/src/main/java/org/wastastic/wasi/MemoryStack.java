package org.wastastic.wasi;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import org.jetbrains.annotations.NotNull;

final class MemoryStack {
    private static final ThreadLocal<MemoryStack> THREAD_LOCAL = ThreadLocal.withInitial(MemoryStack::new);

    private static final long SIZE = 1024 * 1024;
    private static final long ALIGNMENT = 8;

    private final @NotNull MemorySegment segment;
    private long pointer;

    private MemoryStack() {
        segment = MemorySegment.allocateNative(SIZE, ALIGNMENT, ResourceScope.newImplicitScope());
        pointer = segment.byteSize();
    }

    static @NotNull MemoryStack get() {
        return THREAD_LOCAL.get();
    }

    static @NotNull Frame getFrame() {
        return THREAD_LOCAL.get().frame();
    }

    @NotNull Frame frame() {
        return new Frame();
    }

    final class Frame implements SegmentAllocator, AutoCloseable {
        private final long framePointer;

        private Frame() {
            framePointer = pointer;
        }

        @NotNull MemoryStack stack() {
            return MemoryStack.this;
        }

        @Override public @NotNull MemorySegment allocate(long bytesSize, long bytesAlignment) {
            if (bytesSize < 0) {
                throw new IllegalArgumentException("Invalid size for allocation: " + bytesSize);
            }

            if (Long.bitCount(bytesAlignment) != 1) {
                throw new IllegalArgumentException("Invalid alignment for allocation: " + bytesAlignment);
            }

            if (bytesAlignment > ALIGNMENT) {
                throw new UnsupportedOperationException("Requested alignment (" + bytesAlignment + ") exceeds maximum alignment (" + ALIGNMENT + ")");
            }

            pointer = (pointer & -bytesAlignment) - bytesSize;
            return segment.asSlice(pointer, bytesSize);
        }

        @Override public void close() {
            pointer = framePointer;
        }
    }
}
