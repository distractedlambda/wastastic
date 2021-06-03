package org.wastastic.compiler;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

enum ValueType {
    I32,
    I64,
    F32,
    F64,
    FUNCREF,
    EXTERNREF;

    public @NotNull String getDescriptor() {
        return switch (this) {
            case I32 -> "I";
            case I64 -> "J";
            case F32 -> "F";
            case F64 -> "D";
            case FUNCREF -> "Ljava/lang/invoke/MethodHandle;";
            case EXTERNREF -> "Ljava/lang/Object;";
        };
    }

    public char getTupleSignatureCharacter() {
        return switch (this) {
            case I32 -> 'I';
            case I64 -> 'J';
            case F32 -> 'F';
            case F64 -> 'D';
            case FUNCREF, EXTERNREF -> 'L';
        };
    }

    public boolean isDoubleWidth() {
        return switch (this) {
            case I32, F32, FUNCREF, EXTERNREF -> false;
            case I64, F64 -> true;
        };
    }

    public int getWidth() {
        return switch (this) {
            case I32, F32, FUNCREF, EXTERNREF -> 1;
            case I64, F64 -> 2;
        };
    }

    public int getLocalLoadOpcode() {
        return switch (this) {
            case I32 -> Opcodes.ILOAD;
            case I64 -> Opcodes.LLOAD;
            case F32 -> Opcodes.FLOAD;
            case F64 -> Opcodes.DLOAD;
            case FUNCREF, EXTERNREF -> Opcodes.ALOAD;
        };
    }

    public int getLocalStoreOpcode() {
        return switch (this) {
            case I32 -> Opcodes.ISTORE;
            case I64 -> Opcodes.LSTORE;
            case F32 -> Opcodes.FSTORE;
            case F64 -> Opcodes.DSTORE;
            case FUNCREF, EXTERNREF -> Opcodes.ASTORE;
        };
    }
}
