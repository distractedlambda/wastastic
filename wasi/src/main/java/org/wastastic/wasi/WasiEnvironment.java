package org.wastastic.wasi;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
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
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.SwitchPoint;
import java.lang.invoke.VarHandle;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import static java.lang.System.arraycopy;
import static java.lang.invoke.MethodHandles.catchException;
import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodType.methodType;
import static java.lang.ref.Reference.reachabilityFence;
import static java.util.Objects.requireNonNull;
import static jdk.incubator.foreign.MemoryLayout.sequenceLayout;
import static org.wastastic.wasi.WasiConstants.ADVICE_DONTNEED;
import static org.wastastic.wasi.WasiConstants.ADVICE_NOREUSE;
import static org.wastastic.wasi.WasiConstants.ADVICE_NORMAL;
import static org.wastastic.wasi.WasiConstants.ADVICE_RANDOM;
import static org.wastastic.wasi.WasiConstants.ADVICE_SEQUENTIAL;
import static org.wastastic.wasi.WasiConstants.ADVICE_WILLNEED;
import static org.wastastic.wasi.WasiConstants.CLOCKID_THREAD_CPUTIME_ID;
import static org.wastastic.wasi.WasiConstants.ERRNO_2BIG;
import static org.wastastic.wasi.WasiConstants.ERRNO_ACCES;
import static org.wastastic.wasi.WasiConstants.ERRNO_ADDRINUSE;
import static org.wastastic.wasi.WasiConstants.ERRNO_ADDRNOTAVAIL;
import static org.wastastic.wasi.WasiConstants.ERRNO_AFNOSUPPORT;
import static org.wastastic.wasi.WasiConstants.ERRNO_AGAIN;
import static org.wastastic.wasi.WasiConstants.ERRNO_ALREADY;
import static org.wastastic.wasi.WasiConstants.ERRNO_BADF;
import static org.wastastic.wasi.WasiConstants.ERRNO_BADMSG;
import static org.wastastic.wasi.WasiConstants.ERRNO_BUSY;
import static org.wastastic.wasi.WasiConstants.ERRNO_CANCELED;
import static org.wastastic.wasi.WasiConstants.ERRNO_CHILD;
import static org.wastastic.wasi.WasiConstants.ERRNO_CONNABORTED;
import static org.wastastic.wasi.WasiConstants.ERRNO_CONNREFUSED;
import static org.wastastic.wasi.WasiConstants.ERRNO_CONNRESET;
import static org.wastastic.wasi.WasiConstants.ERRNO_DEADLK;
import static org.wastastic.wasi.WasiConstants.ERRNO_DESTADDRREQ;
import static org.wastastic.wasi.WasiConstants.ERRNO_DOM;
import static org.wastastic.wasi.WasiConstants.ERRNO_DQUOT;
import static org.wastastic.wasi.WasiConstants.ERRNO_EXIST;
import static org.wastastic.wasi.WasiConstants.ERRNO_FAULT;
import static org.wastastic.wasi.WasiConstants.ERRNO_FBIG;
import static org.wastastic.wasi.WasiConstants.ERRNO_HOSTUNREACH;
import static org.wastastic.wasi.WasiConstants.ERRNO_IDRM;
import static org.wastastic.wasi.WasiConstants.ERRNO_ILSEQ;
import static org.wastastic.wasi.WasiConstants.ERRNO_INPROGRESS;
import static org.wastastic.wasi.WasiConstants.ERRNO_INTR;
import static org.wastastic.wasi.WasiConstants.ERRNO_INVAL;
import static org.wastastic.wasi.WasiConstants.ERRNO_IO;
import static org.wastastic.wasi.WasiConstants.ERRNO_ISCONN;
import static org.wastastic.wasi.WasiConstants.ERRNO_ISDIR;
import static org.wastastic.wasi.WasiConstants.ERRNO_LOOP;
import static org.wastastic.wasi.WasiConstants.ERRNO_MFILE;
import static org.wastastic.wasi.WasiConstants.ERRNO_MLINK;
import static org.wastastic.wasi.WasiConstants.ERRNO_MSGSIZE;
import static org.wastastic.wasi.WasiConstants.ERRNO_MULTIHOP;
import static org.wastastic.wasi.WasiConstants.ERRNO_NAMETOOLONG;
import static org.wastastic.wasi.WasiConstants.ERRNO_NETDOWN;
import static org.wastastic.wasi.WasiConstants.ERRNO_NETRESET;
import static org.wastastic.wasi.WasiConstants.ERRNO_NETUNREACH;
import static org.wastastic.wasi.WasiConstants.ERRNO_NFILE;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOBUFS;
import static org.wastastic.wasi.WasiConstants.ERRNO_NODEV;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOENT;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOEXEC;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOLCK;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOLINK;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOMEM;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOMSG;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOPROTOOPT;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOSPC;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOSYS;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOTCAPABLE;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOTCONN;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOTDIR;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOTEMPTY;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOTRECOVERABLE;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOTSOCK;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOTSUP;
import static org.wastastic.wasi.WasiConstants.ERRNO_NOTTY;
import static org.wastastic.wasi.WasiConstants.ERRNO_NXIO;
import static org.wastastic.wasi.WasiConstants.ERRNO_OVERFLOW;
import static org.wastastic.wasi.WasiConstants.ERRNO_OWNERDEAD;
import static org.wastastic.wasi.WasiConstants.ERRNO_PERM;
import static org.wastastic.wasi.WasiConstants.ERRNO_PIPE;
import static org.wastastic.wasi.WasiConstants.ERRNO_PROTO;
import static org.wastastic.wasi.WasiConstants.ERRNO_PROTONOSUPPORT;
import static org.wastastic.wasi.WasiConstants.ERRNO_PROTOTYPE;
import static org.wastastic.wasi.WasiConstants.ERRNO_RANGE;
import static org.wastastic.wasi.WasiConstants.ERRNO_ROFS;
import static org.wastastic.wasi.WasiConstants.ERRNO_SPIPE;
import static org.wastastic.wasi.WasiConstants.ERRNO_SRCH;
import static org.wastastic.wasi.WasiConstants.ERRNO_STALE;
import static org.wastastic.wasi.WasiConstants.ERRNO_SUCCESS;
import static org.wastastic.wasi.WasiConstants.ERRNO_TIMEDOUT;
import static org.wastastic.wasi.WasiConstants.ERRNO_TXTBSY;
import static org.wastastic.wasi.WasiConstants.ERRNO_XDEV;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_APPEND;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_DSYNC;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_NONBLOCK;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_RSYNC;
import static org.wastastic.wasi.WasiConstants.FDFLAGS_SYNC;
import static org.wastastic.wasi.WasiConstants.FILETYPE_BLOCK_DEVICE;
import static org.wastastic.wasi.WasiConstants.FILETYPE_CHARACTER_DEVICE;
import static org.wastastic.wasi.WasiConstants.FILETYPE_DIRECTORY;
import static org.wastastic.wasi.WasiConstants.FILETYPE_REGULAR_FILE;
import static org.wastastic.wasi.WasiConstants.FILETYPE_SOCKET_DGRAM;
import static org.wastastic.wasi.WasiConstants.FILETYPE_SOCKET_STREAM;
import static org.wastastic.wasi.WasiConstants.FILETYPE_SYMBOLIC_LINK;
import static org.wastastic.wasi.WasiConstants.FILETYPE_UNKNOWN;
import static org.wastastic.wasi.WasiConstants.FSTFLAGS_ATIM;
import static org.wastastic.wasi.WasiConstants.FSTFLAGS_ATIM_NOW;
import static org.wastastic.wasi.WasiConstants.FSTFLAGS_MTIM;
import static org.wastastic.wasi.WasiConstants.FSTFLAGS_MTIM_NOW;
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
import static org.wastastic.wasi.WasiConstants.WHENCE_CUR;
import static org.wastastic.wasi.WasiConstants.WHENCE_END;
import static org.wastastic.wasi.WasiConstants.WHENCE_SET;

