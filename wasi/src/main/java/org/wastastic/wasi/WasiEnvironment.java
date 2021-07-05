package org.wastastic.wasi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.wastastic.Memory;
import org.wastastic.ModuleInstance;
import org.wastastic.QualifiedName;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.System.arraycopy;
import static java.lang.invoke.MethodType.methodType;
import static java.nio.file.Files.readAttributes;
import static java.nio.file.Files.readSymbolicLink;
import static java.util.Objects.requireNonNull;

public final class WasiEnvironment {
    private final @NotNull VarHandle memoryHandle;
    private final int @NotNull[] argOffsets;
    private final byte @NotNull[] argBytes;

    private final ArrayList<OpenFile> fileDescriptorTable = new ArrayList<>();
    private final ArrayDeque<Integer> recycledFileDescriptors = new ArrayDeque<>();

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

    private @Nullable OpenFile openFile(int fd) {
        if (fd >= fileDescriptorTable.size()) {
            return null;
        }

        return fileDescriptorTable.get(fd);
    }

    private int argsGet(int argvAddress, int argvBufAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        for (var i = 0; i < argOffsets.length; i++) {
            memory.setInt(argvAddress + i*4, argvBufAddress + argOffsets[i]);
        }

        memory.setBytes(argvBufAddress, argBytes);
        return ERRNO_SUCCESS;
    }

