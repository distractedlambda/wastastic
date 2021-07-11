package org.wastastic.wasi;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;
import org.wastastic.Memory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodType.methodType;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import static jdk.incubator.foreign.CLinker.asVarArg;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static jdk.incubator.foreign.MemoryLayout.PathElement.sequenceElement;
import static jdk.incubator.foreign.MemoryLayout.paddingLayout;
import static jdk.incubator.foreign.MemoryLayout.sequenceLayout;
import static org.wastastic.wasi.LayoutUtils.cStructLayout;
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

final class Linux {
    private Linux() {}

    static final MemoryLayout time_t = C_LONG;
    static final MemoryLayout off_t = C_LONG;
    static final MemoryLayout dev_t = C_LONG;
    static final MemoryLayout ino_t = C_LONG;
    static final MemoryLayout mode_t = C_INT;
    static final MemoryLayout nlink_t = C_LONG;
    static final MemoryLayout uid_t = C_INT;
    static final MemoryLayout gid_t = C_INT;
    static final MemoryLayout blksize_t = C_LONG;
    static final MemoryLayout blkcnt_t = C_LONG;
    static final MemoryLayout socklen_t = C_INT;
    static final MemoryLayout size_t = C_LONG;
    static final MemoryLayout ssize_t = C_LONG;

    static final MemoryLayout timespec = cStructLayout(
        time_t.withName("sec"),
        C_LONG.withName("nsec")
    );

    static final VarHandle tv_sec = timespec.varHandle(long.class, groupElement("sec"));
    static final VarHandle tv_nsec = timespec.varHandle(long.class, groupElement("nsec"));

    // FIXME: support non-x86_64
    // FIXME: double-check size on this mf
    static final MemoryLayout stat = cStructLayout(
        dev_t.withName("dev"),
        ino_t.withName("ino"),
        nlink_t.withName("nlink"),
        mode_t.withName("mode"),
        uid_t.withName("uid"),
        gid_t.withName("gid"),
        paddingLayout(32).withBitAlignment(32),
        dev_t.withName("rdev"),
        off_t.withName("size"),
        blksize_t.withName("blksize"),
        blkcnt_t.withName("blkcnt"),
        timespec.withName("atim"),
        timespec.withName("mtim"),
        timespec.withName("ctim"),
        paddingLayout(3 * 64).withBitAlignment(64)
    );

    static final VarHandle st_dev = stat.varHandle(long.class, groupElement("dev"));
    static final VarHandle st_ino = stat.varHandle(long.class, groupElement("ino"));
    static final VarHandle st_nlink = stat.varHandle(long.class, groupElement("nlink"));
    static final VarHandle st_mode = stat.varHandle(int.class, groupElement("mode"));
    static final VarHandle st_size = stat.varHandle(long.class, groupElement("size"));

    static final MethodHandle st_atim = stat.sliceHandle(groupElement("atim"));
    static final MethodHandle st_mtim = stat.sliceHandle(groupElement("atim"));
    static final MethodHandle st_ctim = stat.sliceHandle(groupElement("atim"));

    static final MemoryLayout iovec = cStructLayout(
        C_POINTER.withName("base"),
        size_t.withName("len")
    );

    static final VarHandle iov_base = iovec.varHandle(MemoryAddress.class, groupElement("base"));
    static final VarHandle iov_len = iovec.varHandle(long.class, groupElement("len"));

    static final MethodHandle iovec_list_elem = sequenceLayout(iovec).sliceHandle(sequenceElement());

