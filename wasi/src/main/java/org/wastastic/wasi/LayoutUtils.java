package org.wastastic.wasi;

import jdk.incubator.foreign.MemoryLayout;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static jdk.incubator.foreign.MemoryLayout.paddingLayout;
import static jdk.incubator.foreign.MemoryLayout.structLayout;

final class LayoutUtils {
    private LayoutUtils() {}

    static @NotNull MemoryLayout cStructLayout(@NotNull MemoryLayout @NotNull... memberLayouts) {
        var byteOffset = 0L;
        var byteAlignment = 1L;
        var actualMemberLayouts = new ArrayList<MemoryLayout>();

        for (var layout : memberLayouts) {
            var alignedOffset = Math.addExact(byteOffset, layout.byteAlignment() - 1) & (layout.byteAlignment() - 1);

            if (byteOffset != alignedOffset) {
                actualMemberLayouts.add(paddingLayout((alignedOffset - byteOffset) * 8));
                byteOffset = alignedOffset;
            }

            if (layout.byteAlignment() > byteAlignment) {
                byteAlignment = layout.byteAlignment();
            }

            actualMemberLayouts.add(layout);
            byteOffset += layout.byteSize();
        }

        var alignedSize = Math.addExact(byteOffset, byteAlignment - 1) & (byteAlignment - 1);

        if (byteOffset != alignedSize) {
            actualMemberLayouts.add(paddingLayout((alignedSize - byteOffset) * 8));
        }

        return structLayout(actualMemberLayouts.toArray(MemoryLayout[]::new)).withBitAlignment(byteAlignment * 8);
    }
}
