package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

final class OpenFile implements AutoCloseable {
    private final @NotNull File file;
    private long baseRights;
    private long inheritingRights;
    private int referenceCount = 1;

    OpenFile(@NotNull File file, long baseRights, long inheritingRights) {
        this.file = requireNonNull(file);
        this.baseRights = baseRights;
        this.inheritingRights = inheritingRights;
    }

    @NotNull File file() {
        return file;
    }

    long baseRights() {
        return baseRights;
    }

    long inheritingRights() {
        return inheritingRights;
    }

    void requireBaseRights(long rights) throws ErrnoException {
        if ((baseRights & rights) != rights) {
            throw new ErrnoException(Errno.NOTCAPABLE);
        }
    }

    void requireInheritingRights(long rights) throws ErrnoException {
        if ((inheritingRights & rights) != rights) {
            throw new ErrnoException(Errno.NOTCAPABLE);
        }
    }

    void setRights(long newBaseRights, long newInheritingRights) throws ErrnoException {
        requireBaseRights(newBaseRights);
        requireInheritingRights(newInheritingRights);
        baseRights = newBaseRights;
        inheritingRights = newInheritingRights;
    }

    void addReference() {
        referenceCount++;
    }

    @Override public void close() throws ErrnoException {
        if (--referenceCount == 0) {
            file.fdClose();
        }
    }
}
