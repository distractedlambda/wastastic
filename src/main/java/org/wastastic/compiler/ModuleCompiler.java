package org.wastastic.compiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

import static java.lang.Integer.toUnsignedLong;
import static java.util.Objects.requireNonNull;

final class ModuleCompiler {
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private final String internalName;
    private final ModuleReader reader;
    private final ClassVisitor clazz;

    private final ArrayList<FunctionType> types = new ArrayList<>();
    private final ArrayList<Function> functions = new ArrayList<>();
    private final ArrayList<Table> tables = new ArrayList<>();
    private final ArrayList<Memory> memories = new ArrayList<>();
    private final ArrayList<Global> globals = new ArrayList<>();

    private int scratchLocalsOffset;
    private int[] localIndices = EMPTY_INT_ARRAY;
    private final ArrayDeque<ValueType> operandTypes = new ArrayDeque<>();
    private final ArrayList<BranchTarget> branchTargets = new ArrayList<>();

    private MethodVisitor method;

    ModuleCompiler(String internalName, ModuleReader reader, ClassVisitor clazz) {
        this.internalName = requireNonNull(internalName);
        this.reader = requireNonNull(reader);
        this.clazz = requireNonNull(clazz);
    }

    void compile() throws CompilationException, IOException {
        if (reader.nextByte() != 0x00 ||
            reader.nextByte() != 0x61 ||
            reader.nextByte() != 0x73 ||
            reader.nextByte() != 0x6d
        ) {
            throw new CompilationException("Invalid magic number");
        }

        if (reader.nextByte() != 0x01 ||
            reader.nextByte() != 0x00 ||
            reader.nextByte() != 0x00 ||
            reader.nextByte() != 0x00
        ) {
            throw new CompilationException("Unsupported version");
        }

        while (true) {
            byte sectionId;
            try {
                sectionId = reader.nextByte();
            } catch (EOFException ignored) {
                break;
            }

            var unsignedSectionSize = reader.nextUnsigned32();

            switch (sectionId) {
                case SECTION_CUSTOM -> reader.skip(unsignedSectionSize);
                case SECTION_TYPE -> compileTypeSection();
                case SECTION_IMPORT -> compileImportSection();
                case SECTION_FUNCTION -> compileFunctionSection();
                case SECTION_TABLE -> compileTableSection();
                case SECTION_MEMORY -> compileMemorySection();
                default -> throw new CompilationException("Unrecognized section ID: " + sectionId);
            }
        }
    }

    private void compileUntilEnd() throws IOException, CompilationException {
        for (var opcode = reader.nextByte();; opcode = reader.nextByte()) switch (opcode) {
            case OP_UNREACHABLE -> compileUnreachable();
            case OP_NOP -> compileNop();
            case OP_RETURN -> compileReturn();
            case OP_I32_LOAD -> compileI32Load();
            case OP_I32_STORE -> compileI32Store();
            case OP_I64_LOAD -> compileI64Load();
            case OP_I64_STORE -> compileI64Store();
            case OP_DROP -> compileDrop();
            case OP_SELECT -> compileSelect();
            case OP_CALL -> compileCall();
            case OP_BLOCK -> compileBlock();
            case OP_LOOP -> compileLoop();
            case OP_END -> {
                // FIXME: pop a label
                return;
            }
        }
    }

    private BranchTarget nextBranchTarget() throws IOException {
        return branchTargets.get(branchTargets.size() - 1 - reader.nextUnsigned32());
    }

    private void compileConditionalBranch() throws IOException {
        var pastBranchLabel = new Label();
        method.visitJumpInsn(Opcodes.IFEQ, pastBranchLabel);
        compileBranch();
        method.visitLabel(pastBranchLabel);
    }

