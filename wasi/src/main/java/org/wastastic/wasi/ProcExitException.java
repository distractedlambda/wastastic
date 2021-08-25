package org.wastastic.wasi;

public final class ProcExitException extends RuntimeException {
    private final int code;

    ProcExitException(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}
