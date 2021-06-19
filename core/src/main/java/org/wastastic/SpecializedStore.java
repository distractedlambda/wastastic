package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.wastastic.CodegenUtils.pushI64Constant;
import static org.wastastic.Names.GENERATED_INSTANCE_INTERNAL_NAME;
import static org.wastastic.Names.INTEGER_INTERNAL_NAME;
import static org.wastastic.Names.MEMORY_SEGMENT_DESCRIPTOR;
import static org.wastastic.Names.MODULE_INSTANCE_DESCRIPTOR;
import static org.wastastic.Names.VAR_HANDLE_DESCRIPTOR;
import static org.wastastic.Names.VAR_HANDLE_INTERNAL_NAME;

record SpecializedStore(@NotNull Op op, int memoryIndex, int offset) {
    enum Op {
        I32("i32", 'I', 'I'),
        I32_AS_I8("i32i8", 'I', 'B'),
        I32_AS_I16("i32i16", 'I', 'S'),
        I64("i64", 'J', 'J'),
        I64_AS_I8("i64i8", 'J', 'B'),
        I64_AS_I16("i64i16", 'J', 'S'),
        I64_AS_I32("i64i32", 'J', 'I'),
        F32("f32", 'F', 'F'),
        F64("f64", 'D', 'D');

        private final @NotNull String name;
        private final @NotNull String vhSetDescriptor;
        private final @NotNull String methodDescriptor;

        private final @NotNull String vhName;
        private final int moduleArgumentFieldIndex;
        private final int argumentLoadOpcode;
        private final @NotNull ValueType argumentValueType;

        Op(@NotNull String name, char argumentType, char storedType) {
            this.name = name;
            this.vhSetDescriptor = "(" + MEMORY_SEGMENT_DESCRIPTOR + "J" + storedType + ")V";
            this.methodDescriptor = "(I" + argumentType + MODULE_INSTANCE_DESCRIPTOR + ")V";

            this.vhName = switch (storedType) {
                case 'B' -> "VH_BYTE";
                case 'S' -> "VH_SHORT";
                case 'I' -> "VH_INT";
                case 'J' -> "VH_LONG";
                case 'F' -> "VH_FLOAT";
                case 'D' -> "VH_DOUBLE";
                default -> throw new IllegalArgumentException();
            };

            this.moduleArgumentFieldIndex = switch (argumentType) {
                case 'B', 'S', 'I', 'F' -> 2;
                case 'J', 'D' -> 3;
                default -> throw new IllegalArgumentException();
            };

            this.argumentLoadOpcode = switch (argumentType) {
                case 'B', 'S', 'I' -> ILOAD;
                case 'J' -> LLOAD;
                case 'F' -> FLOAD;
                case 'D' -> DLOAD;
                default -> throw new IllegalArgumentException();
            };

            this.argumentValueType = switch (argumentType) {
                case 'B', 'S', 'I' -> ValueType.I32;
                case 'J' -> ValueType.I64;
                case 'F' -> ValueType.F32;
                case 'D' -> ValueType.F64;
                default -> throw new IllegalArgumentException();
            };
        }
    }

    @NotNull String methodName() {
        return "s" + op.name + "m" + memoryIndex + "o" + offset;
    }

    @NotNull String methodDescriptor() {
        return op.methodDescriptor;
    }

    @NotNull ValueType argumentType() {
        return op.argumentValueType;
    }

    void writeMethod(@NotNull ClassVisitor classVisitor) {
        var writer = classVisitor.visitMethod(ACC_PRIVATE | ACC_STATIC, methodName(), op.methodDescriptor, null, null);
        writer.visitCode();

        writer.visitFieldInsn(GETSTATIC, Memory.INTERNAL_NAME, op.vhName, VAR_HANDLE_DESCRIPTOR);

        writer.visitVarInsn(ALOAD, op.moduleArgumentFieldIndex);
        writer.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
        writer.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, "m" + memoryIndex, Memory.DESCRIPTOR);
        writer.visitFieldInsn(GETFIELD, Memory.INTERNAL_NAME, "segment", MEMORY_SEGMENT_DESCRIPTOR);

        writer.visitVarInsn(ILOAD, 0);
        writer.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);

        if (offset != 0) {
            pushI64Constant(writer, Integer.toUnsignedLong(offset));
            writer.visitInsn(LADD);
        }

        writer.visitVarInsn(op.argumentLoadOpcode, 1);

        if (op == Op.I64_AS_I32) {
            writer.visitInsn(L2I);
        }

        writer.visitMethodInsn(INVOKEVIRTUAL, VAR_HANDLE_INTERNAL_NAME, "set", op.vhSetDescriptor, false);

        writer.visitInsn(RETURN);
        writer.visitMaxs(0, 0);
        writer.visitEnd();
    }
}