    private void compileBranch() throws IOException {
        // FIXME: don't actually pop from stack

        var target = nextBranchTarget();
        var localOffset = scratchLocalsOffset;

        for (var i = 0; i < target.getParameterCount(); i++) switch (operandTypes.pop()) {
            case I32 -> {
                method.visitVarInsn(Opcodes.ISTORE, localOffset);
                localOffset += 1;
            }

            case F32 -> {
                method.visitVarInsn(Opcodes.FSTORE, localOffset);
                localOffset += 1;
            }

            case FUNCREF, EXTERNREF -> {
                method.visitVarInsn(Opcodes.ASTORE, localOffset);
                localOffset += 1;
            }

            case I64 -> {
                method.visitVarInsn(Opcodes.LSTORE, localOffset);
                localOffset += 2;
            }

            case F64 -> {
                method.visitVarInsn(Opcodes.DSTORE, localOffset);
                localOffset += 2;
            }
        }

        while (operandTypes.size() > target.operandStackDepth()) {
            if (operandTypes.pop().isDoubleWidth()) {
                method.visitInsn(Opcodes.POP2);
            } else {
                method.visitInsn(Opcodes.POP);
            }
        }

        for (var parameterType : target.getParameterTypeList()) switch (parameterType) {
            case I32 -> method.visitVarInsn(Opcodes.ILOAD, (localOffset -= 1));
            case F32 -> method.visitVarInsn(Opcodes.FLOAD, (localOffset -= 1));
            case FUNCREF, EXTERNREF -> method.visitVarInsn(Opcodes.ALOAD, (localOffset -= 1));
            case I64 -> method.visitVarInsn(Opcodes.LLOAD, (localOffset -= 2));
            case F64 -> method.visitVarInsn(Opcodes.DLOAD, (localOffset -= 2));
        }

        method.visitJumpInsn(Opcodes.GOTO, target.label());
    }

    private void compileLoop() throws CompilationException, IOException {
        var type = nextBlockType();
        var startLabel = new Label();
        branchTargets.add(
            new BranchTarget(
                startLabel,
                type.getParameterTypes(),
                operandTypes.size() - type.getParameterCount()
            )
        );
        method.visitLabel(startLabel);
        compileUntilEnd();
        branchTargets.remove(branchTargets.size() - 1);
    }

    private void compileBlock() throws CompilationException, IOException {
        var type = nextBlockType();
        var endLabel = new Label();
        branchTargets.add(
            new BranchTarget(
                endLabel,
                type.getReturnTypes(),
                operandTypes.size() - type.getParameterCount()
            )
        );
        compileUntilEnd();
        branchTargets.remove(branchTargets.size() - 1);
        method.visitLabel(endLabel);
    }

    private FunctionType nextBlockType() throws IOException, CompilationException {
        var code = reader.nextSigned33();
        if (code >= 0) {
            return types.get((int) code);
        } else return switch ((byte) (code & 0x7F)) {
            case 0x40 -> new FunctionType(null, null);
            case TYPE_I32 -> new FunctionType(null, ValueType.I32);
            case TYPE_I64 -> new FunctionType(null, ValueType.I64);
            case TYPE_F32 -> new FunctionType(null, ValueType.F32);
            case TYPE_F64 -> new FunctionType(null, ValueType.F64);
            case TYPE_EXTERNREF -> new FunctionType(null, ValueType.EXTERNREF);
            case TYPE_FUNCREF -> new FunctionType(null, ValueType.FUNCREF);
            default -> throw new CompilationException("Invalid block type");
        };
    }

    private void compileUnreachable() {
        emitTrap();
    }

    private void compileNop() {
        // Nothing to do
    }

    private void compileReturn() {
        throw new UnsupportedOperationException("TODO");
    }

    private void compileI32Load() throws IOException {
        emitEffectiveAddress(nextMemArg().unsignedOffset());
        emitGetMemory();
        method.visitInsn(Opcodes.SWAP);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/wastastic/Memory", "loadInt", "(I)I", true);
        operandTypes.push(ValueType.I32);
    }