    static final int EPERM = 1;
    static final int ENOENT = 2;
    static final int ESRCH = 3;
    static final int EINTR = 4;
    static final int EIO = 5;
    static final int ENXIO = 6;
    static final int E2BIG = 7;
    static final int ENOEXEC = 8;
    static final int EBADF = 9;
    static final int ECHILD = 10;
    static final int EAGAIN = 11;
    static final int ENOMEM = 12;
    static final int EACCES = 13;
    static final int EFAULT = 14;
    static final int EBUSY = 16;
    static final int EEXIST = 17;
    static final int EXDEV = 18;
    static final int ENODEV = 19;
    static final int ENOTDIR = 20;
    static final int EISDIR = 21;
    static final int EINVAL = 22;
    static final int ENFILE = 23;
    static final int EMFILE = 24;
    static final int ENOTTY = 25;
    static final int ETXTBSY = 26;
    static final int EFBIG = 27;
    static final int ENOSPC = 28;
    static final int ESPIPE = 29;
    static final int EROFS = 30;
    static final int EMLINK = 31;
    static final int EPIPE = 32;
    static final int EDOM = 33;
    static final int ERANGE = 34;
    static final int EDEADLK = 35;
    static final int ENAMETOOLONG = 36;
    static final int ENOLCK = 37;
    static final int ENOSYS = 38;
    static final int ENOTEMPTY = 39;
    static final int ELOOP = 40;
    static final int ENOMSG = 42;
    static final int EIDRM = 43;
    static final int ENOLINK = 67;
    static final int EPROTO = 71;
    static final int EMULTIHOP = 72;
    static final int EBADMSG = 74;
    static final int EOVERFLOW = 75;
    static final int EILSEQ = 84;
    static final int ENOTSOCK = 88;
    static final int EDESTADDRREQ = 89;
    static final int EMSGSIZE = 90;
    static final int EPROTOTYPE = 91;
    static final int ENOPROTOOPT = 92;
    static final int EPROTONOSUPPORT = 93;
    static final int ENOTSUP = 95;
    static final int EAFNOSUPPORT = 97;
    static final int EADDRINUSE = 98;
    static final int EADDRNOTAVAIL = 99;
    static final int ENETDOWN = 100;
    static final int ENETUNREACH = 101;
    static final int ENETRESET = 102;
    static final int ECONNABORTED = 103;
    static final int ECONNRESET = 104;
    static final int ENOBUFS = 105;
    static final int EISCONN = 106;
    static final int ENOTCONN = 107;
    static final int ETIMEDOUT = 110;
    static final int ECONNREFUSED = 111;
    static final int EHOSTUNREACH = 113;
    static final int EALREADY = 114;
    static final int EINPROGRESS = 115;
    static final int ESTALE = 116;
    static final int EDQUOT = 122;
    static final int ECANCELED = 125;
    static final int EOWNERDEAD = 130;
    static final int ENOTRECOVERABLE = 131;

    static final int CLOCK_REALTIME = 0;
    static final int CLOCK_MONOTONIC = 1;
    static final int CLOCK_PROCESS_CPUTIME_ID = 2;
    static final int CLOCK_THREAD_CPUTIME_ID = 3;

    static final int POSIX_FADV_NORMAL = 0;
    static final int POSIX_FADV_RANDOM = 1;
    static final int POSIX_FADV_SEQUENTIAL = 2;
    static final int POSIX_FADV_WILLNEED = 3;
    static final int POSIX_FADV_DONTNEED = 4;
    static final int POSIX_FADV_NOREUSE = 5;

    static final int F_GETFL = 3;
    static final int F_SETFL = 4;

    static final int O_CREAT = 1 << 6;
    static final int O_EXCL = 1 << 7;
    static final int O_APPEND = 1 << 10;
    static final int O_NONBLOCK = 1 << 11;
    static final int O_DSYNC = 1 << 12;
    static final int O_ASYNC = 1 << 13;
    static final int O_DIRECT = 1 << 14;
    static final int O_DIRECTORY = 1 << 16;
    static final int O_NOFOLLOW = 1 << 17;
    static final int O_NOATIME = 1 << 18;
    static final int O_CLOEXEC = 1 << 19;
    static final int O_SYNC = (1 << 20) | O_DSYNC;
    static final int O_PATH = 1 << 21;

    static final int S_IFIFO = 0x1000;
    static final int S_IFCHR = 0x2000;
    static final int S_IFDIR = 0x4000;
    static final int S_IFBLK = 0x6000;
    static final int S_IFREG = 0x8000;
    static final int S_IFLNK = 0xa000;
    static final int S_IFSOCK = 0xc000;

    static final int SOL_SOCKET = 1;

    static final int SO_TYPE = 3;

    static final int SOCK_STREAM = 1;
    static final int SOCK_DGRAM = 2;

    static final long UTIME_NOW = (1 << 30) - 1;
    static final long UTIME_OMIT = (1 << 30) - 2;

