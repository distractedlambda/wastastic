package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

record MemoryType(Limits limits) {
    MemoryType {
        requireNonNull(limits);
    }
}
