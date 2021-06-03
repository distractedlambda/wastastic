package org.wastastic.compiler;

import org.objectweb.asm.Opcodes;

import static java.util.Objects.requireNonNull;

enum ValueType {
    I32("I", false, 1, Opcodes.ILOAD, Opcodes.ISTORE),
    I64("J", true, 2, Opcodes.LLOAD, Opcodes.LSTORE),
    F32("F", false, 1, Opcodes.FLOAD, Opcodes.FSTORE),
    F64("D", true, 2, Opcodes.DLOAD, Opcodes.DSTORE),
    FUNCREF("Ljava/lang/invoke/MethodHandle;", false, 1, Opcodes.ALOAD, Opcodes.ASTORE),
    EXTERNREF("Ljava/lang/Object;", false, 1, Opcodes.ALOAD, Opcodes.ASTORE);

    private final String descriptor;
    private final boolean doubleWidth;
    private final int width;
    private final int localLoadOpcode;
    private final int localStoreOpcode;

    ValueType(String descriptor, boolean doubleWidth, int width, int localLoadOpcode, int localStoreOpcode) {
        this.descriptor = requireNonNull(descriptor);
        this.doubleWidth = doubleWidth;
        this.width = width;
        this.localLoadOpcode = localLoadOpcode;
        this.localStoreOpcode = localStoreOpcode;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public boolean isDoubleWidth() {
        return doubleWidth;
    }

    public int getWidth() {
        return width;
    }

    public int getLocalLoadOpcode() {
        return localLoadOpcode;
    }

    public int getLocalStoreOpcode() {
        return localStoreOpcode;
    }
}
