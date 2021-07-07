package org.wastastic.wasi;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;

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

    static {
        var linker = CLinker.getInstance();
        var lib = CLinker.systemLookup();

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
    }
}