    private void compileI32Store() throws IOException {
        operandTypes.pop();
        operandTypes.pop();

        method.visitInsn(Opcodes.SWAP);
        emitEffectiveAddress(nextMemArg().unsignedOffset());
        method.visitInsn(Opcodes.SWAP);
        emitGetMemory();
        method.visitInsn(Opcodes.DUP_X2);
        method.visitInsn(Opcodes.POP);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/wastastic/Memory", "storeInt", "(II)V", true);
    }

    private void compileI64Load() throws IOException {
        emitEffectiveAddress(nextMemArg().unsignedOffset());
        emitGetMemory();
        method.visitInsn(Opcodes.SWAP);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/wastastic/Memory", "loadLong", "(I)J", true);
        operandTypes.push(ValueType.I64);
    }

    private void compileI64Store() throws IOException {
        operandTypes.pop();
        operandTypes.pop();

        method.visitInsn(Opcodes.DUP2_X1);
        method.visitInsn(Opcodes.POP2);
        emitEffectiveAddress(nextMemArg().unsignedOffset());
        emitGetMemory();
        method.visitInsn(Opcodes.SWAP);
        method.visitInsn(Opcodes.DUP2_X2);
        method.visitInsn(Opcodes.POP2);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/wastastic/Memory", "storeLong", "(IJ)V", true);
    }

    private void compileCall() {
        throw new UnsupportedOperationException("TODO");
    }

    private void compileSelect() {
        var noSwapLabel = new Label();
        method.visitJumpInsn(Opcodes.IFNE, noSwapLabel);

        operandTypes.pop();

        if (operandTypes.pop().isDoubleWidth()) {
            method.visitInsn(Opcodes.DUP2_X2);
            method.visitInsn(Opcodes.POP2);
            method.visitLabel(noSwapLabel);
            method.visitInsn(Opcodes.POP2);
        } else {
            method.visitInsn(Opcodes.SWAP);
            method.visitLabel(noSwapLabel);
            method.visitInsn(Opcodes.POP);
        }

        operandTypes.pop();
    }

    private void compileDrop() {
        method.visitInsn(operandTypes.pop().isDoubleWidth() ? Opcodes.POP2 : Opcodes.POP);
    }

    private void emitGetMemory() {
        method.visitFieldInsn(Opcodes.GETFIELD, "org/wastastic/Module", "memory", "Lorg/wastastic/Memory;");
    }

    private void emitZextIntToLong() {
        method.visitInsn(Opcodes.I2L);
        method.visitLdcInsn(0xffffffffL);
        method.visitInsn(Opcodes.LAND);
    }

    private void emitEffectiveAddress(int unsignedOffset) {
        if (unsignedOffset != 0) {
            var pastTrapLabel = new Label();
            emitZextIntToLong();
            emitLongConstant(toUnsignedLong(unsignedOffset));
            method.visitInsn(Opcodes.LADD);
            method.visitLdcInsn(0xffffffffL);
            method.visitInsn(Opcodes.LCMP);
            method.visitJumpInsn(Opcodes.IFLE, pastTrapLabel);
            emitTrap();
            method.visitLabel(pastTrapLabel);
        }
    }

    private void emitTrap() {
        method.visitTypeInsn(Opcodes.NEW, "org/wastastic/TrapException");
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/wastastic/TrapException", "<init>", "()V", false);
        method.visitInsn(Opcodes.ATHROW);
    }

    private MemArg nextMemArg() throws IOException {
        return new MemArg(reader.nextUnsigned32(), reader.nextUnsigned32());
    }

