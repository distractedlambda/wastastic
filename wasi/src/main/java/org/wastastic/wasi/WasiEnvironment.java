package org.wastastic.wasi;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.wastastic.Memory;
import org.wastastic.ModuleInstance;
import org.wastastic.QualifiedName;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import static java.lang.System.arraycopy;
import static java.lang.invoke.MethodHandles.catchException;
import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodType.methodType;
import static java.lang.ref.Reference.reachabilityFence;
import static java.util.Objects.checkFromIndexSize;
import static java.util.Objects.requireNonNull;
import static jdk.incubator.foreign.MemoryLayout.sequenceLayout;
import static org.wastastic.wasi.WasiConstants.ADVICE_DONTNEED;
import static org.wastastic.wasi.WasiConstants.ADVICE_NOREUSE;
import static org.wastastic.wasi.WasiConstants.ADVICE_NORMAL;
import static org.wastastic.wasi.WasiConstants.ADVICE_RANDOM;
import static org.wastastic.wasi.WasiConstants.ADVICE_SEQUENTIAL;
import static org.wastastic.wasi.WasiConstants.ADVICE_WILLNEED;
import static org.wastastic.wasi.WasiConstants.CLOCKID_MONOTONIC;
import static org.wastastic.wasi.WasiConstants.CLOCKID_PROCESS_CPUTIME_ID;
import static org.wastastic.wasi.WasiConstants.CLOCKID_REALTIME;
import static org.wastastic.wasi.WasiConstants.CLOCKID_THREAD_CPUTIME_ID;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_APPEND;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_DSYNC;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_NONBLOCK;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_RSYNC;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_SYNC;
import static org.wastastic.wasi.WasiConstants.FILETYPE_BLOCK_DEVICE;
import static org.wastastic.wasi.WasiConstants.FILETYPE_CHARACTER_DEVICE;
import static org.wastastic.wasi.WasiConstants.FILETYPE_DIRECTORY;
import static org.wastastic.wasi.WasiConstants.FILETYPE_REGULAR_FILE;
import static org.wastastic.wasi.WasiConstants.FILETYPE_SYMBOLIC_LINK;
import static org.wastastic.wasi.WasiConstants.FILETYPE_UNKNOWN;
import static org.wastastic.wasi.WasiConstants.FSTFLAGS_ATIM;
import static org.wastastic.wasi.WasiConstants.FSTFLAGS_ATIM_NOW;
import static org.wastastic.wasi.WasiConstants.FSTFLAGS_MTIM;
import static org.wastastic.wasi.WasiConstants.FSTFLAGS_MTIM_NOW;
import static org.wastastic.wasi.WasiConstants.ILLEGAL_FDFLAGS;
import static org.wastastic.wasi.WasiConstants.ILLEGAL_FSTFLAGS;
import static org.wastastic.wasi.WasiConstants.LOOKUPFLAGS_SYMLINK_FOLLOW;
import static org.wastastic.wasi.WasiConstants.PREOPENTYPE_DIR;
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
import static org.wastastic.wasi.WasiConstants.RIGHTS_PATH_FILESTAT_GET;
import static org.wastastic.wasi.WasiConstants.WHENCE_END;

public final class WasiEnvironment {
    private final @NotNull VarHandle memoryHandle;
    private final @NotNull Map<@NotNull ClockId, @NotNull Clock> clocks;

    private final int @NotNull[] argOffsets;
    private final byte @NotNull[] argBytes;
    private final int @NotNull[] envOffsets;
    private final byte @NotNull[] envBytes;

    private final Map<Integer, OpenFile> fdTable = new HashMap<>();
    private final ArrayDeque<Integer> retiredFds = new ArrayDeque<>();

    private static final String MODULE_NAME = "wasi_snapshot_preview1";

    public WasiEnvironment(@NotNull VarHandle memoryHandle, @NotNull List<String> args, @NotNull Map<@NotNull ClockId, @NotNull Clock> clocks) {
        this.memoryHandle = requireNonNull(memoryHandle);
        this.clocks = Map.copyOf(clocks);

        argOffsets = new int[args.size()];
        var bytes = new byte[args.size()][];
        var offset = 0;

        for (var i = 0; i < args.size(); i++) {
            argOffsets[i] = offset;
            bytes[i] = args.get(i).getBytes(StandardCharsets.UTF_8);
            offset += bytes[i].length + 1;
        }

        argBytes = new byte[offset];
        offset = 0;

        for (var arg : bytes) {
            arraycopy(arg, 0, argBytes, offset, argBytes.length);
            offset += arg.length + 1;
        }

        throw new UnsupportedOperationException("TODO");
    }

