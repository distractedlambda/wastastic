package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public record Fdstat(@NotNull Filetype filetype, boolean append, boolean dsync, boolean nonblock, boolean rsync, boolean sync) {
    public Fdstat {
        requireNonNull(filetype);
    }
}
