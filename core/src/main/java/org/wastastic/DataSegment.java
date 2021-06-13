package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;

record DataSegment(@NotNull MemorySegment contents, @NotNull Mode mode, int memoryIndex, int memoryOffset) {
    enum Mode {
        PASSIVE,
        ACTIVE,
    }
}