    private int argsSizesGet(int numArgsAddress, int argDataSizeAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);
        memory.setInt(numArgsAddress, argOffsets.length);
        memory.setInt(argDataSizeAddress, argBytes.length);
        return ERRNO_SUCCESS;
    }

    private int environGet(int ptrsAddress, int dataAddress, @NotNull ModuleInstance module) {
        // TODO
        return ERRNO_NOTSUP;
    }

    private int environSizesGet(int numVarsAddress, int dataSizeAddress, @NotNull ModuleInstance module) {
        // TODO
        return ERRNO_NOTSUP;
    }

    private int clockResGet(int clockId, int resolutionOut, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);
        long resolution;

        switch (clockId) {
            case CLOCKID_REALTIME -> {
                resolution = 1_000_000L;
            }

            case CLOCKID_MONOTONIC -> {
                resolution = 1L;
            }

            default -> {
                return ERRNO_INVAL;
            }
        }

        memory.setLong(resolutionOut, resolution);
        return ERRNO_SUCCESS;
    }

    private int clockTimeGet(int clockId, long precision, int timeOut, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);
        long time;

        switch (clockId) {
            case CLOCKID_REALTIME -> {
                time = System.currentTimeMillis() * 1_000_000L;
            }

            case CLOCKID_MONOTONIC -> {
                time = System.nanoTime();
            }

            default -> {
                return ERRNO_INVAL;
            }
        }

        memory.setLong(timeOut, time);
        return ERRNO_SUCCESS;
    }

    private int fdAdvise(int fd, long offset, long length, int advice, @NotNull ModuleInstance module) {
        if (openFile(fd) == null) {
            return ERRNO_BADF;
        }

        return switch (advice) {
            case ADVICE_NORMAL,
                ADVICE_SEQUENTIAL,
                ADVICE_RANDOM,
                ADVICE_WILLNEED,
                ADVICE_DONTNEED,
                ADVICE_NOREUSE -> ERRNO_SUCCESS;

            default -> ERRNO_INVAL;
        };
    }

    private int fdAllocate(int fd, long offset, long length, @NotNull ModuleInstance module) {
        OpenFile file;

        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if (!(file.channel instanceof FileChannel channel)) {
            return ERRNO_NODEV;
        }

        if (offset < 0 || length <= 0 || offset + length < 0) {
            return ERRNO_INVAL;
        }

        return ERRNO_NOTSUP;
    }

    private int fdClose(int fd, @NotNull ModuleInstance module) {
        OpenFile file;

        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        try {
            file.channel.close();
        }
        catch (IOException exception) {
            return ERRNO_IO;
        }

        fileDescriptorTable.set(fd, null);
        recycledFileDescriptors.addLast(fd);

        return ERRNO_SUCCESS;
    }

    private int fdDatasync(int fd, @NotNull ModuleInstance module) {
        OpenFile file;

        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if (!(file.channel instanceof FileChannel channel)) {
            return ERRNO_INVAL;
        }

        try {
            channel.force(true);
        }
        catch (IOException exception) {
            return ERRNO_IO;
        }

        return ERRNO_SUCCESS;
    }

    private int fdFdstatGet(int fd, int statAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        memory.setByte(statAddress, file.type);
        memory.setShort(statAddress + 2, file.flags);
        memory.setLong(statAddress + 8, file.baseRights);
        memory.setLong(statAddress + 16, file.inheritingRights);

        return ERRNO_SUCCESS;
    }

    private int fdFdstatSetFlags(int fd, short newFlags, @NotNull ModuleInstance module) {
        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if (newFlags == file.flags) {
            return ERRNO_SUCCESS;
        }

        var options = new ArrayList<OpenOption>();
        options.add(LinkOption.NOFOLLOW_LINKS);

        if ((file.baseRights & RIGHTS_FD_READ) != 0) {
            options.add(StandardOpenOption.READ);
        }

        if ((file.baseRights & RIGHTS_FD_WRITE) != 0) {
            options.add(StandardOpenOption.WRITE);
        }

        if ((newFlags & FDFLAGS_APPEND) != 0) {
            options.add(StandardOpenOption.APPEND);
        }

        if ((newFlags & FDFLAGS_DSYNC) != 0) {
            options.add(StandardOpenOption.DSYNC);
        }

        if ((newFlags & FDFLAGS_NONBLOCK) != 0) {
            return ERRNO_NOTSUP;
        }

        if ((newFlags & FDFLAGS_RSYNC) != 0) {
            return ERRNO_NOTSUP;
        }

        if ((newFlags & FDFLAGS_SYNC) != 0) {
            options.add(StandardOpenOption.SYNC);
        }

        try {
            file.channel.close();
            file.channel = FileChannel.open(file.path, options.toArray(OpenOption[]::new));
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        return ERRNO_SUCCESS;
    }

    private int fdFdstatSetRights(int fd, long newBaseRights, long newInheritingRights, @NotNull ModuleInstance module) {
        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & newBaseRights) != newBaseRights || (file.inheritingRights & newInheritingRights) != newInheritingRights) {
            return ERRNO_NOTCAPABLE;
        }

        file.baseRights = newBaseRights;
        file.inheritingRights = newInheritingRights;

        return ERRNO_SUCCESS;
    }

    private int fdFilestatGet(int fd, int filestatAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        BasicFileAttributes attributes;
        long size;

        try {
            attributes = readAttributes(file.path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            if (file.type == FILETYPE_SYMBOLIC_LINK) {
                size = readSymbolicLink(file.path).toString().getBytes(StandardCharsets.UTF_8).length;
            }
            else {
                size = attributes.size();
            }
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        memory.setLong(filestatAddress, 0); // 'dev', not implemented
        memory.setLong(filestatAddress + 8, 0); // 'ino', not implemented
        memory.setByte(filestatAddress + 16, file.type);
        memory.setLong(filestatAddress + 24, 0); // 'nlink', not implemented
        memory.setLong(filestatAddress + 32, size);
        memory.setLong(filestatAddress + 40, attributes.lastAccessTime().toMillis() * 1_000_000);
        memory.setLong(filestatAddress + 48, attributes.lastModifiedTime().toMillis() * 1_000_000);
        memory.setLong(filestatAddress + 56, attributes.lastModifiedTime().toMillis() * 1_000_000);

        return ERRNO_SUCCESS;
    }

    // private int fdFilestatSetSize(int fd, long size, @NotNull ModuleInstance module) {
    //     OpenFile file;
    //     if ((file = openFile(fd)) == null) {
    //         return ERRNO_BADF;
    //     }

    //     if (!(file.channel instanceof FileChannel channel)) {
    //         return ERRNO_NODEV;
    //     }
    // }

    private static final class OpenFile {
        private Path path;
        private Channel channel;
        private byte type;
        private short flags;
        private long baseRights;
        private long inheritingRights;
    }

    private static final int PREOPENTYPE_DIR = 0;

    private static final int SDFLAGS_RD = 1;
    private static final int SDFLAGS_WR = 1 << 1;

    private static final int ROFLAGS_RECV_DATA_TRUNCATED = 1;

    private static final int RIFLAGS_RECV_PEEK = 1;
    private static final int RIFLAGS_RECV_WAITALL = 1 << 1;

    private static final int SIGNAL_NONE = 0;
    private static final int SIGNAL_HUP = 1;
    private static final int SIGNAL_INT = 2;
    private static final int SIGNAL_QUIT = 3;
    private static final int SIGNAL_ILL = 4;
    private static final int SIGNAL_TRAP = 5;
    private static final int SIGNAL_ABRT = 6;
    private static final int SIGNAL_BUS = 7;
    private static final int SIGNAL_FPE = 8;
    private static final int SIGNAL_KILL = 9;
    private static final int SIGNAL_USR1 = 10;
    private static final int SIGNAL_SEGV = 11;
    private static final int SIGNAL_USR2 = 12;
    private static final int SIGNAL_PIPE = 13;
    private static final int SIGNAL_ALRM = 14;
    private static final int SIGNAL_TERM = 15;
    private static final int SIGNAL_CHLD = 16;
    private static final int SIGNAL_CONT = 17;
    private static final int SIGNAL_STOP = 18;
    private static final int SIGNAL_TSTP = 19;
    private static final int SIGNAL_TTIN = 20;
    private static final int SIGNAL_TTOU = 21;
    private static final int SIGNAL_URG = 22;
    private static final int SIGNAL_XCPU = 23;
    private static final int SIGNAL_XFSZ = 24;
    private static final int SIGNAL_VTALRM = 25;
    private static final int SIGNAL_PROF = 26;
    private static final int SIGNAL_WINCH = 27;
    private static final int SIGNAL_POLL = 28;
    private static final int SIGNAL_PWR = 29;
    private static final int SIGNAL_SYS = 30;

    private static final int SUBCLOCK_FLAGS_SUBSCRIPTION_CLOCK_ABSTIME = 1;

    private static final int EVENTRWFLAGS_FD_READWRITE_HANGUP = 1;

    private static final int EVENTTYPE_CLOCK = 1;
    private static final int EVENTTYPE_FD_READ = 1 << 1;
    private static final int EVENTTYPE_FD_WRITE = 1 << 2;

    private static final int OFLAGS_CREAT = 1;
    private static final int OFLAGS_DIRECTORY = 1 << 1;
    private static final int OFLAGS_EXCL = 1 << 2;
    private static final int OFLAGS_TRUNC = 1 << 3;

    private static final int LOOKUPFLAGS_SYMLINK_FOLLOW = 1;

    private static final int FSTFLAGS_ATIM = 1;
    private static final int FSTFLAGS_ATIM_NOW = 1 << 1;
    private static final int FSTFLAGS_MTIM = 1 << 2;
    private static final int FSTFLAGS_MTIM_NOW = 1 << 3;

    private static final int FDFLAGS_APPEND = 1;
    private static final int FDFLAGS_DSYNC = 1 << 1;
    private static final int FDFLAGS_NONBLOCK = 1 << 2;
    private static final int FDFLAGS_RSYNC = 1 << 3;
    private static final int FDFLAGS_SYNC = 1 << 4;

    private static final int FILETYPE_UNKNOWN = 0;
    private static final int FILETYPE_BLOCK_DEVICE = 0;
    private static final int FILETYPE_CHARACTER_DEVICE = 0;
    private static final int FILETYPE_DIRECTORY = 0;
    private static final int FILETYPE_REGULAR_FILE = 0;
    private static final int FILETYPE_SOCKET_DGRAM = 0;
    private static final int FILETYPE_SOCKET_STREAM = 0;
    private static final int FILETYPE_SYMBOLIC_LINK = 0;

    private static final int WHENCE_SET = 0;
    private static final int WHENCE_CUR = 1;
    private static final int WHENCE_END = 2;

    private static final long RIGHTS_FD_DATASYNC = 1;
    private static final long RIGHTS_FD_READ = 1 << 1;
    private static final long RIGHTS_FD_SEEK = 1 << 2;
    private static final long RIGHTS_FD_FDSTAT_SET_FLAGS = 1 << 3;
    private static final long RIGHTS_FD_SYNC = 1 << 4;
    private static final long RIGHTS_FD_TELL = 1 << 5;
    private static final long RIGHTS_FD_WRITE = 1 << 6;
    private static final long RIGHTS_FD_ADVISE = 1 << 7;
    private static final long RIGHTS_FD_ALLOCATE = 1 << 8;
    private static final long RIGHTS_PATH_CREATE_DIRECTORY = 1 << 9;
    private static final long RIGHTS_PATH_CREATE_FILE = 1 << 10;
    private static final long RIGHTS_PATH_LINK_SOURCE = 1 << 11;
    private static final long RIGHTS_PATH_LINK_TARGET = 1 << 12;
    private static final long RIGHTS_PATH_OPEN = 1 << 13;
    private static final long RIGHTS_FD_READDIR = 1 << 14;
    private static final long RIGHTS_PATH_READLINK = 1 << 15;
    private static final long RIGHTS_PATH_RENAME_SOURCE = 1 << 16;
    private static final long RIGHTS_PATH_RENAME_TARGET = 1 << 17;
    private static final long RIGHTS_PATH_FILESTAT_GET = 1 << 18;
    private static final long RIGHTS_PATH_FILESTAT_SET_SIZE = 1 << 19;
    private static final long RIGHTS_PATH_FILESTAT_SET_TIMES = 1 << 20;
    private static final long RIGHTS_FD_FILESTAT_GET = 1 << 21;
    private static final long RIGHTS_FD_FILESTAT_SET_SIZE = 1 << 22;
    private static final long RIGHTS_FD_FILESTAT_SET_TIMES = 1 << 23;
    private static final long RIGHTS_PATH_SYMLINK = 1 << 24;
    private static final long RIGHTS_PATH_REMOVE_DIRECTORY = 1 << 25;
    private static final long RIGHTS_PATH_UNLINK_FILE = 1 << 26;
    private static final long RIGHTS_POLL_FD_READWRITE = 1 << 27;
    private static final long RIGHTS_SOCK_SHUTDOWN = 1 << 28;

    private static final int ADVICE_NORMAL = 0;
    private static final int ADVICE_SEQUENTIAL = 1;
    private static final int ADVICE_RANDOM = 2;
    private static final int ADVICE_WILLNEED = 3;
    private static final int ADVICE_DONTNEED = 4;
    private static final int ADVICE_NOREUSE = 5;

    private static final int CLOCKID_REALTIME = 0;
    private static final int CLOCKID_MONOTONIC = 1;
    private static final int CLOCKID_PROCESS_CPUTIME_ID = 2;
    private static final int CLOCKID_THREAD_CPUTIME_ID = 3;

    private static final int ERRNO_SUCCESS = 0;
    private static final int ERRNO_2BIG = 1;
    private static final int ERRNO_ACCES = 2;
    private static final int ERRNO_ADDRINUSE = 3;
    private static final int ERRNO_ADDRNOTAVAIL = 4;
    private static final int ERRNO_AFNOSUPPORT = 5;
    private static final int ERRNO_AGAIN = 6;
    private static final int ERRNO_ALREADY = 7;
    private static final int ERRNO_BADF = 8;
    private static final int ERRNO_BADMSG = 9;
    private static final int ERRNO_BUSY = 10;
    private static final int ERRNO_CANCELED = 11;
    private static final int ERRNO_CHILD = 12;
    private static final int ERRNO_CONNABORTED = 13;
    private static final int ERRNO_CONNREFUSED = 14;
    private static final int ERRNO_CONNRESET = 15;
    private static final int ERRNO_DEADLK = 16;
    private static final int ERRNO_DESTADDRREQ = 17;
    private static final int ERRNO_DOM = 18;
    private static final int ERRNO_DQUOT = 19;
    private static final int ERRNO_EXIST = 20;
    private static final int ERRNO_FAULT = 21;
    private static final int ERRNO_FBIG = 22;
    private static final int ERRNO_HOSTUNREACH = 23;
    private static final int ERRNO_IDRM = 24;
    private static final int ERRNO_ILSEQ = 25;
    private static final int ERRNO_INPROGRESS = 26;
    private static final int ERRNO_INTR = 27;
    private static final int ERRNO_INVAL = 28;
    private static final int ERRNO_IO = 29;
    private static final int ERRNO_ISCONN = 30;
    private static final int ERRNO_ISDIR = 31;
    private static final int ERRNO_LOOP = 32;
    private static final int ERRNO_MFILE = 33;
    private static final int ERRNO_MLINK = 34;
    private static final int ERRNO_MSGSIZE = 35;
    private static final int ERRNO_MULTIHOP = 36;
    private static final int ERRNO_NAMETOOLONG = 37;
    private static final int ERRNO_NETDOWN = 38;
    private static final int ERRNO_NETRESET = 39;
    private static final int ERRNO_NETUNREACH = 40;
    private static final int ERRNO_NFILE = 41;
    private static final int ERRNO_NOBUFS = 42;
    private static final int ERRNO_NODEV = 43;
    private static final int ERRNO_NOENT = 44;
    private static final int ERRNO_NOEXEC = 45;
    private static final int ERRNO_NOLCK = 46;
    private static final int ERRNO_NOLINK = 47;
    private static final int ERRNO_NOMEM = 48;
    private static final int ERRNO_NOMSG = 49;
    private static final int ERRNO_NOPROTOOPT = 50;
    private static final int ERRNO_NOSPC = 51;
    private static final int ERRNO_NOSYS = 52;
    private static final int ERRNO_NOTCONN = 53;
    private static final int ERRNO_NOTDIR = 54;
    private static final int ERRNO_NOTEMPTY = 55;
    private static final int ERRNO_NOTRECOVERABLE = 56;
    private static final int ERRNO_NOTSOCK = 57;
    private static final int ERRNO_NOTSUP = 58;
    private static final int ERRNO_NOTTY = 59;
    private static final int ERRNO_NXIO = 60;
    private static final int ERRNO_OVERFLOW = 61;
    private static final int ERRNO_OWNERDEAD = 62;
    private static final int ERRNO_PERM = 63;
    private static final int ERRNO_PIPE = 64;
    private static final int ERRNO_PROTO = 65;
    private static final int ERRNO_PROTONOSUPPORT = 66;
    private static final int ERRNO_PROTOTYPE = 67;
    private static final int ERRNO_RANGE = 68;
    private static final int ERRNO_ROFS = 69;
    private static final int ERRNO_SPIPE = 70;
    private static final int ERRNO_SRCH = 71;
    private static final int ERRNO_STALE = 72;
    private static final int ERRNO_TIMEDOUT = 73;
    private static final int ERRNO_TXTBUSY = 74;
    private static final int ERRNO_XDEV = 75;
    private static final int ERRNO_NOTCAPABLE = 76;
}