    static final MethodHandle __errno_location;
    static final MethodHandle clock_getres;
    static final MethodHandle clock_gettime;
    static final MethodHandle posix_fadvise;
    static final MethodHandle posix_fallocate;
    static final MethodHandle close;
    static final MethodHandle fdatasync;
    static final MethodHandle fcntl_GETFL;
    static final MethodHandle fcntl_SETFL;
    static final MethodHandle fstat;
    static final MethodHandle ftruncate;
    static final MethodHandle getsockopt;
    static final MethodHandle futimens;
    static final MethodHandle pread;
    static final MethodHandle pwrite;
    static final MethodHandle preadv;
    static final MethodHandle pwritev;
    static final MethodHandle read;
    static final MethodHandle readv;
    static final MethodHandle write;
    static final MethodHandle writev;

    static {
        var lookup = MethodHandles.lookup();
        var linker = CLinker.getInstance();
        var lib = CLinker.systemLookup();

        MethodHandle throwErrnoOnNonzero;
        MethodHandle throwArgOnNonzero;
        MethodHandle throwErrnoOnM1;
        try {
            throwErrnoOnNonzero = lookup.findStatic(Linux.class, "throwErrnoOnNonzero", methodType(void.class, int.class));
            throwArgOnNonzero = lookup.findStatic(Linux.class, "throwArgOnNonzero", methodType(void.class, int.class));
            throwErrnoOnM1 = lookup.findStatic(Linux.class, "throwErrnoOnM1", methodType(int.class, int.class));
        }
        catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new UnsupportedOperationException(exception);
        }

        __errno_location = linker.downcallHandle(
            lib.lookup("__errno_location").orElseThrow(),
            methodType(MemoryAddress.class),
            FunctionDescriptor.of(C_POINTER)
        );

