package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.wastastic.Names.METHOD_HANDLE_INTERNAL_NAME;
import static org.wastastic.Names.OBJECT_INTERNAL_NAME;

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
            case FUNCREF -> METHOD_HANDLE_INTERNAL_NAME;
            case EXTERNREF -> OBJECT_INTERNAL_NAME;
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

    int zeroConstantOpcode() {
        return switch (this) {
            case I32 -> ICONST_0;
            case I64 -> LCONST_0;
            case F32 -> FCONST_0;
            case F64 -> DCONST_0;
            case FUNCREF, EXTERNREF -> ACONST_NULL;
        };
    }

    @NotNull Class<?> jvmType() {
        return switch (this) {
            case I32 -> int.class;
            case I64 -> long.class;
            case F32 -> float.class;
            case F64 -> double.class;
            case FUNCREF -> MethodHandle.class;
            case EXTERNREF -> Object.class;
        };
    }

    @NotNull String globalGetterDescriptor() {
        return switch (this) {
            case I32 -> "(Lorg/wastastic/GeneratedModuleInstance;)I";
            case I64 -> "(Lorg/wastastic/GeneratedModuleInstance;)J";
            case F32 -> "(Lorg/wastastic/GeneratedModuleInstance;)F";
            case F64 -> "(Lorg/wastastic/GeneratedModuleInstance;)D";
            case FUNCREF -> "(Lorg/wastastic/GeneratedModuleInstance;)Ljava/lang/invoke/MethodHandle;";
            case EXTERNREF -> "(Lorg/wastastic/GeneratedModuleInstance;)Ljava/lang/Object;";
        };
    }

    @NotNull String globalSetterDescriptor() {
        return switch (this) {
            case I32 -> "(ILorg/wastastic/GeneratedModuleInstance;)V";
            case I64 -> "(JLorg/wastastic/GeneratedModuleInstance;)V";
            case F32 -> "(FLorg/wastastic/GeneratedModuleInstance;)V";
            case F64 -> "(DLorg/wastastic/GeneratedModuleInstance;)V";
            case FUNCREF -> "(Ljava/lang/invoke/MethodHandle;Lorg/wastastic/GeneratedModuleInstance;)V";
            case EXTERNREF -> "(Ljava/lang/Object;Lorg/wastastic/GeneratedModuleInstance;)V";
        };
    }
}
