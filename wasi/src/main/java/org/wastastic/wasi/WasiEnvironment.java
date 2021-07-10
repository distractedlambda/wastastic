package org.wastastic.wasi;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.wastastic.Memory;
import org.wastastic.ModuleInstance;
import org.wastastic.QualifiedName;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.WildcardType;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import static java.lang.System.arraycopy;
import static java.lang.invoke.MethodType.methodType;
import static java.nio.file.Files.readAttributes;
import static java.nio.file.Files.readSymbolicLink;
import static java.util.Objects.requireNonNull;

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

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocateDirect(0);
    private static final ByteBuffer ZEROS_BUFFER = ByteBuffer.allocateDirect(4096);

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
        var memory = (Memory) memoryHandle.get(module);

        for (var i = 0; i < envOffsets.length; i++) {
            memory.setInt(ptrsAddress + i*4, dataAddress + envOffsets[i]);
        }

        memory.setBytes(dataAddress, envBytes);
        return ERRNO_SUCCESS;
    }

    private int environSizesGet(int numVarsAddress, int dataSizeAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);
        memory.setInt(numVarsAddress, envOffsets.length);
        memory.setInt(dataSizeAddress, envBytes.length);
        return ERRNO_SUCCESS;
    }

    private int clockResGet(int clockId, int resolutionOut, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);

        if (clockId < 0 || clockId > CLOCKID_THREAD_CPUTIME_ID) {
            return ERRNO_INVAL;
        }

        long resolution;
        try (var frame = MemoryStack.getFrame()) {
            var timespec = frame.allocate(Linux.timespec);

            if ((int) Linux.clock_getres.invoke(clockId, timespec.address()) != 0) {
                return translateErrno(Linux.errno());
            }

            var sec = (long) Linux.tv_sec.get(timespec);
            var nsec = (long) Linux.tv_nsec.get(timespec);
            resolution = addExact(multiplyExact(sec, 1_000_000_000L), nsec);
        }

        memory.setLong(resolutionOut, resolution);
        return ERRNO_SUCCESS;
    }

    private int clockTimeGet(int clockId, long precision, int timeOut, @NotNull ModuleInstance module) throws Throwable {
        var memory = (Memory) memoryHandle.get(module);

        if (clockId < 0 || clockId > CLOCKID_THREAD_CPUTIME_ID) {
            return ERRNO_INVAL;
        }

        long time;
        try (var frame = MemoryStack.getFrame()) {
            var timespec = frame.allocate(Linux.timespec);

            if ((int) Linux.clock_gettime.invoke(clockId, timespec.address()) != 0) {
                return translateErrno(Linux.errno());
            }

            var sec = (long) Linux.tv_sec.get(timespec);
            var nsec = (long) Linux.tv_nsec.get(timespec);
            time = addExact(multiplyExact(sec, 1_000_000_000L), nsec);
        }

        memory.setLong(timeOut, time);
        return ERRNO_SUCCESS;
    }

    private int fdAdvise(int fd, long offset, long length, int advice, @NotNull ModuleInstance module) throws Throwable {
        OpenFile file;
        if ((file = fdTable.get(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights() & RIGHTS_FD_ADVISE) == 0) {
            return ERRNO_NOTCAPABLE;
        }

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
                return ERRNO_INVAL;
            }
        }

        return translateErrno((int) Linux.posix_fadvise.invoke(file.nativeFd(), offset, length, linuxAdvice));
    }

    private int fdAllocate(int fd, long offset, long length, @NotNull ModuleInstance module) throws Throwable {
        OpenFile file;
        if ((file = fdTable.get(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights() & RIGHTS_FD_ALLOCATE) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        return translateErrno((int) Linux.posix_fallocate.invoke(file.nativeFd(), offset, length));
    }

    private int fdClose(int fd, @NotNull ModuleInstance module) throws Throwable {
        OpenFile file;
        if ((file = fdTable.get(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((int) Linux.close.invoke(file.nativeFd()) != 0) {
            return translateErrno(Linux.errno());
        }

        fdTable.remove(fd);
        retiredFds.addLast(fd);
        return ERRNO_SUCCESS;
    }

    private int fdDatasync(int fd, @NotNull ModuleInstance module) throws Throwable {
        OpenFile file;
        if ((file = fdTable.get(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights() & RIGHTS_FD_DATASYNC) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if ((int) Linux.fdatasync.invoke(file.nativeFd()) != 0) {
            return translateErrno(Linux.errno());
        }

        return ERRNO_SUCCESS;
    }

    private int fdFdstatGet(int fd, int statAddress, @NotNull ModuleInstance module) throws Throwable {
        var stack = MemoryStack.get();
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = fdTable.get(fd)) == null) {
            return ERRNO_BADF;
        }

        int linuxFlags;
        if ((linuxFlags = (int) Linux.fcntl_void.invoke(file.nativeFd(), Linux.F_GETFL)) == -1) {
            return translateErrno(Linux.errno());
        }

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

        int linuxType;
        try (var frame = stack.frame()) {
            var stat = frame.allocate(Linux.stat);

            if ((int) Linux.fstat.invoke(file.nativeFd(), stat.address()) != 0) {
                return translateErrno(Linux.errno());
            }

            linuxType = ((int) Linux.st_mode.get(stat) >> 12) & 0xf;
        }

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
                try (var frame = stack.frame()) {
                    var optval = frame.allocate(MemoryLayouts.JAVA_INT);
                    var optlen = frame.allocate(Linux.socklen_t);
                    MemoryAccess.setInt(optlen, 4);

                    if ((int) Linux.getsockopt.invoke(file.nativeFd(), Linux.SOL_SOCKET, Linux.SO_TYPE, optval.address(), optlen.address()) != 0) {
                        return translateErrno(Linux.errno());
                    }

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

        memory.setByte(statAddress, type);
        memory.setShort(statAddress + 2, flags);
        memory.setLong(statAddress + 8, file.baseRights());
        memory.setLong(statAddress + 16, file.inheritingRights());

        return ERRNO_SUCCESS;
    }

    private int fdFdstatSetFlags(int fd, short newFlags, @NotNull ModuleInstance module) {
        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_FDSTAT_SET_FLAGS) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof FileChannel channel)) {
            return ERRNO_NODEV;
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
            channel.close();
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

        if ((file.baseRights & RIGHTS_FD_FILESTAT_GET) == 0) {
            return ERRNO_NOTCAPABLE;
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
        memory.setLong(filestatAddress + 40, attributes.lastAccessTime().to(TimeUnit.NANOSECONDS));
        memory.setLong(filestatAddress + 48, attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS));
        memory.setLong(filestatAddress + 56, attributes.lastModifiedTime().to(TimeUnit.NANOSECONDS));

        return ERRNO_SUCCESS;
    }

    private int fdFilestatSetSize(int fd, long size, @NotNull ModuleInstance module) {
        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_FILESTAT_SET_SIZE) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof FileChannel channel)) {
            return ERRNO_NODEV;
        }

        try {
            var lastSize = channel.size();
            if (size < lastSize) {
                channel.truncate(size);
            }
            else if (size > lastSize) {
                var offset = lastSize;
                while (offset < size) {
                    offset += channel.write(ZEROS_BUFFER.slice(0, (int) Long.min(ZEROS_BUFFER.remaining(), size - offset)), offset);
                }
            }
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        return ERRNO_SUCCESS;
    }

    private int fdFilestatSetTimes(int fd, long accessTime, long modificationTime, short fstFlags, @NotNull ModuleInstance module) {
        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_FILESTAT_SET_TIMES) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if ((fstFlags & FSTFLAGS_ATIM) != 0 && (fstFlags & FSTFLAGS_ATIM_NOW) != 0) {
            return ERRNO_INVAL;
        }

        if ((fstFlags & FSTFLAGS_MTIM) != 0 && (fstFlags & FSTFLAGS_MTIM_NOW) != 0) {
            return ERRNO_INVAL;
        }

        var attributesView = Files.getFileAttributeView(file.path, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);

        try {
            var attributes = attributesView.readAttributes();

            FileTime newLastAccessTime, newLastModificationTime;

            var now = System.currentTimeMillis();

            if ((fstFlags & FSTFLAGS_ATIM) != 0) {
                newLastAccessTime = FileTime.from(accessTime, TimeUnit.NANOSECONDS);
            }
            else if ((fstFlags & FSTFLAGS_ATIM_NOW) != 0) {
                newLastAccessTime = FileTime.from(now, TimeUnit.MILLISECONDS);
            }
            else {
                newLastAccessTime = attributes.lastAccessTime();
            }

            if ((fstFlags & FSTFLAGS_MTIM) != 0) {
                newLastModificationTime = FileTime.from(modificationTime, TimeUnit.NANOSECONDS);
            }
            else if ((fstFlags & FSTFLAGS_MTIM_NOW) != 0) {
                newLastModificationTime = FileTime.from(now, TimeUnit.MILLISECONDS);
            }
            else {
                newLastModificationTime = attributes.lastModifiedTime();
            }

            attributesView.setTimes(newLastModificationTime, newLastAccessTime, attributes.creationTime());
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        return ERRNO_SUCCESS;
    }

    private int fdPread(int fd, int iovsAddress, int iovsCount, long offset, int bytesReadAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        if (offset < 0) {
            return ERRNO_INVAL;
        }

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_READ) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof FileChannel channel)) {
            return ERRNO_NODEV;
        }

        if (iovsCount == 0) {
            memory.setInt(bytesReadAddress, 0);
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

        int bytesRead;
        try {
            if (iovsCount == 1) {
                bytesRead = Integer.max(0, channel.read(buffers[0], offset));
            }
            else {
                var lastPosition = channel.position();
                channel.position(offset);
                bytesRead = (int) Long.max(0, channel.read(buffers));
                channel.position(lastPosition);
            }
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        memory.setInt(bytesReadAddress, bytesRead);
        return ERRNO_SUCCESS;
    }

    private int fdPrestatGet(int fd, int prestatAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if (!file.isPreopened) {
            return ERRNO_BADF;
        }

        memory.setByte(prestatAddress, PREOPENTYPE_DIR);
        memory.setInt(prestatAddress + 4, file.path.toString().getBytes(StandardCharsets.UTF_8).length);

        return ERRNO_SUCCESS;
    }

    private int fdPrestatDirName(int fd, int outAddress, int outLength, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if (!file.isPreopened) {
            return ERRNO_BADF;
        }

        var bytes = file.path.toString().getBytes(StandardCharsets.UTF_8);

        if (bytes.length != outLength) {
            return ERRNO_INVAL;
        }

        memory.setBytes(outAddress, bytes);

        return ERRNO_SUCCESS;
    }

    private int fdPwrite(int fd, int iovsAddress, int iovsCount, long offset, int bytesWrittenAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        if (offset < 0) {
            return ERRNO_INVAL;
        }

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_WRITE) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof FileChannel channel)) {
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
            if (iovsCount == 1) {
                bytesWritten = Integer.max(0, channel.write(buffers[0], offset));
            }
            else {
                var lastPosition = channel.position();
                channel.position(offset);
                bytesWritten = (int) Long.max(0, channel.write(buffers));
                channel.position(lastPosition);
            }
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        memory.setInt(bytesWrittenAddress, bytesWritten);
        return ERRNO_SUCCESS;
    }

    private int fdRead(int fd, int iovsAddress, int iovsCount, int bytesReadAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_READ) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (!(file.channel instanceof ScatteringByteChannel channel)) {
            return ERRNO_NODEV;
        }

        if (iovsCount == 0) {
            memory.setInt(bytesReadAddress, 0);
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

        int bytesRead;
        try {
            bytesRead = (int) Long.max(0, channel.read(buffers));
        }
        catch (IOException ignored) {
            return ERRNO_IO;
        }

        memory.setInt(bytesReadAddress, bytesRead);
        return ERRNO_SUCCESS;
    }

    private int fdReaddir(int fd, int buf, int bufLen, long cookie, int sizeAddress, @NotNull ModuleInstance module) {
        var memory = (Memory) memoryHandle.get(module);

        OpenFile file;
        if ((file = openFile(fd)) == null) {
            return ERRNO_BADF;
        }

        if ((file.baseRights & RIGHTS_FD_READDIR) == 0) {
            return ERRNO_NOTCAPABLE;
        }

        if (cookie < 0) {
            return ERRNO_INVAL;
        }

        try (var entries = Files.list(file.path)) {
            var paths = entries.skip(cookie).toArray(Path[]::new);

            var outAddress = buf;
            var endAddress = buf + bufLen;

            for (var i = 0; i < paths.length; i++) {
                outAddress = (outAddress + 7) & ~7;

                if (Integer.compareUnsigned(endAddress - outAddress, 24) < 0) {
                    outAddress = endAddress;
                    break;
                }

                var path = paths[i];
                var bytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);

                memory.setLong(outAddress, cookie + i);
                memory.setLong(outAddress + 8, 0); // 'd_ino', not implemented
                memory.setInt(outAddress + 16, bytes.length);

                var attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                memory.setByte(outAddress + 20, detectFiletype(attributes));

                outAddress += 24;

                if (Integer.compareUnsigned(endAddress - outAddress, bytes.length) < 0) {
                    memory.setBytes(outAddress, bytes, 0, endAddress - outAddress);
                    outAddress = endAddress;
                    break;
                }
                else {
                    memory.setBytes(outAddress, bytes);
                    outAddress += bytes.length;
                }
            }

            memory.setInt(sizeAddress, outAddress - buf);
        }
        catch (IOException | UncheckedIOException ignored) {
            return ERRNO_IO;
        }

        return ERRNO_SUCCESS;
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

    private static final byte PREOPENTYPE_DIR = 0;

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
    private static final int FILETYPE_BLOCK_DEVICE = 1;
    private static final int FILETYPE_CHARACTER_DEVICE = 2;
    private static final int FILETYPE_DIRECTORY = 3;
    private static final int FILETYPE_REGULAR_FILE = 4;
    private static final int FILETYPE_SOCKET_DGRAM = 5;
    private static final int FILETYPE_SOCKET_STREAM = 6;
    private static final int FILETYPE_SYMBOLIC_LINK = 7;

    private static final byte WHENCE_SET = 0;
    private static final byte WHENCE_CUR = 1;
    private static final byte WHENCE_END = 2;

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
    private static final int ERRNO_TXTBSY = 74;
    private static final int ERRNO_XDEV = 75;
    private static final int ERRNO_NOTCAPABLE = 76;
}