public final class WasiEnvironment {
    private final @NotNull VarHandle memoryHandle;
    private final int @NotNull[] argOffsets;
    private final byte @NotNull[] argBytes;
    private final int @NotNull[] envOffsets;
    private final byte @NotNull[] envBytes;

    private final Map<Integer, OpenFile> fdTable = new HashMap<>();
    private final ArrayDeque<Integer> retiredFds = new ArrayDeque<>();

    private static final String MODULE_NAME = "wasi_snapshot_preview1";

    public WasiEnvironment(@NotNull VarHandle memoryHandle, @NotNull List<String> args) {
        this.memoryHandle = requireNonNull(memoryHandle);

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
        handle = handle.bindTo(this);
        handle = filterReturnValue(handle, constant(int.class, ERRNO_SUCCESS));
        handle = catchException(handle, ErrnoException.class, ErrnoException.CODE_HANDLE);
        return Map.entry(new QualifiedName(MODULE_NAME, name), handle);
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

    static {
        var lookup = MethodHandles.lookup();
        try {
            ARGS_GET = lookup.findVirtual(WasiEnvironment.class, "argsGet", methodType(int.class, int.class, int.class, ModuleInstance.class));
            ARGS_SIZES_GET = lookup.findVirtual(WasiEnvironment.class, "argsSizesGet", methodType(int.class, int.class, int.class, ModuleInstance.class));
            ENVIRON_GET = lookup.findVirtual(WasiEnvironment.class, "environGet", methodType(int.class, int.class, int.class, ModuleInstance.class));
            ENVIRON_SIZES_GET = lookup.findVirtual(WasiEnvironment.class, "environSizesGet", methodType(int.class, int.class, int.class, ModuleInstance.class));
            CLOCK_RES_GET = lookup.findVirtual(WasiEnvironment.class, "clockResGet", methodType(int.class, int.class, int.class, ModuleInstance.class));
            CLOCK_TIME_GET = lookup.findVirtual(WasiEnvironment.class, "clockTimeGet", methodType(int.class, int.class, long.class, int.class, ModuleInstance.class));
            FD_ADVISE = lookup.findVirtual(WasiEnvironment.class, "fdAdvise", methodType(int.class, int.class, long.class, long.class, int.class, ModuleInstance.class));
            FD_ALLOCATE = lookup.findVirtual(WasiEnvironment.class, "fdAllocate", methodType(int.class, int.class, long.class, long.class, ModuleInstance.class));
            FD_CLOSE = lookup.findVirtual(WasiEnvironment.class, "fdClose", methodType(int.class, int.class, ModuleInstance.class));
            FD_DATASYNC = lookup.findVirtual(WasiEnvironment.class, "fdDatasync", methodType(int.class, int.class, ModuleInstance.class));
            FD_FDSTAT_GET = lookup.findVirtual(WasiEnvironment.class, "fdFdstatGet", methodType(int.class, int.class, int.class, ModuleInstance.class));
            FD_FDSTAT_SET_FLAGS = lookup.findVirtual(WasiEnvironment.class, "fdFdstatSetFlags", methodType(int.class, int.class, short.class, ModuleInstance.class));
            FD_FDSTAT_SET_RIGHTS = lookup.findVirtual(WasiEnvironment.class, "fdFdstatSetRights", methodType(int.class, int.class, long.class, long.class, ModuleInstance.class));
            FD_FILESTAT_GET = lookup.findVirtual(WasiEnvironment.class, "fdFilestatGet", methodType(int.class, int.class, int.class, ModuleInstance.class));
        }
        catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new UnsupportedOperationException(exception);
        }
    }

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0);
    private static final ByteBuffer ZEROS_BUFFER = ByteBuffer.allocateDirect(4096);

    private void argsGet(int argvAddress, int argvBufAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        for (var i = 0; i < argOffsets.length; i++) {
            memory.setInt(argvAddress + i*4, argvBufAddress + argOffsets[i]);
        }

        memory.setBytes(argvBufAddress, argBytes);
    }

    private void argsSizesGet(int numArgsAddress, int argDataSizeAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);
        memory.setInt(numArgsAddress, argOffsets.length);
        memory.setInt(argDataSizeAddress, argBytes.length);
    }

    private void environGet(int ptrsAddress, int dataAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        for (var i = 0; i < envOffsets.length; i++) {
            memory.setInt(ptrsAddress + i*4, dataAddress + envOffsets[i]);
        }

        memory.setBytes(dataAddress, envBytes);
    }

    private void environSizesGet(int numVarsAddress, int dataSizeAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);
        memory.setInt(numVarsAddress, envOffsets.length);
        memory.setInt(dataSizeAddress, envBytes.length);
    }

    private void clockResGet(int clockId, int resolutionOut, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);

        if (clockId < 0 || clockId > CLOCKID_THREAD_CPUTIME_ID) {
            throw new ErrnoException(ERRNO_INVAL);
        }

        long resolution;
        try (var frame = MemoryStack.getFrame()) {
            var timespec = frame.allocate(Linux.timespec);
            Linux.clock_getres.invokeExact(clockId, timespec.address());
            resolution = Linux.timespecToNanos(timespec);
        }

        memory.setLong(resolutionOut, resolution);
    }

    private void clockTimeGet(int clockId, long precision, int timeOut, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);

        if (clockId < 0 || clockId > CLOCKID_THREAD_CPUTIME_ID) {
            throw new ErrnoException(ERRNO_INVAL);
        }

        long time;
        try (var frame = MemoryStack.getFrame()) {
            var timespec = frame.allocate(Linux.timespec);
            Linux.clock_gettime.invokeExact(clockId, timespec.address());
            time = Linux.timespecToNanos(timespec);
        }

        memory.setLong(timeOut, time);
    }

    private @NotNull OpenFile lookUpFile(int fd) throws ErrnoException {
        OpenFile file;

        if ((file = fdTable.get(fd)) == null) {
            throw new ErrnoException(ERRNO_BADF);
        }

        return file;
    }

    private void fdAdvise(int fd, long offset, long length, int advice, @NotNull ModuleInstance module) throws Throwable {
        var file = lookUpFile(fd);

        file.requireBaseRights(RIGHTS_FD_ADVISE);

        int linuxAdvice;
        switch (advice) {
            case ADVICE_NORMAL -> {
                linuxAdvice = Linux.POSIX_FADV_NORMAL;
            }

            case ADVICE_SEQUENTIAL -> {
                linuxAdvice = Linux.POSIX_FADV_SEQUENTIAL;
            }

            case ADVICE_RANDOM -> {
                linuxAdvice = Linux.POSIX_FADV_RANDOM;
            }

            case ADVICE_WILLNEED -> {
                linuxAdvice = Linux.POSIX_FADV_WILLNEED;
            }

            case ADVICE_DONTNEED -> {
                linuxAdvice = Linux.POSIX_FADV_DONTNEED;
            }

            case ADVICE_NOREUSE -> {
                linuxAdvice = Linux.POSIX_FADV_NOREUSE;
            }

            default -> {
                throw new ErrnoException(ERRNO_INVAL);
            }
        }

        Linux.posix_fadvise.invokeExact(file.nativeFd(), offset, length, linuxAdvice);
    }

    private void fdAllocate(int fd, long offset, long length, @NotNull ModuleInstance module) throws Throwable {
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_ALLOCATE);
        Linux.posix_fallocate.invokeExact(file.nativeFd(), offset, length);
    }

    private void fdClose(int fd, @NotNull ModuleInstance module) throws Throwable {
        var file = lookUpFile(fd);
        Linux.close.invokeExact(file.nativeFd());
        fdTable.remove(fd);
        retiredFds.addLast(fd);
    }

    private void fdDatasync(int fd, @NotNull ModuleInstance module) throws Throwable {
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_DATASYNC);
        Linux.fdatasync.invokeExact(file.nativeFd());
    }

    private static byte linuxModeToFiletype(@NotNull OpenFile file, int mode) throws Throwable {
        var linuxType = mode & 0xf000;
        byte type;

        switch (linuxType) {
            case Linux.S_IFCHR -> {
                type = FILETYPE_CHARACTER_DEVICE;
            }

            case Linux.S_IFDIR -> {
                type = FILETYPE_DIRECTORY;
            }

            case Linux.S_IFBLK -> {
                type = FILETYPE_BLOCK_DEVICE;
            }

            case Linux.S_IFREG -> {
                type = FILETYPE_REGULAR_FILE;
            }

            case Linux.S_IFLNK -> {
                type = FILETYPE_SYMBOLIC_LINK;
            }

            case Linux.S_IFSOCK -> {
                try (var frame = MemoryStack.getFrame()) {
                    var optval = frame.allocate(MemoryLayouts.JAVA_INT);
                    var optlen = frame.allocate(Linux.socklen_t);
                    MemoryAccess.setInt(optlen, 4);

                    Linux.getsockopt.invokeExact(file.nativeFd(), Linux.SOL_SOCKET, Linux.SO_TYPE, optval.address(), optlen.address());

                    switch (MemoryAccess.getInt(optval)) {
                        case Linux.SOCK_STREAM -> {
                            type = FILETYPE_SOCKET_STREAM;
                        }

                        case Linux.SOCK_DGRAM -> {
                            type = FILETYPE_SOCKET_DGRAM;
                        }

                        default -> {
                            type = FILETYPE_UNKNOWN;
                        }
                    }
                }
            }

            default -> {
                type = FILETYPE_UNKNOWN;
            }
        }

        return type;
    }

    private void fdFdstatGet(int fd, int statAddress, @NotNull ModuleInstance module) throws Throwable {
        var stack = MemoryStack.get();
        var memory = (Memory) memoryHandle.get(module);

        var file = lookUpFile(fd);

        var linuxFlags = (int) Linux.fcntl_GETFL.invokeExact(file.nativeFd());

        short flags = 0;

        if ((linuxFlags & Linux.O_APPEND) != 0) {
            flags |= FDFLAGS_APPEND;
        }

        if ((linuxFlags & Linux.O_SYNC) == Linux.O_SYNC) {
            flags |= FDFLAGS_SYNC | FDFLAGS_RSYNC;
        }

        if ((linuxFlags & Linux.O_DSYNC) != 0) {
            flags |= FDFLAGS_DSYNC;
        }

        if ((linuxFlags & Linux.O_NONBLOCK) != 0) {
            flags |= FDFLAGS_NONBLOCK;
        }

        byte type;
        try (var frame = stack.frame()) {
            var stat = frame.allocate(Linux.stat);
            Linux.fstat.invokeExact(file.nativeFd(), stat.address());
            type = linuxModeToFiletype(file, (int) Linux.st_mode.get(stat));
        }

        memory.setByte(statAddress, type);
        memory.setShort(statAddress + 2, flags);
        memory.setLong(statAddress + 8, file.baseRights());
        memory.setLong(statAddress + 16, file.inheritingRights());
    }

    private void fdFdstatSetFlags(int fd, short newFlags, @NotNull ModuleInstance module) throws Throwable {
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_FDSTAT_SET_FLAGS);

        int linuxFlags = 0;

        if ((newFlags & FDFLAGS_APPEND) != 0) {
            newFlags -= FDFLAGS_APPEND;
            linuxFlags |= Linux.O_APPEND;
        }

        if ((newFlags & FDFLAGS_DSYNC) != 0) {
            newFlags -= FDFLAGS_DSYNC;
            linuxFlags |= Linux.O_DSYNC;
        }

        if ((newFlags & FDFLAGS_NONBLOCK) != 0) {
            newFlags -= FDFLAGS_NONBLOCK;
            linuxFlags |= Linux.O_NONBLOCK;
        }

        if ((newFlags & FDFLAGS_RSYNC) != 0) {
            newFlags -= FDFLAGS_RSYNC;
            linuxFlags |= Linux.O_SYNC;
        }

        if ((newFlags & FDFLAGS_SYNC) != 0) {
            newFlags -= FDFLAGS_SYNC;
            linuxFlags |= Linux.O_SYNC;
        }

        if (newFlags != 0) {
            throw new ErrnoException(ERRNO_INVAL);
        }

        Linux.fcntl_SETFL.invokeExact(file.nativeFd(), linuxFlags);
    }

    private void fdFdstatSetRights(int fd, long newBaseRights, long newInheritingRights, @NotNull ModuleInstance module) throws ErrnoException {
        var file = lookUpFile(fd);
        file.setRights(newBaseRights, newInheritingRights);
    }

    private void fdFilestatGet(int fd, int filestatAddress, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);

        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_FILESTAT_GET);

        long dev, ino, nlink, size, atim, mtim, ctim;
        byte type;

        try (var frame = MemoryStack.getFrame()) {
            var stat = frame.allocate(Linux.stat);

            Linux.fstat.invokeExact(file.nativeFd(), stat.address());

            dev = (long) Linux.st_dev.get(stat);
            ino = (long) Linux.st_ino.get(stat);
            type = linuxModeToFiletype(file, (int) Linux.st_mode.get(stat));
            nlink = (long) Linux.st_nlink.get(stat);
            size = (long) Linux.st_size.get(stat);
            atim = Linux.timespecToNanos((MemorySegment) Linux.st_atim.invokeExact(stat));
            mtim = Linux.timespecToNanos((MemorySegment) Linux.st_mtim.invokeExact(stat));
            ctim = Linux.timespecToNanos((MemorySegment) Linux.st_ctim.invokeExact(stat));
        }

        memory.setLong(filestatAddress, dev);
        memory.setLong(filestatAddress + 8, ino);
        memory.setByte(filestatAddress + 16, type);
        memory.setLong(filestatAddress + 24, nlink);
        memory.setLong(filestatAddress + 32, size);
        memory.setLong(filestatAddress + 40, atim);
        memory.setLong(filestatAddress + 48, mtim);
        memory.setLong(filestatAddress + 56, ctim);
    }

    private void fdFilestatSetSize(int fd, long size, @NotNull ModuleInstance module) throws Throwable {
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_FILESTAT_SET_SIZE);
        Linux.ftruncate.invokeExact(file.nativeFd(), size);
    }

    private void fdFilestatSetTimes(int fd, long newAccessTime, long newModificationTime, short fstFlags, @NotNull ModuleInstance module) throws Throwable {
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_FILESTAT_SET_TIMES);
        try (var frame = MemoryStack.getFrame()) {
            var times = frame.allocate(sequenceLayout(2, Linux.timespec));
            var accessTime = times.asSlice(0, Linux.timespec.byteSize());
            var modificationTime = times.asSlice(Linux.timespec.byteSize());

            if ((fstFlags & FSTFLAGS_ATIM) != 0) {
                fstFlags -= FSTFLAGS_ATIM;

                if ((fstFlags & FSTFLAGS_ATIM_NOW) != 0) {
                    throw new ErrnoException(ERRNO_INVAL);
                }

                Linux.fillTimespec(accessTime, newAccessTime);
            }
            else if ((fstFlags & FSTFLAGS_ATIM_NOW) != 0) {
                fstFlags -= FSTFLAGS_ATIM_NOW;
                Linux.tv_nsec.set(accessTime, Linux.UTIME_NOW);
            }
            else {
                Linux.tv_nsec.set(accessTime, Linux.UTIME_OMIT);
            }

            if ((fstFlags & FSTFLAGS_MTIM) != 0) {
                fstFlags -= FSTFLAGS_MTIM;

                if ((fstFlags & FSTFLAGS_MTIM_NOW) != 0) {
                    throw new ErrnoException(ERRNO_INVAL);
                }

                Linux.fillTimespec(modificationTime, newModificationTime);
            }
            else if ((fstFlags & FSTFLAGS_MTIM_NOW) != 0) {
                fstFlags -= FSTFLAGS_MTIM_NOW;
                Linux.tv_nsec.set(modificationTime, Linux.UTIME_NOW);
            }
            else {
                Linux.tv_nsec.set(modificationTime, Linux.UTIME_OMIT);
            }

            if (fstFlags != 0) {
                throw new ErrnoException(ERRNO_INVAL);
            }

            Linux.futimens.invokeExact(file.nativeFd(), times.address());
        }
    }

    private static @NotNull MemorySegment makeNativeIovecs(@NotNull MemorySegment segment, int iovsAddress, int iovsCount, @NotNull SegmentAllocator allocator) throws ErrnoException {
        var nativeVecs = allocator.allocate(multiplyExact(Linux.iovec.byteSize(), Integer.toUnsignedLong(iovsCount)), Linux.iovec.byteAlignment());
        var totalLen = 0L;

        for (var i = 0; i != iovsCount; i++) {
            var iovOffset = addExact(iovsAddress, multiplyExact(Integer.toUnsignedLong(i), 8));
            var buf = (int) Memory.VH_INT.get(segment, iovOffset);
            var len = (int) Memory.VH_INT.get(segment, addExact(iovOffset, 4));

            totalLen += Integer.toUnsignedLong(len);
            if (totalLen > 0xFFFF_FFFFL) {
                throw new ErrnoException(ERRNO_INVAL);
            }

            var bufSlice = segment.asSlice(Integer.toUnsignedLong(buf), Integer.toUnsignedLong(len));
            var nativeIov = nativeVecs.asSlice(Integer.toUnsignedLong(i) * Linux.iovec.byteSize(), Linux.iovec.byteSize());
            Linux.iov_base.set(nativeIov, bufSlice.address());
            Linux.iov_len.set(nativeIov, Integer.toUnsignedLong(len));
        }

        return nativeVecs;
    }

    private void fdPread(int fd, int iovsAddress, int iovsCount, long offset, int bytesReadAddress, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_READ);

        long bytesRead;
        if (iovsCount == 0) {
            bytesRead = 0;
        }
        else if (iovsCount == 1) {
            var buf = memory.getInt(iovsAddress);
            var bufLen = memory.getInt(iovsAddress + 4);
            var bufSlice = memory.currentSegment().asSlice(buf, bufLen);
            bytesRead = (long) Linux.pread.invokeExact(file.nativeFd(), bufSlice.address(), bufSlice.byteSize(), offset);
        }
        else {
            var segment = memory.currentSegment();
            try (var frame = MemoryStack.getFrame()) {
                var nativeIovs = makeNativeIovecs(segment, iovsAddress, iovsCount, frame);
                bytesRead = (long) Linux.preadv.invokeExact(file.nativeFd(), nativeIovs.address(), iovsCount, offset);
            } finally {
                reachabilityFence(segment.scope());
            }
        }

        memory.setInt(bytesReadAddress, (int) bytesRead);
    }

    private void fdPrestatGet(int fd, int prestatAddress, @NotNull ModuleInstance module) throws ErrnoException {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);

        if (file.prestatDirName() == null) {
            throw new ErrnoException(ERRNO_BADF);
        }

        memory.setByte(prestatAddress, PREOPENTYPE_DIR);
        memory.setInt(prestatAddress + 4, file.prestatDirName().length);
    }

    private void fdPrestatDirName(int fd, int outAddress, int outLength, @NotNull ModuleInstance module) throws ErrnoException {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);

        if (file.prestatDirName() == null) {
            throw new ErrnoException(ERRNO_BADF);
        }

        if (outLength != file.prestatDirName().length) {
            throw new ErrnoException(ERRNO_INVAL);
        }

        memory.setBytes(outAddress, file.prestatDirName());
    }

    private void fdPwrite(int fd, int iovsAddress, int iovsCount, long offset, int bytesWrittenAddress, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_WRITE);

        long bytesRead;
        if (iovsCount == 0) {
            bytesRead = 0;
        }
        else if (iovsCount == 1) {
            var buf = memory.getInt(iovsAddress);
            var bufLen = memory.getInt(iovsAddress + 4);
            var bufSlice = memory.currentSegment().asSlice(buf, bufLen);
            bytesRead = (long) Linux.pwrite.invokeExact(file.nativeFd(), bufSlice.address(), bufSlice.byteSize(), offset);
        }
        else {
            var segment = memory.currentSegment();
            try (var frame = MemoryStack.getFrame()) {
                var nativeIovs = makeNativeIovecs(segment, iovsAddress, iovsCount, frame);
                bytesRead = (long) Linux.pwritev.invokeExact(file.nativeFd(), nativeIovs.address(), iovsCount, offset);
            } finally {
                reachabilityFence(segment.scope());
            }
        }

        memory.setInt(bytesWrittenAddress, (int) bytesRead);
    }

    private void fdRead(int fd, int iovsAddress, int iovsCount, int bytesReadAddress, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_READ);

        long bytesRead;
        if (iovsCount == 0) {
            bytesRead = 0;
        }
        else if (iovsCount == 1) {
            var buf = memory.getInt(iovsAddress);
            var bufLen = memory.getInt(iovsAddress + 4);
            var bufSlice = memory.currentSegment().asSlice(buf, bufLen);
            bytesRead = (long) Linux.read.invokeExact(file.nativeFd(), bufSlice.address(), bufSlice.byteSize());
        }
        else {
            var segment = memory.currentSegment();
            try (var frame = MemoryStack.getFrame()) {
                var nativeIovs = makeNativeIovecs(segment, iovsAddress, iovsCount, frame);
                bytesRead = (long) Linux.readv.invokeExact(file.nativeFd(), nativeIovs.address(), iovsCount);
            } finally {
                reachabilityFence(segment.scope());
            }
        }

        memory.setInt(bytesReadAddress, (int) bytesRead);
    }

    private void fdReaddir(int fd, int buf, int bufLen, long cookie, int sizeAddress, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);
        var file = lookUpFile(fd);
        file.requireBaseRights(RIGHTS_FD_READDIR);

        MemoryAddress dir;
        try (var frame = MemoryStack.getFrame()) {
            var opendirBytes = ("/proc/self/fd/" + Integer.toUnsignedString(file.nativeFd()) + "\0").getBytes(StandardCharsets.UTF_8);
            var opendirArg = frame.allocateArray(MemoryLayouts.JAVA_BYTE, opendirBytes);
            dir = (MemoryAddress) Linux.opendir.invokeExact(opendirArg.address());
        }

        var offset = 0;
        try {
            if (cookie != 0) {
                Linux.seekdir.invokeExact(dir, cookie);
            }

            while (bufLen - offset >= 24) {
                var entryAddress = (MemoryAddress) Linux.readdir.invokeExact(dir);

                if (entryAddress.equals(MemoryAddress.NULL)) {
                    memory.setInt(sizeAddress, offset);
                    return;
                }

                var entry = entryAddress.asSegment(Linux.dirent.byteSize(), ResourceScope.globalScope());
                var ino = (long) Linux.d_ino.get(entry);
                var off = (long) Linux.d_off.get(entry);
                var linuxType = (byte) Linux.d_type.get(entry);

                memory.setLong(offset, off);
                memory.setLong(offset + 8, ino);

                var type = switch (linuxType) {
                    case Linux.DT_CHR -> FILETYPE_CHARACTER_DEVICE;
                    case Linux.DT_DIR -> FILETYPE_DIRECTORY;
                    case Linux.DT_BLK -> FILETYPE_BLOCK_DEVICE;
                    case Linux.DT_REG -> FILETYPE_REGULAR_FILE;
                    case Linux.DT_LNK -> FILETYPE_SYMBOLIC_LINK;
                    default -> FILETYPE_UNKNOWN;
                };

                var nameAddress = entryAddress.addOffset(Linux.d_name_offset);
                var nameLength = (long) Linux.strlen.invokeExact(nameAddress);
                var nameSlice = nameAddress.asSegment(nameLength, ResourceScope.globalScope());
            }

            memory.setInt(sizeAddress, bufLen);
        }
        finally {
            Linux.closedir.invokeExact(dir);
        }
    }

    private static byte detectFiletype(@NotNull BasicFileAttributes attributes) {
        if (attributes.isSymbolicLink()) {
            return FILETYPE_SYMBOLIC_LINK;
        }
        else if (attributes.isDirectory()) {
            return FILETYPE_DIRECTORY;
        }
        else if (attributes.isRegularFile()) {
            return FILETYPE_REGULAR_FILE;
        }
        else {
            return FILETYPE_UNKNOWN;
        }
    }

    private int fdRenumber(int fdFrom, int fdTo, @NotNull ModuleInstance module) {
        OpenFile from;
        if ((from = openFile(fdFrom)) == null) {
            return ERRNO_BADF;
        }

        OpenFile to;
        if ((to = openFile(fdTo)) == null) {
            return ERRNO_BADF;
        }

        if (to.channel != null) {
            try {
                to.channel.close();
            }
            catch (IOException exception) {
                return ERRNO_IO;
            }
        }

        fileDescriptorTable.set(fdFrom, null);
        fileDescriptorTable.set(fdTo, from);
        recycledFileDescriptors.addLast(fdFrom);

        return ERRNO_SUCCESS;
    }

    private int fdSeek(int fd, long delta, byte whence, int newOffsetAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_SEEK) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof FileChannel channel)) {
            return ERRNO_NODEV;
        }

        long newPosition;

        try {
            switch (whence) {
                case WHENCE_SET -> {
                    newPosition = delta;
                }

                case WHENCE_CUR -> {
                    newPosition = channel.position() + delta;
                }

                case WHENCE_END -> {
                    newPosition = channel.size() + delta;
                }

                default -> {
                    return ERRNO_INVAL;
                }
            }

            channel.position(newPosition);
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        memory.setLong(newOffsetAddress, newPosition);

        return ERRNO_SUCCESS;
    }

    private int fdSync(int fd, @NotNull ModuleInstance module) {
        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_SYNC) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof FileChannel channel)) {
            return ERRNO_NODEV;
        }

        try {
            channel.force(true);
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        return ERRNO_SUCCESS;
    }

    private int fdTell(int fd, int resultAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_TELL) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof FileChannel channel)) {
            return ERRNO_NODEV;
        }

        long result;

        try {
            result = channel.position();
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        memory.setLong(resultAddress, result);
        return ERRNO_SUCCESS;
    }

    private int fdWrite(int fd, int iovsAddress, int iovsCount, int bytesWrittenAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_WRITE) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof GatheringByteChannel channel)) {
            return ERRNO_NODEV;
        }

        if (iovsCount == 0) {
            memory.setInt(bytesWrittenAddress, 0);
            return ERRNO_SUCCESS;
        }

        var buffers = new ByteBuffer[iovsCount];
        var totalLen = 0L;

        for (var i = 0; i < iovsCount; i++) {
            var ptr = memory.getInt(iovsAddress + 8*i);
            var len = memory.getInt(iovsAddress + 8*i + 4);

            if (len < 0) {
                return ERRNO_INVAL;
            }

            totalLen += len;
            if (totalLen > Integer.toUnsignedLong(-1)) {
                return ERRNO_INVAL;
            }

            try {
                buffers[i] = memory.segment().asSlice(ptr, len).asByteBuffer();
            }
            catch (IndexOutOfBoundsException exception) {
                return ERRNO_FAULT;
            }
        }

        int bytesWritten;
        try {
            bytesWritten = (int) Long.max(0, channel.write(buffers));
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        memory.setInt(bytesWrittenAddress, bytesWritten);
        return ERRNO_SUCCESS;
    }

    private int pathCreateDirectory(int fd, int pathPtr, int pathLen, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_PATH_CREATE_DIRECTORY) == 0) {
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

        try {
            Files.createDirectory(file.path.resolve(path));
        }
        catch (FileAlreadyExistsException ignored) {
            return ERRNO_EXIST;
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        return ERRNO_SUCCESS;
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

    private static int translateErrno(int linuxValue) {
        return switch (linuxValue) {
            case 0 -> ERRNO_SUCCESS;
            case Linux.EPERM -> ERRNO_PERM;
            case Linux.ENOENT -> ERRNO_NOENT;
            case Linux.ESRCH -> ERRNO_SRCH;
            case Linux.EINTR -> ERRNO_INTR;
            case Linux.EIO -> ERRNO_IO;
            case Linux.ENXIO -> ERRNO_NXIO;
            case Linux.E2BIG -> ERRNO_2BIG;
            case Linux.ENOEXEC -> ERRNO_NOEXEC;
            case Linux.EBADF -> ERRNO_BADF;
            case Linux.ECHILD -> ERRNO_CHILD;
            case Linux.EAGAIN -> ERRNO_AGAIN;
            case Linux.ENOMEM -> ERRNO_NOMEM;
            case Linux.EACCES -> ERRNO_ACCES;
            case Linux.EFAULT -> ERRNO_FAULT;
            case Linux.EBUSY -> ERRNO_BUSY;
            case Linux.EEXIST -> ERRNO_EXIST;
            case Linux.EXDEV -> ERRNO_XDEV;
            case Linux.ENODEV -> ERRNO_NODEV;
            case Linux.ENOTDIR -> ERRNO_NOTDIR;
            case Linux.EISDIR -> ERRNO_ISDIR;
            case Linux.EINVAL -> ERRNO_INVAL;
            case Linux.ENFILE -> ERRNO_NFILE;
            case Linux.EMFILE -> ERRNO_MFILE;
            case Linux.ENOTTY -> ERRNO_NOTTY;
            case Linux.ETXTBSY -> ERRNO_TXTBSY;
            case Linux.EFBIG -> ERRNO_FBIG;
            case Linux.ENOSPC -> ERRNO_NOSPC;
            case Linux.ESPIPE -> ERRNO_SPIPE;
            case Linux.EROFS -> ERRNO_ROFS;
            case Linux.EMLINK -> ERRNO_MLINK;
            case Linux.EPIPE -> ERRNO_PIPE;
            case Linux.EDOM -> ERRNO_DOM;
            case Linux.ERANGE -> ERRNO_RANGE;
            case Linux.EDEADLK -> ERRNO_DEADLK;
            case Linux.ENAMETOOLONG -> ERRNO_NAMETOOLONG;
            case Linux.ENOLCK -> ERRNO_NOLCK;
            case Linux.ENOSYS -> ERRNO_NOSYS;
            case Linux.ENOTEMPTY -> ERRNO_NOTEMPTY;
            case Linux.ELOOP -> ERRNO_LOOP;
            case Linux.ENOMSG -> ERRNO_NOMSG;
            case Linux.EIDRM -> ERRNO_IDRM;
            case Linux.ENOLINK -> ERRNO_NOLINK;
            case Linux.EPROTO -> ERRNO_PROTO;
            case Linux.EMULTIHOP -> ERRNO_MULTIHOP;
            case Linux.EBADMSG -> ERRNO_BADMSG;
            case Linux.EOVERFLOW -> ERRNO_OVERFLOW;
            case Linux.EILSEQ -> ERRNO_ILSEQ;
            case Linux.ENOTSOCK -> ERRNO_NOTSOCK;
            case Linux.EDESTADDRREQ -> ERRNO_DESTADDRREQ;
            case Linux.EMSGSIZE -> ERRNO_MSGSIZE;
            case Linux.EPROTOTYPE -> ERRNO_PROTOTYPE;
            case Linux.ENOPROTOOPT -> ERRNO_NOPROTOOPT;
            case Linux.EPROTONOSUPPORT -> ERRNO_PROTONOSUPPORT;
            case Linux.ENOTSUP -> ERRNO_NOTSUP;
            case Linux.EAFNOSUPPORT -> ERRNO_AFNOSUPPORT;
            case Linux.EADDRINUSE -> ERRNO_ADDRINUSE;
            case Linux.EADDRNOTAVAIL -> ERRNO_ADDRNOTAVAIL;
            case Linux.ENETDOWN -> ERRNO_NETDOWN;
            case Linux.ENETUNREACH -> ERRNO_NETUNREACH;
            case Linux.ENETRESET -> ERRNO_NETRESET;
            case Linux.ECONNABORTED -> ERRNO_CONNABORTED;
            case Linux.ECONNRESET -> ERRNO_CONNRESET;
            case Linux.ENOBUFS -> ERRNO_NOBUFS;
            case Linux.EISCONN -> ERRNO_ISCONN;
            case Linux.ENOTCONN -> ERRNO_NOTCONN;
            case Linux.ETIMEDOUT -> ERRNO_TIMEDOUT;
            case Linux.ECONNREFUSED -> ERRNO_CONNREFUSED;
            case Linux.EHOSTUNREACH -> ERRNO_HOSTUNREACH;
            case Linux.EALREADY -> ERRNO_ALREADY;
            case Linux.EINPROGRESS -> ERRNO_INPROGRESS;
            case Linux.ESTALE -> ERRNO_STALE;
            case Linux.EDQUOT -> ERRNO_DQUOT;
            case Linux.ECANCELED -> ERRNO_CANCELED;
            case Linux.EOWNERDEAD -> ERRNO_OWNERDEAD;
            case Linux.ENOTRECOVERABLE -> ERRNO_NOTRECOVERABLE;
            default -> ERRNO_IO;
        };
    }
}
