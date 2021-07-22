package org.wastastic.wasi;

import static org.wastastic.wasi.WasiConstants.ILLEGAL_RIGHTS;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_ADVISE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_ALLOCATE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_DATASYNC;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_FDSTAT_SET_FLAGS;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_FILESTAT_GET;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_FILESTAT_SET_SIZE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_FILESTAT_SET_TIMES;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_READ;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_READDIR;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_SEEK;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_SYNC;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_TELL;
import static org.wastastic.wasi.WasiConstants.RIGHTS_FD_WRITE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_CREATE_DIRECTORY;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_CREATE_FILE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_FILESTAT_GET;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_FILESTAT_SET_SIZE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_FILESTAT_SET_TIMES;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_LINK_SOURCE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_LINK_TARGET;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_OPEN;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_READLINK;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_REMOVE_DIRECTORY;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_RENAME_SOURCE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_RENAME_TARGET;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_SYMLINK;
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_UNLINK_FILE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_POLL_FD_READWRITE;
import static org.wastastic.wasi.WasiConstants.RIGHTS_SOCK_SHUTDOWN;

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

    public boolean hasFdDatasync() {
        return (bits & RIGHTS_FD_DATASYNC) != 0;
    }

    public boolean hasFdRead() {
        return (bits & RIGHTS_FD_READ) != 0;
    }

    public boolean hasFdSeek() {
        return (bits & RIGHTS_FD_SEEK) != 0;
    }

    public boolean hasFdFdstatSetFlags() {
        return (bits & RIGHTS_FD_FDSTAT_SET_FLAGS) != 0;
    }

    public boolean hasFdSync() {
        return (bits & RIGHTS_FD_SYNC) != 0;
    }

    public boolean hasFdTell() {
        return (bits & RIGHTS_FD_TELL) != 0;
    }

    public boolean hasFdWrite() {
        return (bits & RIGHTS_FD_WRITE) != 0;
    }

    public boolean hasFdAdvise() {
        return (bits & RIGHTS_FD_ADVISE) != 0;
    }

    public boolean hasFdAllocate() {
        return (bits & RIGHTS_FD_ALLOCATE) != 0;
    }

    public boolean hasPathCreateDirectory() {
        return (bits & RIGHTS_PATH_CREATE_DIRECTORY) != 0;
    }

    public boolean hasPathCreateFile() {
        return (bits & RIGHTS_PATH_CREATE_FILE) != 0;
    }

    public boolean hasPathLinkSource() {
        return (bits & RIGHTS_PATH_LINK_SOURCE) != 0;
    }

    public boolean hasPathLinkTarget() {
        return (bits & RIGHTS_PATH_LINK_TARGET) != 0;
    }

    public boolean hasPathOpen() {
        return (bits & RIGHTS_PATH_OPEN) != 0;
    }

    public boolean hasFdReaddir() {
        return (bits & RIGHTS_FD_READDIR) != 0;
    }

    public boolean hasPathReadlink() {
        return (bits & RIGHTS_PATH_READLINK) != 0;
    }

    public boolean hasPathRenameSource() {
        return (bits & RIGHTS_PATH_RENAME_SOURCE) != 0;
    }

    public boolean hasPathRenameTarget() {
        return (bits & RIGHTS_PATH_RENAME_TARGET) != 0;
    }

    public boolean hasPathFilestatGet() {
        return (bits & RIGHTS_PATH_FILESTAT_GET) != 0;
    }

    public boolean hasPathFilestatSetSize() {
        return (bits & RIGHTS_PATH_FILESTAT_SET_SIZE) != 0;
    }

    public boolean hasPathFilestatSetTimes() {
        return (bits & RIGHTS_PATH_FILESTAT_SET_TIMES) != 0;
    }

    public boolean hasFdFilestatGet() {
        return (bits & RIGHTS_FD_FILESTAT_GET) != 0;
    }

    public boolean hasFdFilestatSetSize() {
        return (bits & RIGHTS_FD_FILESTAT_SET_SIZE) != 0;
    }

    public boolean hasFdFilestatSetTimes() {
        return (bits & RIGHTS_FD_FILESTAT_SET_TIMES) != 0;
    }

    public boolean hasPathSymlink() {
        return (bits & RIGHTS_PATH_SYMLINK) != 0;
    }

    public boolean hasPathRemoveDirectory() {
        return (bits & RIGHTS_PATH_REMOVE_DIRECTORY) != 0;
    }

    public boolean hasPathUnlinkFile() {
        return (bits & RIGHTS_PATH_UNLINK_FILE) != 0;
    }

    public boolean hasPollFdReadwrite() {
        return (bits & RIGHTS_POLL_FD_READWRITE) != 0;
    }

    public boolean hasSockShutdown() {
        return (bits & RIGHTS_SOCK_SHUTDOWN) != 0;
    }
}
