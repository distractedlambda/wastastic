package org.wastastic.wasi;

import static org.wastastic.wasi.WasiConstants.ILLEGAL_OFLAGS;
import static org.wastastic.wasi.WasiConstants.OFLAGS_CREAT;
import static org.wastastic.wasi.WasiConstants.OFLAGS_DIRECTORY;
import static org.wastastic.wasi.WasiConstants.OFLAGS_EXCL;
import static org.wastastic.wasi.WasiConstants.OFLAGS_TRUNC;

public final class OFlags {
    private final short bits;

    OFlags(short bits) throws ErrnoException {
        if ((bits & ILLEGAL_OFLAGS) != 0) {
            throw new ErrnoException(Errno.INVAL);
        }

        this.bits = bits;
    }

    short bits() {
        return bits;
    }

    public boolean hasCreate() {
        return (bits & OFLAGS_CREAT) != 0;
    }

    public boolean hasDirectory() {
        return (bits & OFLAGS_DIRECTORY) != 0;
    }

    public boolean hasExclusive() {
        return (bits & OFLAGS_EXCL) != 0;
    }

    public boolean hasTruncate() {
        return (bits & OFLAGS_TRUNC) != 0;
    }
}
