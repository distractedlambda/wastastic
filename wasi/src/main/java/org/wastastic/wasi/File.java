package org.wastastic.wasi;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface File extends AutoCloseable {
    default void advise(long offset, long length, @NotNull Advice advice) throws ErrnoException {}

    default void allocate(long offset, long length) throws ErrnoException {}

    @Override default void close() throws ErrnoException {}

    default void datasync() throws ErrnoException {}

    default @NotNull Fdstat fdstat() throws ErrnoException {
        throw new ErrnoException(Errno.NOTSUP);
    }

    default void setFlags(boolean append, boolean dsync, boolean nonblock, boolean rsync, boolean sync) throws ErrnoException {}

    @NotNull Filestat filestat() throws ErrnoException;

    void setSize(long size) throws ErrnoException;

    void setTimes(@NotNull TimeOverride accessTime, @NotNull TimeOverride modificationTime) throws ErrnoException;

    int pread(@NotNull List<@NotNull MemorySegment> destination, long offset) throws ErrnoException;

    byte @Nullable[] preopenedDirectoryName() throws ErrnoException;

    int pwrite(@NotNull List<@NotNull MemorySegment> source, long offset) throws ErrnoException;

    int read(@NotNull List<@NotNull MemorySegment> destination) throws ErrnoException;

    @NotNull List<@NotNull DirectoryEntry> readDir(long cookie) throws ErrnoException;

    long seek(long offset, @NotNull Whence whence) throws ErrnoException;

    void sync() throws ErrnoException;

    long tell() throws ErrnoException;

    int write(@NotNull List<@NotNull MemorySegment> source) throws ErrnoException;

    void createDirectory(@NotNull MemorySegment path) throws ErrnoException;

    @NotNull Filestat filestat(boolean followSymlinks, @NotNull MemorySegment path) throws ErrnoException;

    void setTimes(boolean followSymlinks, @NotNull MemorySegment path, @NotNull TimeOverride accessTime, @NotNull TimeOverride modificationTime) throws ErrnoException;

    void link(boolean followSymlinks, @NotNull MemorySegment oldPath, @NotNull File newRoot, @NotNull MemorySegment newPath) throws ErrnoException;

    @NotNull File open(boolean followSymlinks, @NotNull MemorySegment path, boolean create, boolean directory, boolean exclusive, boolean truncate, boolean append, boolean dsync, boolean nonblock, boolean rsync, boolean sync, @NotNull Rights baseRights, @NotNull Rights inheritingRights) throws ErrnoException;

    byte @NotNull[] readLink(@NotNull MemorySegment path) throws ErrnoException;

    void removeDirectory(@NotNull MemorySegment path) throws ErrnoException;

    void rename(@NotNull MemorySegment oldPath, @NotNull File newRoot, @NotNull MemorySegment newPath) throws ErrnoException;

    void symlink(@NotNull MemorySegment oldPath, @NotNull File newRoot, @NotNull MemorySegment newPath) throws ErrnoException;

    void unlinkFile(@NotNull MemorySegment path) throws ErrnoException;
}
