package org.wastastic.wasi;

public interface Clock {
    long resolutionNanos() throws ErrnoException;

    long currentTimeNanos(long precisionNanos) throws ErrnoException;
}
