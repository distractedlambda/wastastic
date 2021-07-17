package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public record Filestat(
    long deviceId,
    long inode,
    @NotNull Filetype filetype,
    long linkCount,
    long size,
    long accessTimeNanos,
    long modificationTimeNanos,
    long statusChangeTimeNanos
) {
    public Filestat {
        requireNonNull(filetype);
    }
}
