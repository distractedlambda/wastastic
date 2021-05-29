package org.wastastic;

import static java.util.Objects.requireNonNull;

public record MemoryType(Limits limits) implements ImportType {
    public MemoryType {
        requireNonNull(limits);
    }
}
