package org.wastastic;

import java.util.Objects;

public abstract class Module {
    protected final Memory memory;

    protected Module(Memory memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    public final Memory getMemory() {
        return memory;
    }
}