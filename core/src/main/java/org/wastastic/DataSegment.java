package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

record DataSegment(@NotNull MemorySegment contents, @NotNull Mode mode, int memoryIndex, int memoryOffset) {
    DataSegment {
        requireNonNull(contents);
        requireNonNull(mode);
    }

    enum Mode {
        PASSIVE,
        ACTIVE,
    }
}
