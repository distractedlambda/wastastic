package org.wastastic;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.wastastic.CodegenUtils.pushI64Constant;
import static org.wastastic.Names.BYTE_INTERNAL_NAME;
import static org.wastastic.Names.GENERATED_INSTANCE_INTERNAL_NAME;
import static org.wastastic.Names.INTEGER_INTERNAL_NAME;
import static org.wastastic.Names.MEMORY_SEGMENT_DESCRIPTOR;
import static org.wastastic.Names.MODULE_INSTANCE_DESCRIPTOR;
import static org.wastastic.Names.SHORT_INTERNAL_NAME;
import static org.wastastic.Names.VAR_HANDLE_DESCRIPTOR;
import static org.wastastic.Names.VAR_HANDLE_INTERNAL_NAME;

record SpecializedLoad(@NotNull Op op, int memoryIndex, int offset) {
    enum Op {
        I32("i32", 'I', 'I'),
        I8_AS_I32("i8i32", 'B', 'I'),
        I16_AS_I32("i16i32", 'S', 'I'),
        U8_AS_I32("u8i32", 'B', 'I'),
        U16_AS_I32("u16i32", 'S', 'I'),
        I64("i64", 'J', 'J'),
        I8_AS_I64("i8i64", 'B', 'J'),
        I16_AS_I64("i16i64", 'S', 'J'),
        I32_AS_I64("i32i64", 'I', 'J'),
        U8_AS_I64("u8i64", 'B', 'J'),
        U16_AS_I64("u16i64", 'S', 'J'),
        U32_AS_I64("u32i64", 'I', 'J'),
        F32("f32", 'F', 'F'),
        F64("f64", 'D', 'D');

        private final @NotNull String name;
        private final @NotNull String vhGetDescriptor;
        private final @NotNull String methodDescriptor;

        private final @NotNull String vhName;
        private final int returnOpcode;
        private final @NotNull ValueType returnValueType;

        Op(@NotNull String name, char loadedType, char returnType) {
            this.name = name;
            this.vhGetDescriptor = "(" + MEMORY_SEGMENT_DESCRIPTOR + "J)" + loadedType;
            this.methodDescriptor = "(I" + MODULE_INSTANCE_DESCRIPTOR + ")" + returnType;

            this.vhName = switch (loadedType) {
                case 'B' -> "VH_BYTE";
                case 'S' -> "VH_SHORT";
                case 'I' -> "VH_INT";
                case 'J' -> "VH_LONG";
                case 'F' -> "VH_FLOAT";
                case 'D' -> "VH_DOUBLE";
                default -> throw new IllegalArgumentException();
            };

            this.returnOpcode = switch (returnType) {
                case 'I' -> IRETURN;
                case 'J' -> LRETURN;
                case 'F' -> FRETURN;
                case 'D' -> DRETURN;
                default -> throw new IllegalArgumentException();
            };

            this.returnValueType = switch (returnType) {
                case 'I' -> ValueType.I32;
                case 'J' -> ValueType.I64;
                case 'F' -> ValueType.F32;
                case 'D' -> ValueType.F64;
                default -> throw new IllegalArgumentException();
            };
        }
    }

    @NotNull String methodName() {
        return "l" + op.name + "m" + memoryIndex + "o" + offset;
    }

    @NotNull String methodDescriptor() {
        return op.methodDescriptor;
    }

    @NotNull ValueType returnType() {
        return op.returnValueType;
    }

    void writeMethod(@NotNull ClassVisitor classVisitor) {
        var writer = classVisitor.visitMethod(ACC_PRIVATE | ACC_STATIC, methodName(), op.methodDescriptor, null, null);
        writer.visitCode();

        writer.visitFieldInsn(GETSTATIC, Memory.INTERNAL_NAME, op.vhName, VAR_HANDLE_DESCRIPTOR);

        writer.visitVarInsn(ALOAD, 1);
        writer.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
        writer.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, "m" + memoryIndex, Memory.DESCRIPTOR);
        writer.visitFieldInsn(GETFIELD, Memory.INTERNAL_NAME, Memory.SEGMENT_FIELD_NAME, Memory.SEGMENT_FIELD_DESCRIPTOR);

        writer.visitVarInsn(ILOAD, 0);
        writer.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);

        if (offset != 0) {
            pushI64Constant(writer, Integer.toUnsignedLong(offset));
            writer.visitInsn(LADD);
        }

        writer.visitMethodInsn(INVOKEVIRTUAL, VAR_HANDLE_INTERNAL_NAME, "get", op.vhGetDescriptor, false);

        switch (op) {
            case U8_AS_I32 -> {
                writer.visitMethodInsn(INVOKESTATIC, BYTE_INTERNAL_NAME, "toUnsignedInt", "(B)I", false);
            }

            case U16_AS_I32 -> {
                writer.visitMethodInsn(INVOKESTATIC, SHORT_INTERNAL_NAME, "toUnsignedInt", "(S)I", false);
            }

            case U8_AS_I64 -> {
                writer.visitMethodInsn(INVOKESTATIC, BYTE_INTERNAL_NAME, "toUnsignedLong", "(B)J", false);
            }

            case U16_AS_I64 -> {
                writer.visitMethodInsn(INVOKESTATIC, SHORT_INTERNAL_NAME, "toUnsignedLong", "(S)J", false);
            }

            case I8_AS_I64, I16_AS_I64, I32_AS_I64 -> {
                writer.visitInsn(I2L);
            }

            case U32_AS_I64 -> {
                writer.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);
            }
        }

        writer.visitInsn(op.returnOpcode);
        writer.visitMaxs(0, 0);
        writer.visitEnd();
    }
}
