package org.wastastic.wasi;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import org.wastastic.Memory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

import static java.lang.invoke.MethodType.methodType;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static org.wastastic.wasi.LayoutUtils.cStructLayout;

final class Linux {
    private Linux() {}

    static final MemoryLayout time_t = C_LONG;
    static final MemoryLayout off_t = C_LONG;

    static final MemoryLayout timespec = cStructLayout(
        time_t.withName("tv_sec"),
        C_LONG.withName("tv_nsec")
    );

    static final VarHandle timespec_tv_sec = timespec.varHandle(long.class, groupElement("tv_sec"));
    static final VarHandle timespec_tv_nsec = timespec.varHandle(long.class, groupElement("tv_nsec"));

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
            lib.lookup("fdatasync")
        )
    }
}
