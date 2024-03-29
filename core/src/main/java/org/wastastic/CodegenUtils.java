package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ACMPEQ;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.SIPUSH;

final class CodegenUtils {
    private CodegenUtils() {}

    static int oppositeBranchOpcode(int opcode) {
        return switch (opcode) {
            case IFEQ -> IFNE;
            case IFNE -> IFEQ;
            case IFGE -> IFLT;
            case IFLT -> IFGE;
            case IFGT -> IFLE;
            case IFLE -> IFGT;
            case IFNONNULL -> IFNULL;
            case IFNULL -> IFNONNULL;
            case IF_ICMPEQ -> IF_ICMPNE;
            case IF_ICMPNE -> IF_ICMPEQ;
            case IF_ICMPGE -> IF_ICMPLT;
            case IF_ICMPLT -> IF_ICMPGE;
            case IF_ICMPGT -> IF_ICMPLE;
            case IF_ICMPLE -> IF_ICMPGT;
            case IF_ACMPEQ -> IF_ACMPNE;
            case IF_ACMPNE -> IF_ACMPEQ;
            default -> throw new IllegalArgumentException();
        };
    }

    static void pushI32Constant(@NotNull MethodVisitor visitor, int value) {
        if (value >= -1 && value <= 5) {
            visitor.visitInsn(ICONST_0 + value);
        }
        else if (value == (byte) value) {
            visitor.visitIntInsn(BIPUSH, value);
        }
        else if (value == (short) value) {
            visitor.visitIntInsn(SIPUSH, value);
        }
        else {
            visitor.visitLdcInsn(value);
        }
    }

    static void pushI64Constant(@NotNull MethodVisitor visitor, long value) {
        if (value == 0 || value == 1) {
            visitor.visitInsn(LCONST_0 + (int) value);
        }
        else if (value >= -1 && value <= 5) {
            visitor.visitInsn(ICONST_0 + (int) value);
            visitor.visitInsn(I2L);
        }
        else if (value == (byte) value) {
            visitor.visitIntInsn(BIPUSH, (int) value);
            visitor.visitInsn(I2L);
        }
        else if (value == (short) value) {
            visitor.visitIntInsn(SIPUSH, (int) value);
            visitor.visitInsn(I2L);
        }
        else {
            visitor.visitLdcInsn(value);
        }
    }

    static void pushF32Constant(@NotNull MethodVisitor visitor, float value) {
        if (value == 0) {
            visitor.visitInsn(FCONST_0);
        }
        else if (value == 1) {
            visitor.visitInsn(FCONST_1);
        }
        else if (value == 2) {
            visitor.visitInsn(FCONST_2);
        }
        else if (value == -1) {
            visitor.visitInsn(ICONST_M1);
            visitor.visitInsn(I2F);
        }
        else if (value == 3) {
            visitor.visitInsn(ICONST_3);
            visitor.visitInsn(I2F);
        }
        else if (value == 4) {
            visitor.visitInsn(ICONST_4);
            visitor.visitInsn(I2F);
        }
        else if (value == 5) {
            visitor.visitInsn(ICONST_5);
            visitor.visitInsn(I2F);
        }
        else if (value == (float) (byte) value) {
            visitor.visitIntInsn(BIPUSH, (byte) value);
            visitor.visitInsn(I2F);
        }
        else if (value == (float) (short) value) {
            visitor.visitIntInsn(SIPUSH, (short) value);
            visitor.visitInsn(I2F);
        }
        else {
            visitor.visitLdcInsn(value);
        }
    }

    static void pushF64Constant(@NotNull MethodVisitor visitor, double value) {
        if (value == 0) {
            visitor.visitInsn(DCONST_0);
        }
        else if (value == 1) {
            visitor.visitInsn(DCONST_1);
        }
        else if (value == -1) {
            visitor.visitInsn(ICONST_M1);
            visitor.visitInsn(I2D);
        }
        else if (value == 2) {
            visitor.visitInsn(ICONST_2);
            visitor.visitInsn(I2D);
        }
        else if (value == 3) {
            visitor.visitInsn(ICONST_3);
            visitor.visitInsn(I2D);
        }
        else if (value == 4) {
            visitor.visitInsn(ICONST_4);
            visitor.visitInsn(I2D);
        }
        else if (value == 5) {
            visitor.visitInsn(ICONST_5);
            visitor.visitInsn(I2D);
        }
        else if (value == (double) (byte) value) {
            visitor.visitIntInsn(BIPUSH, (byte) value);
            visitor.visitInsn(I2D);
        }
        else if (value == (double) (short) value) {
            visitor.visitIntInsn(SIPUSH, (short) value);
            visitor.visitInsn(I2D);
        }
        else {
            visitor.visitLdcInsn(value);
        }
    }
}
