package org.wastastic;

import static java.util.Objects.requireNonNull;

public final class ModuleCompiler {
    private final ModuleReader reader;

    public ModuleCompiler(ModuleReader reader) {
        this.reader = requireNonNull(reader);
    }

    public ModuleReader getReader() {
        return reader;
    }
}
