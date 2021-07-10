package org.wastastic.wasi;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.ResourceScope;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import static java.lang.invoke.MethodType.methodType;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import static jdk.incubator.foreign.CLinker.asVarArg;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static jdk.incubator.foreign.MemoryLayout.paddingLayout;
import static org.wastastic.wasi.LayoutUtils.cStructLayout;

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
    static final VarHandle st_atim_sec = stat.varHandle(long.class, groupElement("atim"), groupElement("sec"));
    static final VarHandle st_atim_nsec = stat.varHandle(long.class, groupElement("atim"), groupElement("nsec"));
    static final VarHandle st_mtim_sec = stat.varHandle(long.class, groupElement("mtim"), groupElement("sec"));
    static final VarHandle st_mtim_nsec = stat.varHandle(long.class, groupElement("mtim"), groupElement("nsec"));
    static final VarHandle st_ctim_sec = stat.varHandle(long.class, groupElement("ctim"), groupElement("sec"));
    static final VarHandle st_ctim_nsec = stat.varHandle(long.class, groupElement("ctim"), groupElement("nsec"));

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

    static final MethodHandle __errno_location;
    static final MethodHandle clock_getres;
    static final MethodHandle clock_gettime;
    static final MethodHandle posix_fadvise;
    static final MethodHandle posix_fallocate;
    static final MethodHandle close;
    static final MethodHandle fdatasync;
    static final MethodHandle fcntl_void;
    static final MethodHandle fcntl_int;
    static final MethodHandle fstat;
    static final MethodHandle ftruncate;
    static final MethodHandle getsockopt;

    static {
        var linker = CLinker.getInstance();
        var lib = CLinker.systemLookup();

        __errno_location = linker.downcallHandle(
            lib.lookup("__errno_location").orElseThrow(),
            methodType(MemoryAddress.class),
            FunctionDescriptor.of(C_POINTER)
        );

        clock_getres = linker.downcallHandle(
            lib.lookup("clock_getres").orElseThrow(),
            methodType(int.class, int.class, MemoryAddress.class),
            FunctionDescriptor.of(C_INT, C_INT, C_POINTER)
        );

        clock_gettime = linker.downcallHandle(
            lib.lookup("clock_gettime").orElseThrow(),
            methodType(int.class, int.class, MemoryAddress.class),
            FunctionDescriptor.of(C_INT, C_INT, C_POINTER)
        );

        posix_fadvise = linker.downcallHandle(
            lib.lookup("posix_fadvise").orElseThrow(),
            methodType(int.class, int.class, long.class, long.class, int.class),
            FunctionDescriptor.of(C_INT, C_INT, off_t, off_t, C_INT)
        );

        posix_fallocate = linker.downcallHandle(
            lib.lookup("posix_fallocate").orElseThrow(),
            methodType(int.class, int.class, long.class, long.class),
            FunctionDescriptor.of(C_INT, C_INT, off_t, off_t)
        );

        close = linker.downcallHandle(
            lib.lookup("close").orElseThrow(),
            methodType(int.class, int.class),
            FunctionDescriptor.of(C_INT, C_INT)
        );

        fdatasync = linker.downcallHandle(
            lib.lookup("fdatasync").orElseThrow(),
            methodType(int.class, int.class),
            FunctionDescriptor.of(C_INT, C_INT)
        );

        fcntl_void = linker.downcallHandle(
            lib.lookup("fcntl").orElseThrow(),
            methodType(int.class, int.class, int.class),
            FunctionDescriptor.of(C_INT, C_INT, C_INT)
        );

        fcntl_int = linker.downcallHandle(
            lib.lookup("fcntl").orElseThrow(),
            methodType(int.class, int.class, int.class, int.class),
            FunctionDescriptor.of(C_INT, C_INT, C_INT, asVarArg(C_INT))
        );

        fstat = linker.downcallHandle(
            lib.lookup("fstat").orElseThrow(),
            methodType(int.class, int.class, MemoryAddress.class),
            FunctionDescriptor.of(C_INT, C_INT, C_POINTER)
        );

        ftruncate = linker.downcallHandle(
            lib.lookup("ftruncate").orElseThrow(),
            methodType(int.class, int.class, long.class),
            FunctionDescriptor.of(C_INT, C_INT, off_t)
        );

        getsockopt = linker.downcallHandle(
            lib.lookup("getsockopt").orElseThrow(),
            methodType(int.class, int.class, int.class, int.class, MemoryAddress.class, MemoryAddress.class),
            FunctionDescriptor.of(C_INT, C_INT, C_INT, C_INT, C_POINTER, C_POINTER)
        );
    }

    static int errno() throws Throwable {
        var location = (MemoryAddress) __errno_location.invoke();
        return MemoryAccess.getInt(location.asSegment(4, ResourceScope.globalScope()));
    }
}
