package org.wastastic;

public final class InstructionImpls {
    private InstructionImpls() {}

    public static RuntimeException unreachable() throws TrapException {
        throw new TrapException();
    }
}
