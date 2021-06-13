package org.wastastic;

public final class TranslationException extends Exception {
    TranslationException() {
        super();
    }

    TranslationException(String message) {
        super(message);
    }

    TranslationException(Throwable cause) {
        super(cause);
    }

    TranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
