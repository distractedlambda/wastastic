package org.wastastic.wasi;

import static org.wastastic.wasi.WasiConstants.ILLEGAL_LOOKUPFLAGS;
import static org.wastastic.wasi.WasiConstants.LOOKUPFLAGS_SYMLINK_FOLLOW;

public final class LookupFlags {
    private final int bits;

    LookupFlags(int bits) throws ErrnoException {
        if ((bits & ILLEGAL_LOOKUPFLAGS) != 0) {
            throw new ErrnoException(Errno.INVAL);
        }

        this.bits = bits;
    }

    int bits() {
        return bits;
    }

    public boolean hasSymlinkFollow() {
        return (bits & LOOKUPFLAGS_SYMLINK_FOLLOW) != 0;
    }
}
