package org.wastastic.translator;

import org.jetbrains.annotations.NotNull;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.LSTORE;

enum ValueType {
    I32,
    I64,
    F32,
    F64,
    FUNCREF,
    EXTERNREF;

    @NotNull String descriptor() {
        return switch (this) {
            case I32 -> "I";
            case I64 -> "J";
            case F32 -> "F";
            case F64 -> "D";
            case FUNCREF -> "Ljava/lang/invoke/MethodHandle;";
            case EXTERNREF -> "Ljava/lang/Object;";
        };
    }

    @NotNull String erasedDescriptor() {
        return switch (this) {
            case I32 -> "I";
            case I64 -> "J";
            case F32 -> "F";
            case F64 -> "D";
            case FUNCREF, EXTERNREF -> "Ljava/lang/Object;";
        };
    }

    char tupleSuffixChar() {
        return switch (this) {
            case I32 -> 'I';
            case I64 -> 'J';
            case F32 -> 'F';
            case F64 -> 'D';
            case FUNCREF, EXTERNREF -> 'L';
        };
    }

    boolean isDoubleWidth() {
        return switch (this) {
            case I32, F32, FUNCREF, EXTERNREF -> false;
            case I64, F64 -> true;
        };
    }

    int width() {
        return switch (this) {
            case I32, F32, FUNCREF, EXTERNREF -> 1;
            case I64, F64 -> 2;
        };
    }

    int localLoadOpcode() {
        return switch (this) {
            case I32 -> ILOAD;
            case I64 -> LLOAD;
            case F32 -> FLOAD;
            case F64 -> DLOAD;
            case FUNCREF, EXTERNREF -> ALOAD;
        };
    }

    int localStoreOpcode() {
        return switch (this) {
            case I32 -> ISTORE;
            case I64 -> LSTORE;
            case F32 -> FSTORE;
            case F64 -> DSTORE;
            case FUNCREF, EXTERNREF -> ASTORE;
        };
    }

    int returnOpcode() {
        return switch (this) {
            case I32 -> IRETURN;
            case I64 -> LRETURN;
            case F32 -> FRETURN;
            case F64 -> DRETURN;
            case FUNCREF, EXTERNREF -> ARETURN;
        };
    }
}