    public @NotNull @Unmodifiable Map<QualifiedName, Object> makeImports() {
        return Map.ofEntries(
            makeImport("args_get", ARGS_GET),
            makeImport("args_sizes_get", ARGS_SIZES_GET),
            makeImport("environ_get", ENVIRON_GET),
            makeImport("environ_sizes_get", ENVIRON_SIZES_GET),
            makeImport("clock_res_get", CLOCK_RES_GET),
            makeImport("clock_time_get", CLOCK_TIME_GET),
            makeImport("fd_advise", FD_ADVISE),
            makeImport("fd_allocate", FD_ALLOCATE),
            makeImport("fd_close", FD_CLOSE),
            makeImport("fd_datasync", FD_DATASYNC),
            makeImport("fd_fdstat_get", FD_FDSTAT_GET),
            makeImport("fd_fdstat_set_flags", FD_FDSTAT_SET_FLAGS),
            makeImport("fd_fdstat_set_rights", FD_FDSTAT_SET_RIGHTS),
            makeImport("fd_filestat_get", FD_FILESTAT_GET)
        );
    }

    private @NotNull Map.Entry<QualifiedName, ?> makeImport(@NotNull String name, @NotNull MethodHandle handle) {
        return Map.entry(new QualifiedName(MODULE_NAME, name), handle.bindTo(this));
    }

    private static final MethodHandle ARGS_GET;
    private static final MethodHandle ARGS_SIZES_GET;
    private static final MethodHandle ENVIRON_GET;
    private static final MethodHandle ENVIRON_SIZES_GET;
    private static final MethodHandle CLOCK_RES_GET;
    private static final MethodHandle CLOCK_TIME_GET;
    private static final MethodHandle FD_ADVISE;
    private static final MethodHandle FD_ALLOCATE;
    private static final MethodHandle FD_CLOSE;
    private static final MethodHandle FD_DATASYNC;
    private static final MethodHandle FD_FDSTAT_GET;
    private static final MethodHandle FD_FDSTAT_SET_FLAGS;
    private static final MethodHandle FD_FDSTAT_SET_RIGHTS;
    private static final MethodHandle FD_FILESTAT_GET;

    private static @NotNull MethodHandle wrapExport(@NotNull MethodHandle handle) {
        if (handle.type().parameterCount() == 0 || handle.type().lastParameterType() != ModuleInstance.class) {
            handle = dropArguments(handle, handle.type().parameterCount(), ModuleInstance.class);
        }

        handle = filterReturnValue(handle, constant(int.class, Errno.SUCCESS.ordinal()));
        handle = catchException(handle, ErrnoException.class, ErrnoException.CODE_HANDLE);

        return handle;
    }

