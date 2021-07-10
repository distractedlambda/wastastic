package org.wastastic.wasi;

import static org.wastastic.wasi.WasiConstants.ERRNO_NOTCAPABLE;

final class OpenFile {
    private final int nativeFd;
    private long baseRights;
    private long inheritingRights;

    OpenFile(int nativeFd, int baseRights, int inheritingRights) {
        this.nativeFd = nativeFd;
        this.baseRights = baseRights;
        this.inheritingRights = inheritingRights;
    }

    int nativeFd() {
        return nativeFd;
    }

    long baseRights() {
        return baseRights;
    }

    long inheritingRights() {
        return inheritingRights;
    }

    void requireBaseRights(long rights) throws ErrnoException {
        if ((baseRights & rights) != rights) {
            throw new ErrnoException(ERRNO_NOTCAPABLE);
        }
    }

    void requireInheritingRights(long rights) throws ErrnoException {
        if ((inheritingRights & rights) != rights) {
            throw new ErrnoException(ERRNO_NOTCAPABLE);
        }
    }

    void setRights(long newBaseRights, long newInheritingRights) throws ErrnoException {
        requireBaseRights(newBaseRights);
        requireInheritingRights(newInheritingRights);
        baseRights = newBaseRights;
        inheritingRights = newInheritingRights;
    }
}
