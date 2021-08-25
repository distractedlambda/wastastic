package org.wastastic.wasi;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wastastic.Memory;

import java.util.List;

public interface File {
    void fdAdvise(long offset, long length, @NotNull Advice advice) throws ErrnoException;

    void fdAllocate(long offset, long length) throws ErrnoException;

    void fdClose() throws ErrnoException;

    void fdDatasync() throws ErrnoException;

    @NotNull Fdstat fdFdstatGet() throws ErrnoException;

    void fdFdstatSetFlags(@NotNull FdFlags flags) throws ErrnoException;

    @NotNull Filestat fdFilestatGet() throws ErrnoException;

    void fdFilestatSetSize(long size) throws ErrnoException;

    void fdFilestatSetTimes(@NotNull TimeOverride accessTime, @NotNull TimeOverride modificationTime) throws ErrnoException;

    int fdPread(@NotNull List<@NotNull MemorySegment> destination, long offset) throws ErrnoException;

    byte @Nullable[] fdPrestatDirName() throws ErrnoException;

    int fdPwrite(@NotNull List<@NotNull MemorySegment> source, long offset) throws ErrnoException;

    int fdRead(@NotNull List<@NotNull MemorySegment> destination) throws ErrnoException;

    @NotNull List<@NotNull DirectoryEntry> fdReaddir(long cookie) throws ErrnoException;

    long fdSeek(long offset, @NotNull Whence whence) throws ErrnoException;

    void fdSync() throws ErrnoException;

    long fdTell() throws ErrnoException;

    int fdWrite(@NotNull List<@NotNull MemorySegment> source) throws ErrnoException;

    void pathCreateDirectory(@NotNull MemorySegment path) throws ErrnoException;

    @NotNull Filestat pathFilestatGet(@NotNull LookupFlags flags, @NotNull MemorySegment path) throws ErrnoException;

    void pathFilestatSetTimes(@NotNull LookupFlags flags, @NotNull MemorySegment path, @NotNull TimeOverride accessTime, @NotNull TimeOverride modificationTime) throws ErrnoException;

    void pathLink(@NotNull LookupFlags flags, @NotNull MemorySegment oldPath, @NotNull File newRoot, @NotNull MemorySegment newPath) throws ErrnoException;

    @NotNull File pathOpen(@NotNull LookupFlags flags, @NotNull MemorySegment path, @NotNull OFlags oflags, @NotNull Rights baseRights, @NotNull Rights inheritingRights, @NotNull FdFlags fdflags) throws ErrnoException;

    int pathReadlink(@NotNull MemorySegment path, @NotNull MemorySegment destination) throws ErrnoException;

    void pathRemoveDirectory(@NotNull MemorySegment path) throws ErrnoException;

    void pathRename(@NotNull MemorySegment oldPath, @NotNull File newRoot, @NotNull MemorySegment newPath) throws ErrnoException;

    void pathSymlink(@NotNull MemorySegment oldPath, @NotNull MemorySegment newPath) throws ErrnoException;

    void pathUnlinkFile(@NotNull MemorySegment path) throws ErrnoException;
}
