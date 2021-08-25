package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public record Fdstat(@NotNull Filetype filetype, @NotNull FdFlags flags) {
    public Fdstat {
        requireNonNull(filetype);
        requireNonNull(flags);
    }
}
