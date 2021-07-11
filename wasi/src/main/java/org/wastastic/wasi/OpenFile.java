package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.wastastic.wasi.WasiConstants.ERRNO_NOTCAPABLE;

final class OpenFile {
    private final int nativeFd;
    private final byte @Nullable[] prestatDirName;
    private long baseRights;
    private long inheritingRights;

    OpenFile(int nativeFd, byte @Nullable[] prestatDirName, int baseRights, int inheritingRights) {
        this.nativeFd = nativeFd;
        this.prestatDirName = prestatDirName;
        this.baseRights = baseRights;
        this.inheritingRights = inheritingRights;
    }

    public byte @Nullable[] prestatDirName() {
        return prestatDirName;
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
