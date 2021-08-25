package org.wastastic.wasi;

import static org.wastastic.wasi.WasiConstants.FDFLAGS_APPEND;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_DSYNC;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_NONBLOCK;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_RSYNC;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_SYNC;
import static org.wastastic.wasi.WasiConstants.ILLEGAL_FDFLAGS;

public final class FdFlags {
    private final short bits;

    FdFlags(short bits) throws ErrnoException {
        if ((bits & ILLEGAL_FDFLAGS) != 0) {
            throw new ErrnoException(Errno.INVAL);
        }

        this.bits = bits;
    }

    public FdFlags(boolean append, boolean dsync, boolean nonblock, boolean rsync, boolean sync) {
        short bits = 0;

        if (append) {
            bits |= FDFLAGS_APPEND;
        }

        if (dsync) {
            bits |= FDFLAGS_DSYNC;
        }

        if (nonblock) {
            bits |= FDFLAGS_NONBLOCK;
        }

        if (rsync) {
            bits |= FDFLAGS_RSYNC;
        }

        if (sync) {
            bits |= FDFLAGS_SYNC;
        }

        this.bits = bits;
    }

    short bits() {
        return bits;
    }

    public boolean hasAppend() {
        return (bits & FDFLAGS_APPEND) != 0;
    }

    public boolean hasDsync() {
        return (bits & FDFLAGS_DSYNC) != 0;
    }

    public boolean hasNonblock() {
        return (bits & FDFLAGS_NONBLOCK) != 0;
    }

    public boolean hasRsync() {
        return (bits & FDFLAGS_RSYNC) != 0;
    }

    public boolean hasSync() {
        return (bits & FDFLAGS_SYNC) != 0;
    }
}
