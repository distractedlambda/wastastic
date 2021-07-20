package org.wastastic.wasi;

import static org.wastastic.wasi.WasiConstants.ILLEGAL_RIGHTS;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_ADVISE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_ALLOCATE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_DATASYNC;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_FDSTAT_SET_FLAGS;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_READ;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_SEEK;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_TELL;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_WRITE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_CREATE_DIRECTORY;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_CREATE_FILE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_LINK_SOURCE;

public final class Rights {
    private final long bits;

    Rights(long bits) throws ErrnoException {
        if ((bits & ILLEGAL_RIGHTS) != 0) {
            throw new ErrnoException(Errno.INVAL);
        }

        this.bits = bits;
    }

    long bits() {
        return bits;
    }

    public boolean canDatasync() {
        return (bits & RIGHTS_FD_DATASYNC) != 0;
    }

    public boolean canRead() {
        return (bits & RIGHTS_FD_READ) != 0;
    }

    public boolean canSeek() {
        return (bits & RIGHTS_FD_SEEK) != 0;
    }

    public boolean canSetFlags() {
        return (bits & RIGHTS_FD_FDSTAT_SET_FLAGS) != 0;
    }

    public boolean canSync() {
        return (bits & RIGHTS_FD_DATASYNC) != 0;
    }

    public boolean canTell() {
        return (bits & RIGHTS_FD_TELL) != 0;
    }

    public boolean canWrite() {
        return (bits & RIGHTS_FD_WRITE) != 0;
    }

    public boolean canAdvise() {
        return (bits & RIGHTS_FD_ADVISE) != 0;
    }

    public boolean canAllocate() {
        return (bits & RIGHTS_FD_ALLOCATE) != 0;
    }

    public boolean canCreateDirectory() {
        return (bits & RIGHTS_PATH_CREATE_DIRECTORY) != 0;
    }

    public boolean canCreateFile() {
        return (bits & RIGHTS_PATH_CREATE_FILE) != 0;
    }

    public boolean canBeLinkSource() {
        return (bits & RIGHTS_PATH_LINK_SOURCE) != 0;
    }
}
