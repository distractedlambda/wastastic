package org.wastastic.compiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Type.getConstructorDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.compiler.Tuples.getTupleClass;

final class ModuleTranslator {
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    private final String internalName;
    private final ModuleReader reader;
    private final ClassVisitor clazz;

    private final ArrayList<FunctionType> types = new ArrayList<>();
    private final ArrayList<ImportedFunction> importedFunctions = new ArrayList<>();
    private final ArrayList<FunctionType> definedFunctions = new ArrayList<>();
    private final ArrayList<ImportedTable> importedTables = new ArrayList<>();
    private final ArrayList<TableType> definedTables = new ArrayList<>();
    private final ArrayList<ImportedMemory> importedMemories = new ArrayList<>();
    private final ArrayList<MemoryType> definedMemories = new ArrayList<>();
    private final ArrayList<ImportedGlobal> importedGlobals = new ArrayList<>();
    private final ArrayList<GlobalType> definedGlobals = new ArrayList<>();

    private int scratchLocalsOffset;
    private final ArrayList<Local> locals = new ArrayList<>();
    private final ArrayList<ValueType> operandTypes = new ArrayList<>();
    private final ArrayList<BranchTarget> branchTargets = new ArrayList<>();

    private MethodVisitor method;

    ModuleTranslator(String internalName, ModuleReader reader, ClassVisitor clazz) {
        this.internalName = requireNonNull(internalName);
        this.reader = requireNonNull(reader);
        this.clazz = requireNonNull(clazz);
    }

