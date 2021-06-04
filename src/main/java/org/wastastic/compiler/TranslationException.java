package org.wastastic.compiler;

public final class TranslationException extends Exception {
    public TranslationException() {
        super();
    }

    public TranslationException(String message) {
        super(message);
    }

    public TranslationException(Throwable cause) {
        super(cause);
    }

    public TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