        clock_getres = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("clock_getres").orElseThrow(),
                methodType(int.class, int.class, MemoryAddress.class),
                FunctionDescriptor.of(C_INT, C_INT, C_POINTER)
            ),
            throwErrnoOnNonzero
        );

        clock_gettime = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("clock_gettime").orElseThrow(),
                methodType(int.class, int.class, MemoryAddress.class),
                FunctionDescriptor.of(C_INT, C_INT, C_POINTER)
            ),
            throwErrnoOnNonzero
        );

        posix_fadvise = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("posix_fadvise").orElseThrow(),
                methodType(int.class, int.class, long.class, long.class, int.class),
                FunctionDescriptor.of(C_INT, C_INT, off_t, off_t, C_INT)
            ),
            throwArgOnNonzero
        );

        posix_fallocate = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("posix_fallocate").orElseThrow(),
                methodType(int.class, int.class, long.class, long.class),
                FunctionDescriptor.of(C_INT, C_INT, off_t, off_t)
            ),
            throwErrnoOnNonzero
        );

        close = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("close").orElseThrow(),
                methodType(int.class, int.class),
                FunctionDescriptor.of(C_INT, C_INT)
            ),
            throwErrnoOnNonzero
        );

        fdatasync = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("fdatasync").orElseThrow(),
                methodType(int.class, int.class),
                FunctionDescriptor.of(C_INT, C_INT)
            ),
            throwErrnoOnNonzero
        );

        fcntl_GETFL = filterReturnValue(
            insertArguments(
                linker.downcallHandle(
                    lib.lookup("fcntl").orElseThrow(),
                    methodType(int.class, int.class, int.class),
                    FunctionDescriptor.of(C_INT, C_INT, C_INT)
                ),
                1, F_GETFL
            ),
            throwErrnoOnM1
        );

        fcntl_SETFL = filterReturnValue(
            insertArguments(
                linker.downcallHandle(
                    lib.lookup("fcntl").orElseThrow(),
                    methodType(int.class, int.class, int.class, int.class),
                    FunctionDescriptor.of(C_INT, C_INT, C_INT, asVarArg(C_INT))
                ),
                1, F_SETFL
            ),
            throwErrnoOnNonzero
        );

        fstat = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("fstat").orElseThrow(),
                methodType(int.class, int.class, MemoryAddress.class),
                FunctionDescriptor.of(C_INT, C_INT, C_POINTER)
            ),
            throwErrnoOnNonzero
        );

        ftruncate = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("ftruncate").orElseThrow(),
                methodType(int.class, int.class, long.class),
                FunctionDescriptor.of(C_INT, C_INT, off_t)
            ),
            throwErrnoOnNonzero
        );

        getsockopt = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("getsockopt").orElseThrow(),
                methodType(int.class, int.class, int.class, int.class, MemoryAddress.class, MemoryAddress.class),
                FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_POINTER, C_POINTER)
            ),
            throwErrnoOnNonzero
        );

        futimens = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("futimens").orElseThrow(),
                methodType(int.class, int.class, MemoryAddress.class),
                FunctionDescriptor.of(C_INT, C_INT, C_POINTER)
            ),
            throwErrnoOnNonzero
        );

        pread = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("pread").orElseThrow(),
                methodType(long.class, int.class, MemoryAddress.class, long.class, long.class),
                FunctionDescriptor.of(ssize_t, C_INT, C_POINTER, size_t, off_t)
            ),
            throwErrnoOnM1
        );

        pwrite = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("pwrite").orElseThrow(),
                methodType(long.class, int.class, MemoryAddress.class, long.class, long.class),
                FunctionDescriptor.of(ssize_t, C_INT, C_POINTER, size_t, off_t)
            ),
            throwErrnoOnM1
        );

        preadv = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("preadv").orElseThrow(),
                methodType(long.class, int.class, MemoryAddress.class, int.class, long.class),
                FunctionDescriptor.of(ssize_t, C_INT, C_POINTER, C_INT, off_t)
            ),
            throwErrnoOnM1
        );

        pwritev = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("pwritev").orElseThrow(),
                methodType(long.class, int.class, MemoryAddress.class, int.class, long.class),
                FunctionDescriptor.of(ssize_t, C_INT, C_POINTER, C_INT, off_t)
            ),
            throwErrnoOnM1
        );

        read = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("read").orElseThrow(),
                methodType(long.class, int.class, MemoryAddress.class, long.class),
                FunctionDescriptor.of(ssize_t, C_INT, C_POINTER, size_t)
            ),
            throwErrnoOnM1
        );

        readv = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("readv").orElseThrow(),
                methodType(long.class, int.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(ssize_t, C_INT, C_POINTER, C_INT)
            ),
            throwErrnoOnM1
        );

        write = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("write").orElseThrow(),
                methodType(long.class, int.class, MemoryAddress.class, long.class),
                FunctionDescriptor.of(ssize_t, C_INT, C_POINTER, size_t)
            ),
            throwErrnoOnM1
        );

        writev = filterReturnValue(
            linker.downcallHandle(
                lib.lookup("writev").orElseThrow(),
                methodType(long.class, int.class, MemoryAddress.class, int.class),
                FunctionDescriptor.of(ssize_t, C_INT, C_POINTER, C_INT)
            ),
            throwErrnoOnM1
        );
    }

    private static void throwErrnoOnNonzero(int returnCode) throws Throwable {
        if (returnCode != 0) {
            throw errno();
        }
    }

    private static void throwArgOnNonzero(int returnCode) throws Throwable {
        if (returnCode != 0) {
            throw new ErrnoException(returnCode);
        }
    }

    private static int throwErrnoOnM1(int returnValue) throws Throwable {
        if (returnValue == -1) {
            throw errno();
        }

        return returnValue;
    }

    private static @NotNull ErrnoException errno() throws Throwable {
        var location = (MemoryAddress) __errno_location.invoke();
        var linuxCode = MemoryAccess.getInt(location.asSegment(4, ResourceScope.globalScope()));

        var code = switch (linuxCode) {
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

        return new ErrnoException(code);
    }

    static long timespecToNanos(@NotNull MemorySegment timespec) {
        return (long) tv_sec.get(timespec) * 1_000_000_000 + (long) tv_nsec.get(timespec);
    }

    static void fillTimespec(@NotNull MemorySegment spec, long nanos) {
        tv_sec.set(spec, Long.divideUnsigned(nanos, 1_000_000_000));
        tv_nsec.set(spec, Long.remainderUnsigned(nanos, 1_000_000_000));
    }
}