    private void emitIntConstant(int value) {
        switch (value) {
            case 0 -> method.visitInsn(Opcodes.ICONST_0);
            case 1 -> method.visitInsn(Opcodes.ICONST_1);
            case 2 -> method.visitInsn(Opcodes.ICONST_2);
            case 3 -> method.visitInsn(Opcodes.ICONST_3);
            case 4 -> method.visitInsn(Opcodes.ICONST_4);
            case 5 -> method.visitInsn(Opcodes.ICONST_5);
            case -1 -> method.visitInsn(Opcodes.ICONST_M1);
            default -> method.visitLdcInsn(value);
        }
    }

    private void emitLongConstant(long value) {
        if (value == 0) {
            method.visitInsn(Opcodes.LCONST_0);
        } else if (value == 1) {
            method.visitInsn(Opcodes.LCONST_1);
        } else {
            method.visitLdcInsn(value);
        }
    }

    private void compileMemorySection() throws IOException, CompilationException {
        var remaining = reader.nextUnsigned32();
        memories.ensureCapacity(memories.size() + remaining);
        for (; remaining != 0; remaining--) {
            memories.add(nextMemoryType());
        }
    }

    private void compileTableSection() throws CompilationException, IOException {
        var remaining = reader.nextUnsigned32();
        tables.ensureCapacity(tables.size() + remaining);
        for (; remaining != 0; remaining--) {
            tables.add(nextTableType());
        }
    }

    private void compileFunctionSection() throws IOException {
        var remaining = reader.nextUnsigned32();
        functions.ensureCapacity(functions.size() + remaining);
        for (; remaining != 0; remaining--) {
            functions.add(nextIndexedType());
        }
    }

    private FunctionType nextIndexedType() throws IOException {
        return types.get(reader.nextUnsigned32());
    }

