package org.wastastic;

public final class CompilationException extends Exception {
    public CompilationException() {
        super();
    }

    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(Throwable cause) {
        super(cause);
    }

    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }
}
