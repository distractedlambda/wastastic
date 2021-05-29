package org.wastastic;

import static java.util.Objects.requireNonNull;

public record Import(String moduleName, String name, ImportType type) {
    public Import {
        requireNonNull(moduleName);
        requireNonNull(name);
        requireNonNull(type);
    }
}