    private void compileImportSection() throws CompilationException, IOException {
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            var moduleName = nextName();
            var name = nextName();
            switch (reader.nextByte()) {
                case 0x00 -> functions.add(new ImportedFunction(moduleName, name, nextIndexedType()));
                case 0x01 -> tables.add(new ImportedTable(moduleName, name, nextTableType()));
                case 0x02 -> memories.add(new ImportedMemory(moduleName, name, nextMemoryType()));
                case 0x03 -> globals.add(new ImportedGlobal(moduleName, name, nextGlobalType()));
                default -> throw new CompilationException("Invalid import description");
            }
        }
    }

    private GlobalType nextGlobalType() throws CompilationException, IOException {
        return new GlobalType(nextValueType(), nextMutability());
    }

    private Mutability nextMutability() throws CompilationException, IOException {
        return switch (reader.nextByte()) {
            case 0x00 -> Mutability.CONST;
            case 0x01 -> Mutability.VAR;
            default -> throw new CompilationException("Invalid mutability");
        };
    }

    private MemoryType nextMemoryType() throws CompilationException, IOException {
        return new MemoryType(nextLimits());
    }

    private TableType nextTableType() throws CompilationException, IOException {
        return new TableType(nextReferenceType(), nextLimits());
    }

    private ReferenceType nextReferenceType() throws IOException, CompilationException {
        return switch (reader.nextByte()) {
            case 0x6F -> ReferenceType.FUNCREF;
            case 0x70 -> ReferenceType.EXTERNREF;
            default -> throw new CompilationException("Invalid reference type");
        };
    }

    private Limits nextLimits() throws IOException, CompilationException {
        return switch (reader.nextByte()) {
            case 0x00 -> new Limits(reader.nextUnsigned32());
            case 0x01 -> new Limits(reader.nextUnsigned32(), reader.nextUnsigned32());
            default -> throw new CompilationException("Invalid limits encoding");
        };
    }

    private String nextName() throws IOException {
        return reader.nextUtf8(reader.nextUnsigned32());
    }

    private void compileTypeSection() throws CompilationException, IOException {
        var remaining = reader.nextUnsigned32();
        types.ensureCapacity(types.size() + remaining);
        for (; remaining != 0; remaining--) {
            types.add(nextFunctionType());
        }
    }

    private FunctionType nextFunctionType() throws CompilationException, IOException {
        if (reader.nextByte() != TYPE_FUNCTION) {
            throw new CompilationException("Invalid function type");
        }

        return new FunctionType(nextValueTypeVector(), nextValueTypeVector());
    }

    private Object nextValueTypeVector() throws CompilationException, IOException {
        var unsignedSize = reader.nextUnsigned32();
        switch (unsignedSize) {
            case 0:
                return null;
            case 1:
                return nextValueType();
            default: {
                var array = new ValueType[unsignedSize];

                for (var i = 0; i != array.length; i++) {
                    array[i] = nextValueType();
                }

                return array;
            }
        }
    }

    private ValueType nextValueType() throws CompilationException, IOException {
        return switch (reader.nextByte()) {
            case TYPE_EXTERNREF -> ValueType.EXTERNREF;
            case TYPE_FUNCREF -> ValueType.FUNCREF;
            case TYPE_F64 -> ValueType.F64;
            case TYPE_F32 -> ValueType.F32;
            case TYPE_I64 -> ValueType.I64;
            case TYPE_I32 -> ValueType.I32;
            default -> throw new CompilationException("Invalid value type");
        };
    }

    private static final byte SECTION_CUSTOM = 0;
    private static final byte SECTION_TYPE = 1;
    private static final byte SECTION_IMPORT = 2;
    private static final byte SECTION_FUNCTION = 3;
    private static final byte SECTION_TABLE = 4;
    private static final byte SECTION_MEMORY = 5;
    private static final byte SECTION_GLOBAL = 6;
    private static final byte SECTION_EXPORT = 7;
    private static final byte SECTION_START = 8;
    private static final byte SECTION_ELEMENT = 9;
    private static final byte SECTION_CODE = 10;
    private static final byte SECTION_DATA = 11;
    private static final byte SECTION_DATA_COUNT = 12;

    private static final byte TYPE_FUNCTION = 0x60;
    private static final byte TYPE_EXTERNREF = 0x6f;
    private static final byte TYPE_FUNCREF = 0x70;
    private static final byte TYPE_F64 = 0x7c;
    private static final byte TYPE_F32 = 0x7d;
    private static final byte TYPE_I64 = 0x7e;
    private static final byte TYPE_I32 = 0x7f;

    private static final byte OP_UNREACHABLE = 0x00;
    private static final byte OP_NOP = 0x01;
    private static final byte OP_BLOCK = 0x02;
    private static final byte OP_LOOP = 0x03;
    private static final byte OP_IF = 0x04;
    private static final byte OP_ELSE = 0x05;
    private static final byte OP_END = 0x0b;
    private static final byte OP_BR = 0x0c;
    private static final byte OP_BR_IF = 0x0d;
    private static final byte OP_BR_TABLE = 0x0e;
    private static final byte OP_RETURN = 0x0f;
    private static final byte OP_CALL = 0x10;
    private static final byte OP_CALL_INDIRECT = 0x11;
    private static final byte OP_DROP = 0x1a;
    private static final byte OP_SELECT = 0x1b;
    private static final byte OP_SELECT_VEC = 0x1c;
    private static final byte OP_LOCAL_GET = 0x20;
    private static final byte OP_LOCAL_SET = 0x21;
    private static final byte OP_LOCAL_TEE = 0x22;
    private static final byte OP_GLOBAL_GET = 0x23;
    private static final byte OP_GLOBAL_SET = 0x24;
    private static final byte OP_TABLE_GET = 0x25;
    private static final byte OP_TABLE_SET = 0x26;
    private static final byte OP_I32_LOAD = 0x28;
    private static final byte OP_I64_LOAD = 0x29;
    private static final byte OP_F32_LOAD = 0x2a;
    private static final byte OP_F64_LOAD = 0x2b;
    private static final byte OP_I32_LOAD8_S = 0x2c;
    private static final byte OP_I32_LOAD8_U = 0x2d;
    private static final byte OP_I32_LOAD16_S = 0x2e;
    private static final byte OP_I32_LOAD16_U = 0x2f;
    private static final byte OP_I64_LOAD8_S = 0x30;
    private static final byte OP_I64_LOAD8_U = 0x31;
    private static final byte OP_I64_LOAD16_S = 0x32;
    private static final byte OP_I64_LOAD16_U = 0x33;
    private static final byte OP_I64_LOAD32_S = 0x34;
    private static final byte OP_I64_LOAD32_U = 0x35;
    private static final byte OP_I32_STORE = 0x36;
    private static final byte OP_I64_STORE = 0x37;
    private static final byte OP_F32_STORE = 0x38;
    private static final byte OP_F64_STORE = 0x39;
    private static final byte OP_I32_STORE8 = 0x3a;
    private static final byte OP_I32_STORE16 = 0x3b;
    private static final byte OP_I64_STORE8 = 0x3c;
    private static final byte OP_I64_STORE16 = 0x3d;
    private static final byte OP_I64_STORE32 = 0x3e;
    private static final byte OP_MEMORY_SIZE = 0x3f;
    private static final byte OP_MEMORY_GROW = 0x40;
    private static final byte OP_I32_CONST = 0x41;
    private static final byte OP_I64_CONST = 0x42;
    private static final byte OP_F32_CONST = 0x43;
    private static final byte OP_F64_CONST = 0x44;
    private static final byte OP_I32_EQZ = 0x45;
    private static final byte OP_I32_EQ = 0x46;
    private static final byte OP_I32_NE = 0x47;
    private static final byte OP_I32_LT_S = 0x48;
    private static final byte OP_I32_LT_U = 0x49;
    private static final byte OP_I32_GT_S = 0x4a;
    private static final byte OP_I32_GT_U = 0x4b;
    private static final byte OP_I32_LE_S = 0x4c;
    private static final byte OP_I32_LE_U = 0x4d;
    private static final byte OP_I32_GE_S = 0x4e;
    private static final byte OP_I32_GE_U = 0x4f;
    private static final byte OP_I64_EQZ = 0x50;
    private static final byte OP_I64_EQ = 0x51;
    private static final byte OP_I64_NE = 0x52;
    private static final byte OP_I64_LT_S = 0x53;
    private static final byte OP_I64_LT_U = 0x54;
    private static final byte OP_I64_GT_S = 0x55;
    private static final byte OP_I64_GT_U = 0x56;
    private static final byte OP_I64_LE_S = 0x57;
    private static final byte OP_I64_LE_U = 0x58;
    private static final byte OP_I64_GE_S = 0x59;
    private static final byte OP_I64_GE_U = 0x5a;
    private static final byte OP_F32_EQ = 0x5b;
    private static final byte OP_F32_NE = 0x5c;
    private static final byte OP_F32_LT = 0x5d;
    private static final byte OP_F32_GT = 0x5e;
    private static final byte OP_F32_LE = 0x5f;
    private static final byte OP_F32_GE = 0x60;
    private static final byte OP_F64_EQ = 0x61;
    private static final byte OP_F64_NE = 0x62;
    private static final byte OP_F64_LT = 0x63;
    private static final byte OP_F64_GT = 0x64;
    private static final byte OP_F64_LE = 0x65;
    private static final byte OP_F64_GE = 0x66;
    private static final byte OP_I32_CLZ = 0x67;
    private static final byte OP_I32_CTZ = 0x68;
    private static final byte OP_I32_POPCNT = 0x69;
    private static final byte OP_I32_ADD = 0x6a;
    private static final byte OP_I32_SUB = 0x6b;
    private static final byte OP_I32_MUL = 0x6c;
    private static final byte OP_I32_DIV_S = 0x6d;
    private static final byte OP_I32_DIV_U = 0x6e;
    private static final byte OP_I32_REM_S = 0x6f;
    private static final byte OP_I32_REM_U = 0x70;
    private static final byte OP_I32_AND = 0x71;
    private static final byte OP_I32_OR = 0x72;
    private static final byte OP_I32_XOR = 0x73;
    private static final byte OP_I32_SHL = 0x74;
    private static final byte OP_I32_SHR_S = 0x75;
    private static final byte OP_I32_SHR_U = 0x76;
    private static final byte OP_I32_ROTL = 0x77;
    private static final byte OP_I32_ROTR = 0x78;
    private static final byte OP_I64_CLZ = 0x79;
    private static final byte OP_I64_CTZ = 0x7a;
    private static final byte OP_I64_POPCNT = 0x7b;
    private static final byte OP_I64_ADD = 0x7c;
    private static final byte OP_I64_SUB = 0x7d;
    private static final byte OP_I64_MUL = 0x7e;
    private static final byte OP_I64_DIV_S = 0x7f;
    private static final byte OP_I64_DIV_U = (byte) 0x80;
    private static final byte OP_I64_REM_S = (byte) 0x81;
    private static final byte OP_I64_REM_U = (byte) 0x82;
    private static final byte OP_I64_AND = (byte) 0x83;
    private static final byte OP_I64_OR = (byte) 0x84;
    private static final byte OP_I64_XOR = (byte) 0x85;
    private static final byte OP_I64_SHL = (byte) 0x86;
    private static final byte OP_I64_SHR_S = (byte) 0x87;
    private static final byte OP_I64_SHR_U = (byte) 0x88;
    private static final byte OP_I64_ROTL = (byte) 0x89;
    private static final byte OP_I64_ROTR = (byte) 0x8a;
    private static final byte OP_F32_ABS = (byte) 0x8b;
    private static final byte OP_F32_NEG = (byte) 0x8c;
    private static final byte OP_F32_CEIL = (byte) 0x8d;
    private static final byte OP_F32_FLOOR = (byte) 0x8e;
    private static final byte OP_F32_TRUNC = (byte) 0x8f;
    private static final byte OP_F32_NEAREST = (byte) 0x90;
    private static final byte OP_F32_SQRT = (byte) 0x91;
    private static final byte OP_F32_ADD = (byte) 0x92;
    private static final byte OP_F32_SUB = (byte) 0x93;
    private static final byte OP_F32_MUL = (byte) 0x94;
    private static final byte OP_F32_DIV = (byte) 0x95;
    private static final byte OP_F32_MIN = (byte) 0x96;
    private static final byte OP_F32_MAX = (byte) 0x97;
    private static final byte OP_F32_COPYSIGN = (byte) 0x98;
    private static final byte OP_F64_ABS = (byte) 0x99;
    private static final byte OP_F64_NEG = (byte) 0x9a;
    private static final byte OP_F64_CEIL = (byte) 0x9b;
    private static final byte OP_F64_FLOOR = (byte) 0x9c;
    private static final byte OP_F64_TRUNC = (byte) 0x9d;
    private static final byte OP_F64_NEAREST = (byte) 0x9e;
    private static final byte OP_F64_SQRT = (byte) 0x9f;
    private static final byte OP_F64_ADD = (byte) 0xa0;
    private static final byte OP_F64_SUB = (byte) 0xa1;
    private static final byte OP_F64_MUL = (byte) 0xa2;
    private static final byte OP_F64_DIV = (byte) 0xa3;
    private static final byte OP_F64_MIN = (byte) 0xa4;
    private static final byte OP_F64_MAX = (byte) 0xa5;
    private static final byte OP_F64_COPYSIGN = (byte) 0xa6;
    private static final byte OP_I32_WRAP_I64 = (byte) 0xa7;
    private static final byte OP_I32_TRUNC_F32_S = (byte) 0xa8;
    private static final byte OP_I32_TRUNC_F32_U = (byte) 0xa9;
    private static final byte OP_I32_TRUNC_F64_S = (byte) 0xaa;
    private static final byte OP_I32_TRUNC_F64_U = (byte) 0xab;
    private static final byte OP_I64_EXTEND_I32_S = (byte) 0xac;
    private static final byte OP_I64_EXTEND_I32_U = (byte) 0xad;
    private static final byte OP_I64_TRUNC_F32_S = (byte) 0xae;
    private static final byte OP_I64_TRUNC_F32_U = (byte) 0xaf;
    private static final byte OP_I64_TRUNC_F64_S = (byte) 0xb0;
    private static final byte OP_I64_TRUNC_F64_U = (byte) 0xb1;
    private static final byte OP_F32_CONVERT_I32_S = (byte) 0xb2;
    private static final byte OP_F32_CONVERT_I32_U = (byte) 0xb3;
    private static final byte OP_F32_CONVERT_I64_S = (byte) 0xb4;
    private static final byte OP_F32_CONVERT_I64_U = (byte) 0xb5;
    private static final byte OP_F32_DEMOTE_F64 = (byte) 0xb6;
    private static final byte OP_F64_CONVERT_I32_S = (byte) 0xb7;
    private static final byte OP_F64_CONVERT_I32_U = (byte) 0xb8;
    private static final byte OP_F64_CONVERT_I64_S = (byte) 0xb9;
    private static final byte OP_F64_CONVERT_I64_U = (byte) 0xba;
    private static final byte OP_F64_PROMOTE_F32 = (byte) 0xbb;
    private static final byte OP_I32_REINTERPRET_F32 = (byte) 0xbc;
    private static final byte OP_I64_REINTERPRET_F64 = (byte) 0xbd;
    private static final byte OP_F32_REINTERPRET_I32 = (byte) 0xbe;
    private static final byte OP_F64_REINTERPRET_I64 = (byte) 0xbf;
    private static final byte OP_I32_EXTEND8_S = (byte) 0xc0;
    private static final byte OP_I32_EXTEND16_S = (byte) 0xc1;
    private static final byte OP_I64_EXTEND8_S = (byte) 0xc2;
    private static final byte OP_I64_EXTEND16_S = (byte) 0xc3;
    private static final byte OP_I64_EXTEND32_S = (byte) 0xc4;
    private static final byte OP_REF_NULL = (byte) 0xd0;
    private static final byte OP_REF_IS_NULL = (byte) 0xd1;
    private static final byte OP_REF_FUNC = (byte) 0xd2;
    private static final byte OP_PREFIX = (byte) 0xfc;

    private static final int OP_CONT_I32_TRUNC_SAT_F32_S = 0;
    private static final int OP_CONT_I32_TRUNC_SAT_F32_U = 1;
    private static final int OP_CONT_I32_TRUNC_SAT_F64_S = 2;
    private static final int OP_CONT_I32_TRUNC_SAT_F64_U = 3;
    private static final int OP_CONT_I64_TRUNC_SAT_F32_S = 4;
    private static final int OP_CONT_I64_TRUNC_SAT_F32_U = 5;
    private static final int OP_CONT_I64_TRUNC_SAT_F64_S = 6;
    private static final int OP_CONT_I64_TRUNC_SAT_F64_U = 7;
    private static final int OP_CONT_MEMORY_INIT = 8;
    private static final int OP_CONT_DATA_DROP = 9;
    private static final int OP_CONT_MEMORY_COPY = 10;
    private static final int OP_CONT_MEMORY_FILL = 11;
    private static final int OP_CONT_TABLE_INIT = 12;
    private static final int OP_CONT_ELEM_DROP = 13;
    private static final int OP_CONT_TABLE_TABLE_COPY = 14;
    private static final int OP_CONT_TABLE_GROW = 15;
    private static final int OP_CONT_TABLE_SIZE = 16;
    private static final int OP_CONT_TABLE_FILL = 17;
}
