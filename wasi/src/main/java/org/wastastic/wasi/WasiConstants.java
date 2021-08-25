package org.wastastic.wasi;

final class WasiConstants {
    static final byte PREOPENTYPE_DIR = 0;

    static final int SDFLAGS_RD = 1;
    static final int SDFLAGS_WR = 1 << 1;

    static final int ROFLAGS_RECV_DATA_TRUNCATED = 1;

    static final int RIFLAGS_RECV_PEEK = 1;
    static final int RIFLAGS_RECV_WAITALL = 1 << 1;

    static final int SIGNAL_NONE = 0;
    static final int SIGNAL_HUP = 1;
    static final int SIGNAL_INT = 2;
    static final int SIGNAL_QUIT = 3;
    static final int SIGNAL_ILL = 4;
    static final int SIGNAL_TRAP = 5;
    static final int SIGNAL_ABRT = 6;
    static final int SIGNAL_BUS = 7;
    static final int SIGNAL_FPE = 8;
    static final int SIGNAL_KILL = 9;
    static final int SIGNAL_USR1 = 10;
    static final int SIGNAL_SEGV = 11;
    static final int SIGNAL_USR2 = 12;
    static final int SIGNAL_PIPE = 13;
    static final int SIGNAL_ALRM = 14;
    static final int SIGNAL_TERM = 15;
    static final int SIGNAL_CHLD = 16;
    static final int SIGNAL_CONT = 17;
    static final int SIGNAL_STOP = 18;
    static final int SIGNAL_TSTP = 19;
    static final int SIGNAL_TTIN = 20;
    static final int SIGNAL_TTOU = 21;
    static final int SIGNAL_URG = 22;
    static final int SIGNAL_XCPU = 23;
    static final int SIGNAL_XFSZ = 24;
    static final int SIGNAL_VTALRM = 25;
    static final int SIGNAL_PROF = 26;
    static final int SIGNAL_WINCH = 27;
    static final int SIGNAL_POLL = 28;
    static final int SIGNAL_PWR = 29;
    static final int SIGNAL_SYS = 30;

    static final int SUBCLOCK_FLAGS_SUBSCRIPTION_CLOCK_ABSTIME = 1;

    static final int EVENTRWFLAGS_FD_READWRITE_HANGUP = 1;

    static final int EVENTTYPE_CLOCK = 1;
    static final int EVENTTYPE_FD_READ = 1 << 1;
    static final int EVENTTYPE_FD_WRITE = 1 << 2;

    static final short OFLAGS_CREAT = 1;
    static final short OFLAGS_DIRECTORY = 1 << 1;
    static final short OFLAGS_EXCL = 1 << 2;
    static final short OFLAGS_TRUNC = 1 << 3;
    static final short ILLEGAL_OFLAGS = -(OFLAGS_TRUNC << 1);

    static final int LOOKUPFLAGS_SYMLINK_FOLLOW = 1;
    static final int ILLEGAL_LOOKUPFLAGS = -(LOOKUPFLAGS_SYMLINK_FOLLOW << 1);

    static final int FSTFLAGS_ATIM = 1;
    static final int FSTFLAGS_ATIM_NOW = 1 << 1;
    static final int FSTFLAGS_MTIM = 1 << 2;
    static final int FSTFLAGS_MTIM_NOW = 1 << 3;
    static final int ILLEGAL_FSTFLAGS = -(FSTFLAGS_MTIM_NOW << 1);

    static final int FDFLAGS_APPEND = 1;
    static final int FDFLAGS_DSYNC = 1 << 1;
    static final int FDFLAGS_NONBLOCK = 1 << 2;
    static final int FDFLAGS_RSYNC = 1 << 3;
    static final int FDFLAGS_SYNC = 1 << 4;
    static final int ILLEGAL_FDFLAGS = -(FDFLAGS_SYNC << 1);

    static final byte FILETYPE_UNKNOWN = 0;
    static final byte FILETYPE_BLOCK_DEVICE = 1;
    static final byte FILETYPE_CHARACTER_DEVICE = 2;
    static final byte FILETYPE_DIRECTORY = 3;
    static final byte FILETYPE_REGULAR_FILE = 4;
    static final byte FILETYPE_SOCKET_DGRAM = 5;
    static final byte FILETYPE_SOCKET_STREAM = 6;
    static final byte FILETYPE_SYMBOLIC_LINK = 7;

    static final byte WHENCE_SET = 0;
    static final byte WHENCE_CUR = 1;
    static final byte WHENCE_END = 2;

    static final long RIGHTS_FD_DATASYNC = 1;
    static final long RIGHTS_FD_READ = 1 << 1;
    static final long RIGHTS_FD_SEEK = 1 << 2;
    static final long RIGHTS_FD_FDSTAT_SET_FLAGS = 1 << 3;
    static final long RIGHTS_FD_SYNC = 1 << 4;
    static final long RIGHTS_FD_TELL = 1 << 5;
    static final long RIGHTS_FD_WRITE = 1 << 6;
    static final long RIGHTS_FD_ADVISE = 1 << 7;
    static final long RIGHTS_FD_ALLOCATE = 1 << 8;
    static final long RIGHTS_PATH_CREATE_DIRECTORY = 1 << 9;
    static final long RIGHTS_PATH_CREATE_FILE = 1 << 10;
    static final long RIGHTS_PATH_LINK_SOURCE = 1 << 11;
    static final long RIGHTS_PATH_LINK_TARGET = 1 << 12;
    static final long RIGHTS_PATH_OPEN = 1 << 13;
    static final long RIGHTS_FD_READDIR = 1 << 14;
    static final long RIGHTS_PATH_READLINK = 1 << 15;
    static final long RIGHTS_PATH_RENAME_SOURCE = 1 << 16;
    static final long RIGHTS_PATH_RENAME_TARGET = 1 << 17;
    static final long RIGHTS_PATH_FILESTAT_GET = 1 << 18;
    static final long RIGHTS_PATH_FILESTAT_SET_SIZE = 1 << 19;
    static final long RIGHTS_PATH_FILESTAT_SET_TIMES = 1 << 20;
    static final long RIGHTS_FD_FILESTAT_GET = 1 << 21;
    static final long RIGHTS_FD_FILESTAT_SET_SIZE = 1 << 22;
    static final long RIGHTS_FD_FILESTAT_SET_TIMES = 1 << 23;
    static final long RIGHTS_PATH_SYMLINK = 1 << 24;
    static final long RIGHTS_PATH_REMOVE_DIRECTORY = 1 << 25;
    static final long RIGHTS_PATH_UNLINK_FILE = 1 << 26;
    static final long RIGHTS_POLL_FD_READWRITE = 1 << 27;
    static final long RIGHTS_SOCK_SHUTDOWN = 1 << 28;
    static final long ILLEGAL_RIGHTS = -(RIGHTS_SOCK_SHUTDOWN << 1);

    static final int ADVICE_NORMAL = 0;
    static final int ADVICE_SEQUENTIAL = 1;
    static final int ADVICE_RANDOM = 2;
    static final int ADVICE_WILLNEED = 3;
    static final int ADVICE_DONTNEED = 4;
    static final int ADVICE_NOREUSE = 5;

    static final int CLOCKID_REALTIME = 0;
    static final int CLOCKID_MONOTONIC = 1;
    static final int CLOCKID_PROCESS_CPUTIME_ID = 2;
    static final int CLOCKID_THREAD_CPUTIME_ID = 3;
}
