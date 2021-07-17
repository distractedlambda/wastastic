package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public record DirectoryEntry(long nextCookie, long inode, @NotNull Filetype filetype, byte @NotNull[] name) {
    public DirectoryEntry {
        requireNonNull(filetype);
        requireNonNull(name);
    }
}
