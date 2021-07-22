package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

import static java.util.Objects.requireNonNull;

public final class TimeOverride {
    private final long nanos;
    private final @NotNull Kind kind;

    private static final TimeOverride NOW = new TimeOverride(0, Kind.USE_NOW);
    private static final TimeOverride DISABLED = new TimeOverride(0, Kind.DONT_OVERRIDE);

    private TimeOverride(long nanos, @NotNull Kind kind) {
        this.nanos = nanos;
        this.kind = requireNonNull(kind);
    }

    public static @NotNull TimeOverride now() {
        return TimeOverride.NOW;
    }

    public static @NotNull TimeOverride disabled() {
        return TimeOverride.DISABLED;
    }

    public static @NotNull TimeOverride nanos(long nanos) {
        return new TimeOverride(nanos, Kind.USE_NANOS);
    }

    public boolean isDisabled() {
        return kind == Kind.DONT_OVERRIDE;
    }

    public boolean isNow() {
        return kind == Kind.USE_NOW;
    }

    public @NotNull OptionalLong nanos() {
        if (kind == Kind.USE_NANOS) {
            return OptionalLong.of(nanos);
        }
        else {
            return OptionalLong.empty();
        }
    }

    private enum Kind {
        USE_NANOS,
        USE_NOW,
        DONT_OVERRIDE,
    }
}