    static {
        var lookup = MethodHandles.lookup();
        try {
            ARGS_GET = wrapExport(lookup.findVirtual(WasiEnvironment.class, "argsGet", methodType(void.class, int.class, int.class, ModuleInstance.class)));
            ARGS_SIZES_GET = wrapExport(lookup.findVirtual(WasiEnvironment.class, "argsSizesGet", methodType(void.class, int.class, int.class, ModuleInstance.class)));
            ENVIRON_GET = wrapExport(lookup.findVirtual(WasiEnvironment.class, "environGet", methodType(void.class, int.class, int.class, ModuleInstance.class)));
            ENVIRON_SIZES_GET = wrapExport(lookup.findVirtual(WasiEnvironment.class, "environSizesGet", methodType(void.class, int.class, int.class, ModuleInstance.class)));
            CLOCK_RES_GET = wrapExport(lookup.findVirtual(WasiEnvironment.class, "clockResGet", methodType(void.class, int.class, int.class, ModuleInstance.class)));
            CLOCK_TIME_GET = wrapExport(lookup.findVirtual(WasiEnvironment.class, "clockTimeGet", methodType(void.class, int.class, long.class, int.class, ModuleInstance.class)));
            FD_ADVISE = wrapExport(lookup.findVirtual(WasiEnvironment.class, "fdAdvise", methodType(void.class, int.class, long.class, long.class, int.class)));
            FD_ALLOCATE = wrapExport(lookup.findVirtual(WasiEnvironment.class, "fdAllocate", methodType(void.class, int.class, long.class, long.class)));
            FD_CLOSE = wrapExport(lookup.findVirtual(WasiEnvironment.class, "fdClose", methodType(void.class, int.class)));
            FD_DATASYNC = wrapExport(lookup.findVirtual(WasiEnvironment.class, "fdDatasync", methodType(void.class, int.class)));
            FD_FDSTAT_GET = wrapExport(lookup.findVirtual(WasiEnvironment.class, "fdFdstatGet", methodType(void.class, int.class, int.class, ModuleInstance.class)));
            FD_FDSTAT_SET_FLAGS = wrapExport(lookup.findVirtual(WasiEnvironment.class, "fdFdstatSetFlags", methodType(void.class, int.class, short.class)));
            FD_FDSTAT_SET_RIGHTS = wrapExport(lookup.findVirtual(WasiEnvironment.class, "fdFdstatSetRights", methodType(void.class, int.class, long.class, long.class)));
            FD_FILESTAT_GET = wrapExport(lookup.findVirtual(WasiEnvironment.class, "fdFilestatGet", methodType(void.class, int.class, int.class, ModuleInstance.class)));
        }
        catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new UnsupportedOperationException(exception);
        }
    }

    private @NotNull Memory.Pinned pinMemory(@NotNull ModuleInstance module) {
        return ((Memory) memoryHandle.get(module)).pin();
    }

    private static void checkBounds(@NotNull Memory.Pinned memory, int start, int size) throws ErrnoException {
        checkBounds(memory, start, Integer.toUnsignedLong(size));
    }

    private static void checkBounds(@NotNull Memory.Pinned memory, int start, long size) throws ErrnoException {
        if (memory.segment().byteSize() - Integer.toUnsignedLong(start) < size) {
            throw new ErrnoException(Errno.FAULT);
        }
    }

    private void argsGet(int offsetsAddress, int dataAddress, @NotNull ModuleInstance module) throws ErrnoException {
        try (var memory = pinMemory(module)) {
            checkBounds(memory, offsetsAddress, argOffsets.length * 4L);
            checkBounds(memory, dataAddress, argBytes.length);

            for (var i = 0; i < argOffsets.length; i++) {
                memory.setInt(offsetsAddress + i*4, dataAddress + argOffsets[i]);
            }

            memory.segment(dataAddress).copyFrom(MemorySegment.ofArray(argBytes));
        }
    }

    private void argsSizesGet(int numArgsAddress, int dataSizeAddress, @NotNull ModuleInstance module) throws ErrnoException {
        try (var memory = pinMemory(module)) {
            checkBounds(memory, numArgsAddress, 4);
            checkBounds(memory, dataSizeAddress, 4);
            memory.setInt(numArgsAddress, argOffsets.length);
            memory.setInt(dataSizeAddress, argBytes.length);
        }
    }

    private void environGet(int offsetsAddress, int dataAddress, @NotNull ModuleInstance module) throws ErrnoException {
        try (var memory = pinMemory(module)) {
            checkBounds(memory, offsetsAddress, envOffsets.length * 4L);
            checkBounds(memory, dataAddress, envBytes.length);

            for (var i = 0; i < envOffsets.length; i++) {
                memory.setInt(offsetsAddress + i*4, dataAddress + envOffsets[i]);
            }

            memory.segment(dataAddress).copyFrom(MemorySegment.ofArray(envBytes));
        }
    }

    private void environSizesGet(int numVarsAddress, int dataSizeAddress, @NotNull ModuleInstance module) throws ErrnoException {
        try (var memory = pinMemory(module)) {
            checkBounds(memory, numVarsAddress, 4);
            checkBounds(memory, dataSizeAddress, 4);
            memory.setInt(numVarsAddress, envOffsets.length);
            memory.setInt(dataSizeAddress, envBytes.length);
        }
    }

    private static @NotNull ClockId decodeClockId(int id) throws ErrnoException {
        switch (id) {
            case CLOCKID_REALTIME -> {
                return ClockId.REALTIME;
            }

            case CLOCKID_MONOTONIC -> {
                return ClockId.MONOTONIC;
            }

            case CLOCKID_PROCESS_CPUTIME_ID -> {
                return ClockId.PROCESS_CPUTIME_ID;
            }

            case CLOCKID_THREAD_CPUTIME_ID -> {
                return ClockId.THREAD_CPUTIME_ID;
            }

            default -> {
                throw new ErrnoException(Errno.NOTSUP);
            }
        }
    }

    private @NotNull Clock getClock(int id) throws ErrnoException {
        Clock clock;

        if ((clock = clocks.get(decodeClockId(id))) == null) {
            throw new ErrnoException(Errno.NOTSUP);
        }

        return clock;
    }

    private void clockResGet(int clockId, int resolutionOut, @NotNull ModuleInstance module) throws ErrnoException {
        try (var memory = pinMemory(module)) {
            checkBounds(memory, resolutionOut, 8);
            memory.setLong(resolutionOut, getClock(clockId).resolutionNanos());
        }
    }

    private void clockTimeGet(int clockId, long precision, int timeOut, @NotNull ModuleInstance module) throws ErrnoException {
        try (var memory = pinMemory(module)) {
            checkBounds(memory, timeOut, 8);
            memory.setLong(timeOut, getClock(clockId).currentTimeNanos(precision));
        }
    }

    private @NotNull OpenFile getOpenFile(int fd) throws ErrnoException {
        OpenFile file;

        if ((file = fdTable.get(fd)) == null) {
            throw new ErrnoException(Errno.BADF);
        }

        return file;
    }

    private void fdAdvise(int fd, long offset, long length, int advice) throws ErrnoException {
        Advice decodedAdvice;
        switch (advice) {
            case ADVICE_NORMAL -> {
                decodedAdvice = Advice.NORMAL;
            }

            case ADVICE_SEQUENTIAL -> {
                decodedAdvice = Advice.SEQUENTIAL;
            }

            case ADVICE_RANDOM -> {
                decodedAdvice = Advice.RANDOM;
            }

            case ADVICE_WILLNEED -> {
                decodedAdvice = Advice.WILLNEED;
            }

            case ADVICE_DONTNEED -> {
                decodedAdvice = Advice.DONTNEED;
            }

            case ADVICE_NOREUSE -> {
                decodedAdvice = Advice.NOREUSE;
            }

            default -> {
                throw new ErrnoException(Errno.INVAL);
            }
        }

        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_ADVISE);
        openFile.file().advise(offset, length, decodedAdvice);
    }

    private void fdAllocate(int fd, long offset, long length) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_ALLOCATE);
        openFile.file().allocate(offset, length);
    }

    private void fdClose(int fd) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.file().close();
        fdTable.remove(fd);
        retiredFds.addLast(fd);
    }

    private void fdDatasync(int fd) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_DATASYNC);
        openFile.file().datasync();
    }

    private void fdFdstatGet(int fd, int statOut, @NotNull ModuleInstance module) throws ErrnoException {
        var openFile = getOpenFile(fd);
        var stat = openFile.file().fdstat();

        short flags = 0;

        if (stat.append()) {
            flags |= FDFLAGS_APPEND;
        }

        if (stat.dsync()) {
            flags |= FDFLAGS_DSYNC;
        }

        if (stat.nonblock()) {
            flags |= FDFLAGS_NONBLOCK;
        }

        if (stat.rsync()) {
            flags |= FDFLAGS_RSYNC;
        }

        if (stat.sync()) {
            flags |= FDFLAGS_SYNC;
        }

        try (var memory = pinMemory(module)) {
            checkBounds(memory, statOut, 24);
            memory.setByte(statOut, (byte) stat.filetype().ordinal());
            memory.setShort(statOut + 2, flags);
            memory.setLong(statOut + 8, openFile.baseRights());
            memory.setLong(statOut + 16, openFile.inheritingRights());
        }
    }

    private void fdFdstatSetFlags(int fd, short newFlags) throws ErrnoException {
        if ((newFlags & ILLEGAL_FDFLAGS) != 0) {
            throw new ErrnoException(Errno.INVAL);
        }

        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_FDSTAT_SET_FLAGS);
        openFile.file().setFlags((newFlags & FDFLAGS_APPEND) != 0, (newFlags & FDFLAGS_DSYNC) != 0, (newFlags & FDFLAGS_NONBLOCK) != 0, (newFlags & FDFLAGS_RSYNC) != 0, (newFlags & FDFLAGS_SYNC) != 0);
    }

    private void fdFdstatSetRights(int fd, long newBaseRights, long newInheritingRights) throws ErrnoException {
        getOpenFile(fd).setRights(newBaseRights, newInheritingRights);
    }

    private void fdFilestatGet(int fd, int filestatOut, @NotNull ModuleInstance module) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_FILESTAT_GET);
        var stat = openFile.file().filestat();

        try (var memory = pinMemory(module)) {
            checkBounds(memory, filestatOut, 64);
            memory.setLong(filestatOut, stat.deviceId());
            memory.setLong(filestatOut + 8, stat.inode());
            memory.setByte(filestatOut + 16, (byte) stat.filetype().ordinal());
            memory.setLong(filestatOut + 24, stat.linkCount());
            memory.setLong(filestatOut + 32, stat.size());
            memory.setLong(filestatOut + 40, stat.accessTimeNanos());
            memory.setLong(filestatOut + 48, stat.modificationTimeNanos());
            memory.setLong(filestatOut + 56, stat.statusChangeTimeNanos());
        }
    }

    private void fdFilestatSetSize(int fd, long size) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_FILESTAT_SET_SIZE);
        openFile.file().setSize(size);
    }

    private void fdFilestatSetTimes(int fd, long newAccessTime, long newModificationTime, short fstFlags) throws ErrnoException {
        if ((fstFlags & ILLEGAL_FSTFLAGS) != 0) {
            throw new ErrnoException(Errno.INVAL);
        }

        TimeOverride accessTimeOverride, modificationTimeOverride;

        if ((fstFlags & FSTFLAGS_ATIM_NOW) != 0) {
            if ((fstFlags & FSTFLAGS_ATIM) != 0) {
                throw new ErrnoException(Errno.INVAL);
            }

            accessTimeOverride = TimeOverride.now();
        }
        else if ((fstFlags & FSTFLAGS_ATIM) != 0) {
            accessTimeOverride = TimeOverride.nanos(newAccessTime);
        }
        else {
            accessTimeOverride = TimeOverride.disabled();
        }

        if ((fstFlags & FSTFLAGS_MTIM_NOW) != 0) {
            if ((fstFlags & FSTFLAGS_MTIM) != 0) {
                throw new ErrnoException(Errno.INVAL);
            }

            modificationTimeOverride = TimeOverride.now();
        }
        else if ((fstFlags & FSTFLAGS_MTIM) != 0) {
            modificationTimeOverride = TimeOverride.nanos(newModificationTime);
        }
        else {
            modificationTimeOverride = TimeOverride.disabled();
        }

        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_FILESTAT_SET_TIMES);
        openFile.file().setTimes(accessTimeOverride, modificationTimeOverride);
    }

    private static @NotNull List<@NotNull MemorySegment> extractIovecs(@NotNull Memory.Pinned memory, int iovsAddress, int iovsCount) throws ErrnoException {
        checkBounds(memory, iovsAddress, Integer.toUnsignedLong(iovsCount) * 8L);

        if (iovsCount < 0) {
            throw new ErrnoException(Errno.NOMEM);
        }

        var totalLength = 0L;
        var segments = new MemorySegment[iovsCount];

        for (var i = 0; i != iovsCount; i++) {
            var bufferAddress = memory.getInt(iovsAddress + 8*i);
            var bufferLength = memory.getInt(iovsAddress + 8*i + 4);

            totalLength += Integer.toUnsignedLong(bufferLength);
            if (totalLength > Integer.toUnsignedLong(-1)) {
                throw new ErrnoException(Errno.INVAL);
            }

            checkBounds(memory, bufferAddress, bufferLength);
            segments[i] = memory.segment(bufferAddress, bufferLength);
        }

        return List.of(segments);
    }

    private void fdPread(int fd, int iovsAddress, int iovsCount, long offset, int bytesReadAddress, @NotNull ModuleInstance module) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_READ);

        try (var memory = pinMemory(module)) {
            checkBounds(memory, bytesReadAddress, 4);
            memory.setInt(bytesReadAddress, openFile.file().pread(extractIovecs(memory, iovsAddress, iovsCount), offset));
        }
    }

    private void fdPrestatGet(int fd, int prestatOut, @NotNull ModuleInstance module) throws ErrnoException {
        var openFile = getOpenFile(fd);

        var name = openFile.file().preopenedDirectoryName();
        if (name == null) {
            throw new ErrnoException(Errno.BADF);
        }

        try (var memory = pinMemory(module)) {
            checkBounds(memory, prestatOut, 8);
            memory.setByte(prestatOut, PREOPENTYPE_DIR);
            memory.setInt(prestatOut + 4, name.length);
        }
    }

    private void fdPrestatDirName(int fd, int buffer, int bufferLength, @NotNull ModuleInstance module) throws ErrnoException {
        var openFile = getOpenFile(fd);

        var name = openFile.file().preopenedDirectoryName();
        if (name == null) {
            throw new ErrnoException(Errno.BADF);
        }

        if (bufferLength != name.length) {
            throw new ErrnoException(Errno.INVAL);
        }

        try (var memory = pinMemory(module)) {
            checkBounds(memory, buffer, name.length);
            memory.segment(buffer).copyFrom(MemorySegment.ofArray(name));
        }
    }

    private void fdPwrite(int fd, int iovsAddress, int iovsCount, long offset, int bytesWrittenAddress, @NotNull ModuleInstance module) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_WRITE);

        try (var memory = pinMemory(module)) {
            checkBounds(memory, bytesWrittenAddress, 4);
            memory.setInt(bytesWrittenAddress, openFile.file().pwrite(extractIovecs(memory, iovsAddress, iovsCount), offset));
        }
    }

    private void fdRead(int fd, int iovsAddress, int iovsCount, int bytesReadAddress, @NotNull ModuleInstance module) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_READ);

        try (var memory = pinMemory(module)) {
            checkBounds(memory, bytesReadAddress, 4);
            memory.setInt(bytesReadAddress, openFile.file().read(extractIovecs(memory, iovsAddress, iovsCount)));
        }
    }

    private void fdReaddir(int fd, int buf, int bufLen, long cookie, int sizeAddress, @NotNull ModuleInstance module) throws ErrnoException {
        var openFile = getOpenFile(fd);
        openFile.requireBaseRights(RIGHTS_FD_READDIR);

        try (var memory = pinMemory(module)) {
            checkBounds(memory, buf, bufLen);
            checkBounds(memory, sizeAddress, 4);

            var entries = openFile.file().readDir(cookie);
            var bufOffset = 0;

            for (var entry : entries) {
                if (bufLen - bufOffset < 24) {
                    bufOffset = bufLen;
                    break;
                }

                var nameLength = Integer.min(bufLen - bufOffset - 24, entry.name().length);

                memory.setLong(buf + bufOffset, entry.nextCookie());
                memory.setLong(buf + bufOffset + 8, entry.inode());
                memory.setInt(buf + bufOffset + 16, nameLength);
                memory.setByte(buf + bufOffset + 20, (byte) entry.filetype().ordinal());
                memory.segment(24).copyFrom(MemorySegment.ofArray(entry.name()).asSlice(0, nameLength));

                bufOffset += 24 + nameLength;
            }

            memory.setInt(sizeAddress, bufOffset);
        }
    }

    private void fdRenumber(int fdFrom, int fdTo, @NotNull ModuleInstance module) throws Throwable {
        // FIXME: is this supposed to close the old file?
        var fromFile = getOpenFile(fdFrom);
        var toFile = getOpenFile(fdTo);
        Linux.dup2.invokeExact(fromFile.nativeFd(), toFile.nativeFd());
    }

    private void fdSeek(int fd, long delta, byte whence, int newOffsetAddress, @NotNull ModuleInstance module) throws Throwable {
        if (Byte.compareUnsigned(whence, WHENCE_END) > 0) {
            throw new ErrnoException(ERRNO_INVAL);
        }

        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_SEEK);
        var newOffset = (long) Linux.lseek.invokeExact(file.nativeFd(), delta, (int) whence);
        memory.setLong(newOffsetAddress, newOffset);
    }

    private void fdSync(int fd, @NotNull ModuleInstance module) throws Throwable {
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_SYNC);
        Linux.fsync.invokeExact(file.nativeFd());
    }

    private void fdTell(int fd, int resultAddress, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_TELL);
        var offset = (long) Linux.lseek.invokeExact(file.nativeFd(), 0, Linux.SEEK_CUR);
        memory.setLong(resultAddress, offset);
    }

    private void fdWrite(int fd, int iovsAddress, int iovsCount, int bytesWrittenAddress, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_WRITE);

        long bytesWritten;
        if (iovsCount == 0) {
            bytesWritten = 0;
        }
        else if (iovsCount == 1) {
            var buf = memory.getInt(iovsAddress);
            var bufLen = memory.getInt(iovsAddress + 4);
            var bufSlice = memory.currentSegment().asSlice(buf, bufLen);
            bytesWritten = (long) Linux.write.invokeExact(file.nativeFd(), bufSlice.address(), bufSlice.byteSize());
        }
        else {
            var segment = memory.currentSegment();
            try (var frame = MemoryStack.getFrame()) {
                var nativeIovs = makeNativeIovecs(segment, iovsAddress, iovsCount, frame);
                bytesWritten = (long) Linux.writev.invokeExact(file.nativeFd(), nativeIovs.address(), iovsCount);
            } finally {
                reachabilityFence(segment.scope());
            }
        }

        memory.setInt(bytesWrittenAddress, (int) bytesWritten);
    }

    private static MemoryAddress makePathString(@NotNull Memory memory, int strPtr, int strLen, @NotNull SegmentAllocator allocator) throws ErrnoException {
        var segment = memory.currentSegment();

        if (strLen != 0 && (byte) Memory.VH_BYTE.get(segment, strPtr) == '/') {
            // FIXME: better error code here
            throw new ErrnoException(ERRNO_INVAL);
        }

        var nullAddress = Integer.toUnsignedLong(strPtr) + Integer.toUnsignedLong(strLen);

        if (nullAddress < segment.byteSize() && (byte) Memory.VH_BYTE.get(segment, nullAddress) == 0) {
            return segment.address().addOffset(Integer.toUnsignedLong(strPtr));
        }
        else {
            var cstr = allocator.allocate(Integer.toUnsignedLong(strLen) + 1, 1);
            cstr.copyFrom(segment.asSlice(Integer.toUnsignedLong(strPtr), Integer.toUnsignedLong(strLen)));
            Memory.VH_BYTE.set(cstr, Integer.toUnsignedLong(strLen), (byte) 0);
            return cstr.address();
        }
    }

    private void pathCreateDirectory(int fd, int pathPtr, int pathLen, @NotNull ModuleInstance module) throws ErrnoException {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_PATH_CREATE_DIRECTORY);
        try (var frame = MemoryStack.getFrame()) {
            var pathStr = makePathString(memory, pathPtr, pathLen, frame);
        }
    }

    private int pathFilestatGet(int fd, int lookupflags, int pathPtr, int pathLen, int outPtr, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_PATH_FILESTAT_GET) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        Path path;
        try {
            path = Path.of(memory.getUtf8(pathPtr, pathLen));
        }
        catch (InvalidPathException ignored) {
            return ERRNO_INVAL;
        }

        if (path.isAbsolute() || path.getRoot() != null) {
            return ERRNO_INVAL;
        }

        LinkOption[] linkOptions;
        if (lookupflags == LOOKUPFLAGS_SYMLINK_FOLLOW) {
            linkOptions = new LinkOption[]{LinkOption.NOFOLLOW_LINKS};
        }
        else if (lookupflags != 0) {
            return ERRNO_INVAL;
        }
        else {
            linkOptions = new LinkOption[0];
        }

        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class, linkOptions);
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        memory.setLong(outPtr, 0); // 'dev', unimplemented
        memory.setLong(outPtr + 8, 0); // 'ino', unimplemented
        memory.setByte(outPtr + 16, detectFiletype(attributes));
        memory.setLong(outPtr + 24, 0); // 'nlink', unimplemented
        memory.setLong(outPtr + 32, attributes.size());
        memory.setLong(outPtr + 40, attributes.lastAccessTime().to(TimeUnit.NANOSECONDS));
        memory.setLong(outPtr + 48, attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS));
        memory.setLong(outPtr + 56, attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS));

        return ERRNO_SUCCESS;
    }
}