    void translate() throws CompilationException, IOException {
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
                case SECTION_TYPE -> translateTypeSection();
                case SECTION_IMPORT -> translateImportSection();
                case SECTION_FUNCTION -> translateFunctionSection();
                case SECTION_TABLE -> translateTableSection();
                case SECTION_MEMORY -> translateMemorySection();
                default -> throw new CompilationException("Unrecognized section ID: " + sectionId);
            }
        }
    }

    private void translateUntilEnd() throws IOException, CompilationException {
        for (var opcode = reader.nextByte();; opcode = reader.nextByte()) switch (opcode) {
            case OP_UNREACHABLE -> translateUnreachable();
            case OP_NOP -> translateNop();
            case OP_RETURN -> translateReturn();
            case OP_I32_LOAD -> translateI32Load();
            case OP_I32_STORE -> translateI32Store();
            case OP_I64_LOAD -> translateI64Load();
            case OP_I64_STORE -> translateI64Store();
            case OP_DROP -> translateDrop();
            case OP_SELECT -> translateSelect();
            case OP_CALL -> translateCall();
            case OP_BLOCK -> translateBlock();
            case OP_LOOP -> translateLoop();
            case OP_BR -> translateBranch();
            case OP_BR_IF -> translateConditionalBranch();
            case OP_BR_TABLE -> translateBranchTable();
            case OP_REF_NULL -> translateRefNull();
            case OP_REF_IS_NULL -> translateRefIsNull();
            case OP_I32_ADD -> translateI32Add();
            case OP_I64_ADD -> translateI64Add();
            case OP_I32_SUB -> translateI32Sub();
            case OP_I32_MUL -> translateI32Mul();
            case OP_I32_DIV_U -> translateI32DivU();
            case OP_I64_DIV_U -> translateI64DivU();
            case OP_I32_DIV_S -> translateI32DivS();
            case OP_I64_DIV_S -> translateI64DivS();
            case OP_I64_REM_U -> translateI64RemU();
            case OP_I64_REM_S -> translateI64RemS();
            case OP_I32_REM_U -> translateI32RemU();
            case OP_I32_REM_S -> translateI32RemS();
            case OP_I32_AND -> translateI32And();
            case OP_I64_AND -> translateI64And();
            case OP_I32_OR -> translateI32Or();
            case OP_I64_OR -> translateI64Or();
            case OP_I32_XOR -> translateI32Xor();
            case OP_I64_XOR -> translateI64Xor();
            case OP_I32_SHL -> translateI32Shl();
            case OP_I64_SHL -> translateI64Shl();
            case OP_I32_SHR_U -> translateI32ShrU();
            case OP_I64_SHR_U -> translateI64ShrU();
            case OP_I32_SHR_S -> translateI32ShrS();
            case OP_I64_SHR_S -> translateI64ShrS();
            case OP_I32_ROTL -> translateI32RotL();
            case OP_I64_ROTL -> translateI64RotL();
            case OP_I32_ROTR -> translateI32RotR();
            case OP_I64_ROTR -> translateI64RotR();
            case OP_I32_CLZ -> translateI32Clz();
            case OP_I64_CLZ -> translateI64Clz();
            case OP_I32_CTZ -> translateI32Ctz();
            case OP_I64_CTZ -> translateI64Ctz();
            case OP_I32_POPCNT -> translateI32Popcnt();
            case OP_I64_POPCNT -> translateI64Popcnt();
            case OP_I32_EQZ -> translateI32Eqz();
            case OP_I64_EQZ -> translateI64Eqz();
            case OP_I32_EQ -> translateI32Eq();
            case OP_I64_EQ -> translateI64Eq();
            case OP_I32_NE -> translateI32Ne();
            case OP_I64_NE -> translateI64Ne();
            case OP_I32_LT_U -> translateI32Ltu();
            case OP_I64_LT_U -> translateI64Ltu();
            case OP_I32_LT_S -> translateI32Lts();
            case OP_I64_LT_S -> translateI64Lts();
            case OP_I32_GT_U -> translateI32Gtu();
            case OP_I64_GT_U -> translateI64Gtu();
            case OP_I32_GT_S -> translateI32Gts();
            case OP_I64_GT_S -> translateI64Gts();
            case OP_I32_LE_U -> translateI32Leu();
            case OP_I64_LE_U -> translateI64Leu();
            case OP_I32_LE_S -> translateI32Les();
            case OP_I64_LE_S -> translateI64Les();
            case OP_I32_GE_U -> translateI32Geu();
            case OP_I64_GE_U -> translateI64Geu();
            case OP_I32_GE_S -> translateI32Ges();
            case OP_I64_GE_S -> translateI64Ges();
            case OP_F32_ADD -> translateF32Add();
            case OP_F32_SUB -> translateF32Sub();
            case OP_F32_MUL -> translateF32Mul();
            case OP_F32_DIV -> translateF32Div();
            case OP_F32_MIN -> translateF32Min();
            case OP_F32_MAX -> translateF32Max();
            case OP_F32_COPYSIGN -> translateF32Copysign();
            case OP_F32_ABS -> translateF32Abs();
            case OP_F32_NEG -> translateF32Neg();
            case OP_F32_SQRT -> translateF32Sqrt();
            case OP_F32_CEIL -> translateF32Ceil();
            case OP_F32_FLOOR -> translateF32Floor();
            case OP_F32_TRUNC -> translateF32Trunc();
            case OP_F32_NEAREST -> translateF32Nearest();
            case OP_F32_EQ -> translateF32Eq();
            case OP_F32_NE -> translateF32Ne();
            case OP_F32_LT -> translateF32Lt();
            case OP_F32_GT -> translateF32Gt();
            case OP_F32_LE -> translateF32Le();
            case OP_F32_GE -> translateF32Ge();
            case OP_F64_ADD -> translateF64Add();
            case OP_F64_SUB -> translateF64Sub();
            case OP_F64_MUL -> translateF64Mul();
            case OP_F64_DIV -> translateF64Div();
            case OP_F64_MIN -> translateF64Min();
            case OP_F64_MAX -> translateF64Max();
            case OP_F64_COPYSIGN -> translateF64Copysign();
            case OP_F64_ABS -> translateF64Abs();
            case OP_F64_NEG -> translateF64Neg();
            case OP_F64_SQRT -> translateF64Sqrt();
            case OP_F64_CEIL -> translateF64Ceil();
            case OP_F64_FLOOR -> translateF64Floor();
            case OP_F64_TRUNC -> translateF64Trunc();
            case OP_F64_NEAREST -> translateF64Nearest();
            case OP_F64_EQ -> translateF64Eq();
            case OP_F64_NE -> translateF64Ne();
            case OP_F64_LT -> translateF64Lt();
            case OP_F64_GT -> translateF64Gt();
            case OP_F64_LE -> translateF64Le();
            case OP_F64_GE -> translateF64Ge();
            case OP_I64_MUL -> translateI64Mul();
            case OP_I64_SUB -> translateI64Sub();
            case OP_SELECT_VEC -> translateSelectVec();
            case OP_LOCAL_GET -> translateLocalGet();
            case OP_LOCAL_SET -> translateLocalSet();
            case OP_LOCAL_TEE -> translateLocalTee();
            case OP_GLOBAL_SET -> translateGlobalSet();
            case OP_GLOBAL_GET -> translateGlobalGet();

            case OP_END -> {
                popBranchTarget();
                return;
            }
        }
    }

    private void translateGlobalSet() throws IOException {
        var globalIndex = reader.nextUnsigned32();
        ValueType type;

        if (globalIndex < importedGlobals.size()) {
            type = importedGlobals.get(globalIndex).getType().valueType();

            method.visitFieldInsn(
                Opcodes.GETFIELD,
                internalName,
                "g-" + globalIndex + "-set",
                "Ljava/lang/invoke/MethodHandle;"
            );

            method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                "invoke",
                "(" + type.getDescriptor() + ")V",
                false
            );
        } else {
            type = definedGlobals.get(globalIndex - importedGlobals.size()).valueType();

            method.visitFieldInsn(
                Opcodes.PUTFIELD,
                internalName,
                "g-" + globalIndex,
                type.getDescriptor()
            );
        }

        popOperandType();
    }

    private void translateGlobalGet() throws IOException {
        var globalIndex = reader.nextUnsigned32();
        ValueType type;

        if (globalIndex < importedGlobals.size()) {
            type = importedGlobals.get(globalIndex).getType().valueType();

            method.visitFieldInsn(
                Opcodes.GETFIELD,
                internalName,
                "g-" + globalIndex + "-get",
                "Ljava/lang/invoke/MethodHandle;"
            );

            method.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                "invoke",
                "()" + type.getDescriptor(),
                false
            );
        } else {
            type = definedGlobals.get(globalIndex - importedGlobals.size()).valueType();

            method.visitFieldInsn(
                Opcodes.GETFIELD,
                internalName,
                "g-" + globalIndex,
                type.getDescriptor()
            );
        }

        operandTypes.add(type);
    }

    private Local nextIndexedLocal() throws IOException {
        return locals.get(reader.nextUnsigned32());
    }

    private void translateLocalGet() throws IOException {
        var local = nextIndexedLocal();

        var opcode = switch (local.type()) {
            case I32 -> Opcodes.ILOAD;
            case I64 -> Opcodes.LLOAD;
            case F32 -> Opcodes.FLOAD;
            case F64 -> Opcodes.DLOAD;
            case FUNCREF, EXTERNREF -> Opcodes.ALOAD;
        };

        method.visitVarInsn(opcode, local.index());
        operandTypes.add(local.type());
    }

    private void translateLocalSet() throws IOException {
        var local = nextIndexedLocal();

        var opcode = switch (local.type()) {
            case I32 -> Opcodes.ISTORE;
            case I64 -> Opcodes.LSTORE;
            case F32 -> Opcodes.FSTORE;
            case F64 -> Opcodes.DSTORE;
            case FUNCREF, EXTERNREF -> Opcodes.ASTORE;
        };

        popOperandType();
        method.visitVarInsn(opcode, local.index());
    }

    private void translateLocalTee() throws IOException {
        var local = nextIndexedLocal();

        var opcode = switch (local.type()) {
            case I32 -> Opcodes.ISTORE;
            case I64 -> Opcodes.LSTORE;
            case F32 -> Opcodes.FSTORE;
            case F64 -> Opcodes.DSTORE;
            case FUNCREF, EXTERNREF -> Opcodes.ASTORE;
        };

        method.visitInsn(local.type().isDoubleWidth() ? Opcodes.DUP2 : Opcodes.DUP);
        method.visitVarInsn(opcode, local.index());
    }

    private void translateSelectVec() throws IOException, CompilationException {
        for (var i = reader.nextUnsigned32(); i != 0; i--) {
            nextValueType();
        }

        translateSelect();
    }

    private void translateI64Mul() {
        popOperandType();
        method.visitInsn(Opcodes.LMUL);
    }

    private void translateI64Sub() {
        popOperandType();
        method.visitInsn(Opcodes.LSUB);
    }

    private void translateF32Add() {
        popOperandType();
        method.visitInsn(Opcodes.FADD);
    }

    private void translateF64Add() {
        popOperandType();
        method.visitInsn(Opcodes.DADD);
    }

    private void translateF32Sub() {
        popOperandType();
        method.visitInsn(Opcodes.FSUB);
    }

    private void translateF64Sub() {
        popOperandType();
        method.visitInsn(Opcodes.DSUB);
    }

    private void translateF32Mul() {
        popOperandType();
        method.visitInsn(Opcodes.FMUL);
    }

    private void translateF64Mul() {
        popOperandType();
        method.visitInsn(Opcodes.DMUL);
    }

    private void translateF32Div() {
        popOperandType();
        method.visitInsn(Opcodes.FDIV);
    }

    private void translateF64Div() {
        popOperandType();
        method.visitInsn(Opcodes.DDIV);
    }

    private void translateF32Min() {
        popOperandType();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(FF)F", false);
    }

    private void translateF64Min() {
        popOperandType();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(DD)D", false);
    }

    private void translateF32Max() {
        popOperandType();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(FF)F", false);
    }

    private void translateF64Max() {
        popOperandType();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(DD)D", false);
    }

    private void translateF32Copysign() {
        popOperandType();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "copySign", "(FF)F", false);
    }

    private void translateF64Copysign() {
        popOperandType();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "copySign", "(DD)D", false);
    }

    private void translateF32Abs() {
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "abs", "(F)F", false);
    }

    private void translateF64Abs() {
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
    }

    private void translateF32Neg() {
        method.visitInsn(Opcodes.FNEG);
    }

    private void translateF64Neg() {
        method.visitInsn(Opcodes.DNEG);
    }

    private void translateF32Sqrt() {
        emitHelperCall("fsqrt", "(F)F");
    }

    private void translateF64Sqrt() {
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sqrt", "(D)D", false);
    }

    private void translateF32Ceil() {
        emitHelperCall("fceil", "(F)F");
    }

    private void translateF64Ceil() {
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "ceil", "(D)D", false);
    }

    private void translateF32Floor() {
        emitHelperCall("ffloor", "(F)F");
    }

    private void translateF64Floor() {
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "floor", "(D)D", false);
    }

    private void translateF32Trunc() {
        emitHelperCall("ftrunc", "(F)F");
    }

    private void translateF64Trunc() {
        emitHelperCall("ftrunc", "(D)D");
    }

    private void translateF32Nearest() {
        emitHelperCall("fnearest", "(F)F");
    }

    private void translateF64Nearest() {
        // TODO: check equivalence
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "rint", "(D)D", false);
    }

    private void translateF32Eq() {
        popOperandType();
        popOperandType();
        emitHelperCall("feq", "(FF)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF64Eq() {
        popOperandType();
        popOperandType();
        emitHelperCall("feq", "(DD)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF32Ne() {
        popOperandType();
        popOperandType();
        emitHelperCall("fne", "(FF)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF64Ne() {
        popOperandType();
        popOperandType();
        emitHelperCall("fne", "(DD)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF32Lt() {
        popOperandType();
        popOperandType();
        emitHelperCall("flt", "(FF)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF64Lt() {
        popOperandType();
        popOperandType();
        emitHelperCall("flt", "(DD)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF32Gt() {
        popOperandType();
        popOperandType();
        emitHelperCall("fgt", "(FF)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF64Gt() {
        popOperandType();
        popOperandType();
        emitHelperCall("fgt", "(DD)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF32Le() {
        popOperandType();
        popOperandType();
        emitHelperCall("fle", "(FF)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF64Le() {
        popOperandType();
        popOperandType();
        emitHelperCall("fle", "(DD)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF32Ge() {
        popOperandType();
        popOperandType();
        emitHelperCall("fge", "(FF)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateF64Ge() {
        popOperandType();
        popOperandType();
        emitHelperCall("fge", "(DD)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI32And() {
        popOperandType();
        method.visitInsn(Opcodes.IAND);
    }

    private void translateI64And() {
        popOperandType();
        method.visitInsn(Opcodes.LAND);
    }

    private void translateI32Or() {
        popOperandType();
        method.visitInsn(Opcodes.IOR);
    }

    private void translateI64Or() {
        popOperandType();
        method.visitInsn(Opcodes.LOR);
    }

    private void translateI32Xor() {
        popOperandType();
        method.visitInsn(Opcodes.IXOR);
    }

    private void translateI64Xor() {
        popOperandType();
        method.visitInsn(Opcodes.LXOR);
    }

    private void translateI32Shl() {
        popOperandType();
        method.visitInsn(Opcodes.ISHL);
    }

    private void translateI64Shl() {
        popOperandType();
        method.visitInsn(Opcodes.LSHL);
    }

    private void translateI32ShrU() {
        popOperandType();
        method.visitInsn(Opcodes.IUSHR);
    }

    private void translateI64ShrU() {
        popOperandType();
        method.visitInsn(Opcodes.LUSHR);
    }

    private void translateI32ShrS() {
        popOperandType();
        method.visitInsn(Opcodes.ISHR);
    }

    private void translateI64ShrS() {
        popOperandType();
        method.visitInsn(Opcodes.LSHR);
    }

    private void translateI32RotL() {
        popOperandType();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false);
    }

    private void translateI64RotL() {
        popOperandType();
        emitHelperCall("rotl", "(JJ)J");
    }

    private void translateI32RotR() {
        popOperandType();
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateRight", "(II)I", false);
    }

    private void translateI64RotR() {
        popOperandType();
        emitHelperCall("rotr", "(JJ)J");
    }

    private void translateI32Clz() {
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "numberOfLeadingZeros", "(I)I", false);
    }

    private void translateI64Clz() {
        emitHelperCall("clz", "(J)J");
    }

    private void translateI32Ctz() {
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "numberOfTrailingZeros", "(I)I", false);
    }

    private void translateI64Ctz() {
        emitHelperCall("ctz", "(J)J");
    }

    private void translateI32Popcnt() {
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "bitCount", "(I)I", false);
    }

    private void translateI64Popcnt() {
        emitHelperCall("popcnt", "(J)J");
    }

    private void translateI32Eqz() {
        emitHelperCall("eqz", "(I)Z");
    }

    private void translateI32Eq() {
        popOperandType();
        emitHelperCall("eq", "(II)Z");
    }

    private void translateI32Ne() {
        popOperandType();
        emitHelperCall("ne", "(II)Z");
    }

    private void translateI32Ltu() {
        popOperandType();
        emitHelperCall("ltu", "(II)Z");
    }

    private void translateI32Lts() {
        popOperandType();
        emitHelperCall("lts", "(II)Z");
    }

    private void translateI32Gtu() {
        popOperandType();
        emitHelperCall("gtu", "(II)Z");
    }

    private void translateI32Gts() {
        popOperandType();
        emitHelperCall("gts", "(II)Z");
    }

    private void translateI32Leu() {
        popOperandType();
        emitHelperCall("leu", "(II)Z");
    }

    private void translateI32Les() {
        popOperandType();
        emitHelperCall("les", "(II)Z");
    }

    private void translateI32Geu() {
        popOperandType();
        emitHelperCall("geu", "(II)Z");
    }

    private void translateI32Ges() {
        popOperandType();
        emitHelperCall("ges", "(II)Z");
    }

    private void translateI64Eqz() {
        popOperandType();
        emitHelperCall("eqz", "(J)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Eq() {
        popOperandType();
        popOperandType();
        emitHelperCall("eq", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Ne() {
        popOperandType();
        popOperandType();
        emitHelperCall("ne", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Ltu() {
        popOperandType();
        popOperandType();
        emitHelperCall("ltu", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Lts() {
        popOperandType();
        popOperandType();
        emitHelperCall("lts", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Gtu() {
        popOperandType();
        popOperandType();
        emitHelperCall("gtu", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Gts() {
        popOperandType();
        popOperandType();
        emitHelperCall("gts", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Leu() {
        popOperandType();
        popOperandType();
        emitHelperCall("leu", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Les() {
        popOperandType();
        popOperandType();
        emitHelperCall("les", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Geu() {
        popOperandType();
        popOperandType();
        emitHelperCall("geu", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI64Ges() {
        popOperandType();
        popOperandType();
        emitHelperCall("ges", "(JJ)Z");
        operandTypes.add(ValueType.I32);
    }

    private void translateI32RemU() {
        popOperandType();
        emitHelperCall("remU", "(II)I");
    }

    private void translateI32RemS() {
        popOperandType();
        emitHelperCall("remS", "(II)I");
    }

    private void translateI64RemU() {
        popOperandType();
        emitHelperCall("remU", "(JJ)J");
    }

    private void translateI64RemS() {
        popOperandType();
        emitHelperCall("remS", "(JJ)J");
    }

    private void translateI32DivS() {
        popOperandType();
        emitHelperCall("divS", "(II)I");
    }

    private void translateI64DivS() {
        popOperandType();
        emitHelperCall("divS", "(JJ)J");
    }

    private void translateI64DivU() {
        popOperandType();
        emitHelperCall("divU", "(JJ)J");
    }

    private void translateI32DivU() {
        popOperandType();
        emitHelperCall("divU", "(II)I");
    }

    private void translateI32Mul() {
        popOperandType();
        method.visitInsn(Opcodes.IMUL);
    }

    private void translateI32Add() {
        popOperandType();
        method.visitInsn(Opcodes.IADD);
    }

    private void translateI32Sub() {
        popOperandType();
        method.visitInsn(Opcodes.ISUB);
    }

    private void translateI64Add() {
        popOperandType();
        method.visitInsn(Opcodes.LADD);
    }

    private void translateRefIsNull() {
        popOperandType();
        emitHelperCall("refIsNull", "(Ljava/lang/Object;)Z");
    }

    private void translateRefNull() throws CompilationException, IOException {
        method.visitInsn(Opcodes.ACONST_NULL);
        operandTypes.add(nextReferenceType().toValueType());
    }

    private void translateBranchTable() throws IOException {
        var indexedTargetCount = reader.nextUnsigned32();
        var indexedTargets = new BranchTarget[indexedTargetCount];

        for (var i = 0; i < indexedTargetCount; i++) {
            indexedTargets[i] = nextBranchTarget();
        }

        var defaultTarget = nextBranchTarget();

        var indexedAdapterLabels = new Label[indexedTargetCount];
        for (var i = 0; i < indexedTargetCount; i++) {
            indexedAdapterLabels[i] = new Label();
        }

        var defaultAdapterLabel = new Label();

        method.visitTableSwitchInsn(0, indexedTargetCount - 1, defaultAdapterLabel, indexedAdapterLabels);
        popOperandType();

        for (var i = 0; i < indexedTargetCount; i++) {
            method.visitLabel(indexedAdapterLabels[i]);
            emitBranch(indexedTargets[i]);
        }

        method.visitLabel(defaultAdapterLabel);
        emitBranch(defaultTarget);
    }

    private BranchTarget nextBranchTarget() throws IOException {
        return branchTargets.get(branchTargets.size() - 1 - reader.nextUnsigned32());
    }

    private void translateConditionalBranch() throws IOException {
        var pastBranchLabel = new Label();
        method.visitJumpInsn(Opcodes.IFEQ, pastBranchLabel);
        popOperandType();
        translateBranch();
        method.visitLabel(pastBranchLabel);
    }

    private void emitBranch(BranchTarget target) {
        var localOffset = scratchLocalsOffset;

        for (var i = 0; i < target.getParameterCount(); i++) switch (operandTypes.get(operandTypes.size() - i - 1)) {
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

        for (var i = 0; i < target.getParameterCount(); i++) {
            if (operandTypes.get(operandTypes.size() - i - 1).isDoubleWidth()) {
                method.visitInsn(Opcodes.POP2);
            } else {
                method.visitInsn(Opcodes.POP);
            }
        }

        for (var parameterType : target.getParameterTypeList()) switch (parameterType) {
            case I32 -> method.visitVarInsn(Opcodes.ILOAD, (localOffset -= 1));
            case F32 -> method.visitVarInsn(Opcodes.FLOAD, (localOffset -= 1));
            case I64 -> method.visitVarInsn(Opcodes.LLOAD, (localOffset -= 2));
            case F64 -> method.visitVarInsn(Opcodes.DLOAD, (localOffset -= 2));
            case FUNCREF, EXTERNREF -> method.visitVarInsn(Opcodes.ALOAD, (localOffset -= 1));
        }

        method.visitJumpInsn(Opcodes.GOTO, target.label());
    }

    private void translateBranch() throws IOException {
        var targetIndex = branchTargets.size() - 1 - reader.nextUnsigned32();
        if (targetIndex == 0) {
            translateReturn();
        } else {
            emitBranch(nextBranchTarget());
        }
    }

    private void translateLoop() throws CompilationException, IOException {
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
        translateUntilEnd();
    }

    private void translateBlock() throws CompilationException, IOException {
        var type = nextBlockType();
        var endLabel = new Label();
        branchTargets.add(
            new BranchTarget(
                endLabel,
                type.getReturnTypes(),
                operandTypes.size() - type.getParameterCount()
            )
        );
        translateUntilEnd();
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

    private void translateUnreachable() {
        emitTrap();
    }

    private void translateNop() {
        // Nothing to do
    }

    private void translateReturn() {
        var returnTypes = branchTargets.get(0).parameterTypes();

        if (returnTypes == null) {
            method.visitInsn(Opcodes.RETURN);
        } else if (returnTypes instanceof ValueType returnType) {
            var opcode = switch (returnType) {
                case I32 -> Opcodes.IRETURN;
                case F32 -> Opcodes.FRETURN;
                case I64 -> Opcodes.LRETURN;
                case F64 -> Opcodes.DRETURN;
                case FUNCREF, EXTERNREF -> Opcodes.ARETURN;
            };

            method.visitInsn(opcode);
        } else {
            var returnTypesArray = (ValueType[]) returnTypes;
            var signatureChars = new char[returnTypesArray.length];

            for (var i = 0; i < returnTypesArray.length; i++) {
                signatureChars[i] = switch (returnTypesArray[i]) {
                    case I32 -> 'I';
                    case I64 -> 'J';
                    case F32 -> 'F';
                    case F64 -> 'D';
                    case FUNCREF, EXTERNREF -> 'L';
                };
            }

            var signature = new String(signatureChars);
            var tupleClass = getTupleClass(signature);
            var internalName = getInternalName(tupleClass);
            var constructorDescriptor = getConstructorDescriptor(tupleClass.getConstructors()[0]);

            method.visitTypeInsn(Opcodes.NEW, internalName);
            method.visitMethodInsn(Opcodes.INVOKESPECIAL, internalName, "<init>", constructorDescriptor, false);
            method.visitInsn(Opcodes.ARETURN);
        }
    }

    private void translateI32Load() throws IOException {
        emitEffectiveAddress(nextMemArg().unsignedOffset());
        emitGetMemory();
        method.visitInsn(Opcodes.SWAP);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/wastastic/Memory", "loadInt", "(I)I", true);
        operandTypes.add(ValueType.I32);
    }

    private void translateI32Store() throws IOException {
        popOperandType();
        popOperandType();

        method.visitInsn(Opcodes.SWAP);
        emitEffectiveAddress(nextMemArg().unsignedOffset());
        method.visitInsn(Opcodes.SWAP);
        emitGetMemory();
        method.visitInsn(Opcodes.DUP_X2);
        method.visitInsn(Opcodes.POP);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/wastastic/Memory", "storeInt", "(II)V", true);
    }

    private void translateI64Load() throws IOException {
        emitEffectiveAddress(nextMemArg().unsignedOffset());
        emitGetMemory();
        method.visitInsn(Opcodes.SWAP);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/wastastic/Memory", "loadLong", "(I)J", true);
        operandTypes.add(ValueType.I64);
    }

    private void translateI64Store() throws IOException {
        popOperandType();
        popOperandType();

        method.visitInsn(Opcodes.DUP2_X1);
        method.visitInsn(Opcodes.POP2);
        emitEffectiveAddress(nextMemArg().unsignedOffset());
        emitGetMemory();
        method.visitInsn(Opcodes.SWAP);
        method.visitInsn(Opcodes.DUP2_X2);
        method.visitInsn(Opcodes.POP2);
        method.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/wastastic/Memory", "storeLong", "(IJ)V", true);
    }

    private void translateCall() {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    private void translateSelect() {
        popOperandType();
        emitHelperCall("select", switch (popOperandType()) {
            case I32 -> "(III)I";
            case I64 -> "(IJJ)J";
            case F32 -> "(IFF)F";
            case F64 -> "(IDD)D";
            case FUNCREF, EXTERNREF -> "(ILjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
        });
    }

    private void translateDrop() {
        method.visitInsn(popOperandType().isDoubleWidth() ? Opcodes.POP2 : Opcodes.POP);
    }

    private void emitGetMemory() {
        method.visitFieldInsn(Opcodes.GETFIELD, "org/wastastic/Module", "memory", "Lorg/wastastic/Memory;");
    }

    private void emitEffectiveAddress(int unsignedOffset) {
        if (unsignedOffset != 0) {
            emitHelperCall("effectiveAddress", "(II)I");
        }
    }

    private void emitTrap() {
        emitHelperCall("trap", "()Ljava/lang/RuntimeException;");
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

    private void emitHelperCall(String name, String descriptor) {
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "org/wastastic/runtime/InstructionHelpers",
            name,
            descriptor,
            false
        );
    }

    private void translateMemorySection() throws IOException, CompilationException {
        var remaining = reader.nextUnsigned32();
        definedMemories.ensureCapacity(definedMemories.size() + remaining);
        for (; remaining != 0; remaining--) {
            definedMemories.add(nextMemoryType());
        }
    }

    private void translateTableSection() throws CompilationException, IOException {
        var remaining = reader.nextUnsigned32();
        definedTables.ensureCapacity(definedTables.size() + remaining);
        for (; remaining != 0; remaining--) {
            definedTables.add(nextTableType());
        }
    }

    private void translateFunctionSection() throws IOException {
        var remaining = reader.nextUnsigned32();
        definedFunctions.ensureCapacity(definedFunctions.size() + remaining);
        for (; remaining != 0; remaining--) {
            definedFunctions.add(nextIndexedType());
        }
    }

    private FunctionType nextIndexedType() throws IOException {
        return types.get(reader.nextUnsigned32());
    }

    private void translateImportSection() throws CompilationException, IOException {
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            var moduleName = nextName();
            var name = nextName();
            switch (reader.nextByte()) {
                case 0x00 -> importedFunctions.add(new ImportedFunction(moduleName, name, nextIndexedType()));
                case 0x01 -> importedTables.add(new ImportedTable(moduleName, name, nextTableType()));
                case 0x02 -> importedMemories.add(new ImportedMemory(moduleName, name, nextMemoryType()));
                case 0x03 -> importedGlobals.add(new ImportedGlobal(moduleName, name, nextGlobalType()));
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

    private void translateTypeSection() throws CompilationException, IOException {
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

    private ValueType popOperandType() {
        return operandTypes.remove(operandTypes.size() - 1);
    }

    private BranchTarget popBranchTarget() {
        return branchTargets.remove(branchTargets.size() - 1);
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
