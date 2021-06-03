package org.wastastic.compiler;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Type.getConstructorDescriptor;
import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.compiler.Tuples.getTupleClass;

final class ModuleTranslator {
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

    private final ArrayList<Local> locals = new ArrayList<>();
    private final ArrayList<ValueType> operandStack = new ArrayList<>();
    private final ArrayList<LabelScope> labelStack = new ArrayList<>();

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
            }
            catch (EOFException ignored) {
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

    private void translateFunction() throws IOException, CompilationException {
        for (var opcode = reader.nextByte(); ; opcode = reader.nextByte()) {
            switch (opcode) {
                case OP_UNREACHABLE:
                    emitHelperCall("trap", "()Ljava/lang/RuntimeException;");
                    method.visitInsn(Opcodes.ATHROW);
                    break;

                case OP_NOP:
                    break;

                case OP_BLOCK: {
                    var type = nextBlockType();
                    var endLabel = new Label();
                    labelStack.add(new LabelScope(
                        endLabel,
                        type.getReturnTypes(),
                        operandStack.size() - type.getParameterCount(),
                        endLabel,
                        null,
                        null
                    ));

                    break;
                }

                case OP_LOOP: {
                    var type = nextBlockType();
                    var startLabel = new Label();
                    method.visitLabel(startLabel);
                    labelStack.add(new LabelScope(
                        startLabel,
                        type.getParameterTypes(),
                        operandStack.size() - type.getParameterCount(),
                        null,
                        null,
                        null
                    ));

                    break;
                }

                case OP_IF: {
                    var type = nextBlockType();
                    var endLabel = new Label();
                    var elseLabel = new Label();

                    labelStack.add(new LabelScope(
                        endLabel,
                        type.getReturnTypes(),
                        operandStack.size() - type.getParameterCount() - 1,
                        endLabel,
                        elseLabel,
                        type.getParameterTypes()
                    ));

                    method.visitJumpInsn(Opcodes.IFEQ, elseLabel);
                    popOperandType();

                    break;
                }

                case OP_ELSE: {
                    var scope = popLabelScope();
                    method.visitJumpInsn(Opcodes.GOTO, scope.targetLabel());
                    method.visitLabel(scope.elseLabel());

                    while (operandStack.size() > scope.operandStackSize()) {
                        popOperandType();
                    }

                    operandStack.addAll(ResultTypes.asList(scope.elseParameterTypes()));
                    labelStack.add(new LabelScope(
                        scope.targetLabel(),
                        scope.parameterTypes(),
                        scope.operandStackSize(),
                        scope.endLabel(),
                        null,
                        null
                    ));

                    break;
                }

                case OP_END: {
                    var scope = popLabelScope();

                    if (scope.elseLabel() != null) {
                        method.visitLabel(scope.elseLabel());
                    }

                    if (scope.endLabel() != null) {
                        method.visitLabel(scope.endLabel());
                    }

                    if (labelStack.isEmpty()) {
                        // FIXME: generate return instruction instead? Or break from loop maybe?
                        return;
                    }

                    break;
                }

                case OP_BR:
                    emitBranch(nextBranchTarget());
                    break;

                case OP_BR_IF: {
                    var pastBranchLabel = new Label();
                    method.visitJumpInsn(Opcodes.IFEQ, pastBranchLabel);
                    popOperandType();
                    emitBranch(nextBranchTarget());
                    method.visitLabel(pastBranchLabel);
                    break;
                }

                case OP_BR_TABLE: {
                    var indexedTargetCount = reader.nextUnsigned32();
                    var indexedTargets = new LabelScope[indexedTargetCount];

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

                    break;
                }

                case OP_RETURN:
                    emitBranch(labelStack.get(0));
                    break;

                case OP_CALL: {
                    var index = reader.nextUnsigned32();
                    var name = "f-" + index;

                    if (index < importedFunctions.size()) {
                        var type = importedFunctions.get(index).getType();
                        int savedValuesEnd = saveValues(type.getParameterTypeList());
                        method.visitFieldInsn(Opcodes.GETFIELD, internalName, name, "Ljava/lang/invoke/MethodHandle;");
                        method.visitVarInsn(Opcodes.ALOAD, 0);
                        restoreValues(type.getParameterTypeList(), savedValuesEnd);
                        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invoke", type.getSignatureString(), false);
                        // FIXME: deal with result tuples
                    }
                    else {

                    }

                    break;
                }

                case OP_CALL_INDIRECT:
                    // TODO
                    break;

                case OP_DROP:
                    method.visitInsn(popOperandType().isDoubleWidth() ? Opcodes.POP2 : Opcodes.POP);

                case OP_SELECT_VEC:
                    for (var i = reader.nextUnsigned32(); i != 0; i--) {
                        nextValueType();
                    }
                case OP_SELECT:
                    popOperandType();
                    emitHelperCall("select", switch (popOperandType()) {
                        case I32 -> "(III)I";
                        case I64 -> "(IJJ)J";
                        case F32 -> "(IFF)F";
                        case F64 -> "(IDD)D";
                        case FUNCREF, EXTERNREF -> "(ILjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
                    });
                    break;

                case OP_LOCAL_GET: {
                    var local = nextIndexedLocal();

                    method.visitVarInsn(
                        switch (local.type()) {
                            case I32 -> Opcodes.ILOAD;
                            case I64 -> Opcodes.LLOAD;
                            case F32 -> Opcodes.FLOAD;
                            case F64 -> Opcodes.DLOAD;
                            case FUNCREF, EXTERNREF -> Opcodes.ALOAD;
                        },
                        local.index());

                    operandStack.add(local.type());
                    break;
                }

                case OP_LOCAL_SET: {
                    var local = nextIndexedLocal();

                    popOperandType();

                    method.visitVarInsn(
                        switch (local.type()) {
                            case I32 -> Opcodes.ISTORE;
                            case I64 -> Opcodes.LSTORE;
                            case F32 -> Opcodes.FSTORE;
                            case F64 -> Opcodes.DSTORE;
                            case FUNCREF, EXTERNREF -> Opcodes.ASTORE;
                        },
                        local.index()
                    );

                    break;
                }

                case OP_LOCAL_TEE: {
                    var local = nextIndexedLocal();

                    method.visitInsn(local.type().isDoubleWidth() ? Opcodes.DUP2 : Opcodes.DUP);

                    method.visitVarInsn(
                        switch (local.type()) {
                            case I32 -> Opcodes.ISTORE;
                            case I64 -> Opcodes.LSTORE;
                            case F32 -> Opcodes.FSTORE;
                            case F64 -> Opcodes.DSTORE;
                            case FUNCREF, EXTERNREF -> Opcodes.ASTORE;
                        },
                        local.index()
                    );

                    break;
                }

                case OP_GLOBAL_SET: {
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
                    }
                    else {
                        type = definedGlobals.get(globalIndex - importedGlobals.size()).valueType();

                        method.visitFieldInsn(
                            Opcodes.PUTFIELD,
                            internalName,
                            "g-" + globalIndex,
                            type.getDescriptor()
                        );
                    }

                    popOperandType();
                    break;
                }

                case OP_GLOBAL_GET: {
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
                    }
                    else {
                        type = definedGlobals.get(globalIndex - importedGlobals.size()).valueType();

                        method.visitFieldInsn(
                            Opcodes.GETFIELD,
                            internalName,
                            "g-" + globalIndex,
                            type.getDescriptor()
                        );
                    }

                    operandStack.add(type);
                    break;
                }

                case OP_TABLE_GET:
                case OP_TABLE_SET:
                    // TODO
                    break;

                case OP_I32_LOAD:
                case OP_I64_LOAD:
                case OP_F32_LOAD:
                case OP_F64_LOAD:
                case OP_I32_LOAD8_S:
                case OP_I32_LOAD8_U:
                case OP_I32_LOAD16_S:
                case OP_I32_LOAD16_U:
                case OP_I64_LOAD8_S:
                case OP_I64_LOAD8_U:
                case OP_I64_LOAD16_S:
                case OP_I64_LOAD16_U:
                case OP_I64_LOAD32_S:
                case OP_I64_LOAD32_U:
                case OP_I32_STORE:
                case OP_I64_STORE:
                case OP_F32_STORE:
                case OP_F64_STORE:
                case OP_I32_STORE8:
                case OP_I32_STORE16:
                case OP_I64_STORE8:
                case OP_I64_STORE16:
                case OP_I64_STORE32:
                case OP_MEMORY_SIZE:
                case OP_MEMORY_GROW:
                    // TODO
                    break;

                case OP_I32_CONST: {
                    var value = reader.nextSigned32();

                    switch (value) {
                        case -1 -> method.visitInsn(Opcodes.ICONST_M1);
                        case 0 -> method.visitInsn(Opcodes.ICONST_0);
                        case 1 -> method.visitInsn(Opcodes.ICONST_1);
                        case 2 -> method.visitInsn(Opcodes.ICONST_2);
                        case 3 -> method.visitInsn(Opcodes.ICONST_3);
                        case 4 -> method.visitInsn(Opcodes.ICONST_4);
                        case 5 -> method.visitInsn(Opcodes.ICONST_5);
                        default -> {
                            if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                                method.visitIntInsn(Opcodes.BIPUSH, value);
                            }
                            else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                                method.visitIntInsn(Opcodes.SIPUSH, value);
                            }
                            else {
                                method.visitLdcInsn(value);
                            }
                        }
                    }

                    break;
                }

                case OP_I64_CONST: {
                    var value = reader.nextSigned64();

                    if (value == 0) {
                        method.visitInsn(Opcodes.LCONST_0);
                    }
                    else if (value == 1) {
                        method.visitInsn(Opcodes.LCONST_1);
                    }
                    else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                        method.visitIntInsn(Opcodes.BIPUSH, (int) value);
                        method.visitInsn(Opcodes.I2L);
                    }
                    else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                        method.visitIntInsn(Opcodes.SIPUSH, (int) value);
                        method.visitInsn(Opcodes.I2L);
                    }
                    else {
                        method.visitLdcInsn(value);
                    }

                    break;
                }

                case OP_F32_CONST: {
                    var value = reader.nextFloat32();

                    if (value == 0) {
                        method.visitInsn(Opcodes.FCONST_0);
                    }
                    else if (value == 1) {
                        method.visitInsn(Opcodes.FCONST_1);
                    }
                    else if (value == 2) {
                        method.visitInsn(Opcodes.FCONST_2);
                    }
                    else {
                        method.visitLdcInsn(value);
                    }

                    break;
                }

                case OP_F64_CONST: {
                    var value = reader.nextFloat64();

                    if (value == 0) {
                        method.visitInsn(Opcodes.DCONST_0);
                    }
                    else if (value == 1) {
                        method.visitInsn(Opcodes.DCONST_1);
                    }
                    else {
                        method.visitLdcInsn(value);
                    }

                    break;
                }

                case OP_I32_EQZ:
                    emitHelperCall("eqz", "(I)Z");
                    break;

                case OP_I32_EQ:
                    popOperandType();
                    emitHelperCall("eq", "(II)Z");
                    break;

                case OP_I32_NE:
                    popOperandType();
                    emitHelperCall("ne", "(II)Z");
                    break;

                case OP_I32_LT_S:
                    popOperandType();
                    emitHelperCall("lts", "(II)Z");
                    break;

                case OP_I32_LT_U:
                    popOperandType();
                    emitHelperCall("ltu", "(II)Z");
                    break;

                case OP_I32_GT_S:
                    popOperandType();
                    emitHelperCall("gts", "(II)Z");
                    break;

                case OP_I32_GT_U:
                    popOperandType();
                    emitHelperCall("gtu", "(II)Z");
                    break;

                case OP_I32_LE_S:
                    popOperandType();
                    emitHelperCall("les", "(II)Z");
                    break;

                case OP_I32_LE_U:
                    popOperandType();
                    emitHelperCall("leu", "(II)Z");
                    break;

                case OP_I32_GE_S:
                    popOperandType();
                    emitHelperCall("ges", "(II)Z");
                    break;

                case OP_I32_GE_U:
                    popOperandType();
                    emitHelperCall("geu", "(II)Z");
                    break;

                case OP_I64_EQZ:
                    popOperandType();
                    emitHelperCall("eqz", "(J)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_EQ:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("eq", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_NE:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("ne", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_LT_S:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("lts", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_LT_U:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("ltu", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_GT_S:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("gts", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_GT_U:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("gtu", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_LE_S:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("les", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_LE_U:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("leu", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_GE_S:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("ges", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_GE_U:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("geu", "(JJ)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F32_EQ:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("feq", "(FF)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F32_NE:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("fne", "(FF)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F32_LT:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("flt", "(FF)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F32_GT:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("fgt", "(FF)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F32_LE:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("fle", "(FF)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F32_GE:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("fge", "(FF)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F64_EQ:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("feq", "(DD)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F64_NE:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("fne", "(DD)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F64_LT:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("flt", "(DD)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F64_GT:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("fgt", "(DD)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F64_LE:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("fle", "(DD)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F64_GE:
                    popOperandType();
                    popOperandType();
                    emitHelperCall("fge", "(DD)Z");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I32_CLZ:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "numberOfLeadingZeros", "(I)I", false);
                    break;

                case OP_I32_CTZ:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "numberOfTrailingZeros", "(I)I", false);
                    break;

                case OP_I32_POPCNT:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "bitCount", "(I)I", false);
                    break;

                case OP_I32_ADD:
                    popOperandType();
                    method.visitInsn(Opcodes.IADD);
                    break;

                case OP_I32_SUB:
                    popOperandType();
                    method.visitInsn(Opcodes.ISUB);
                    break;

                case OP_I32_MUL:
                    popOperandType();
                    method.visitInsn(Opcodes.IMUL);
                    break;

                case OP_I32_DIV_S:
                    popOperandType();
                    emitHelperCall("divS", "(II)I");
                    break;

                case OP_I32_DIV_U:
                    popOperandType();
                    emitHelperCall("divU", "(II)I");
                    break;

                case OP_I32_REM_S:
                    popOperandType();
                    emitHelperCall("remS", "(II)I");
                    break;

                case OP_I32_REM_U:
                    popOperandType();
                    emitHelperCall("remU", "(II)I");
                    break;

                case OP_I32_AND:
                    popOperandType();
                    method.visitInsn(Opcodes.IAND);
                    break;

                case OP_I32_OR:
                    popOperandType();
                    method.visitInsn(Opcodes.IOR);
                    break;

                case OP_I32_XOR:
                    popOperandType();
                    method.visitInsn(Opcodes.IXOR);
                    break;

                case OP_I32_SHL:
                    popOperandType();
                    method.visitInsn(Opcodes.ISHL);
                    break;

                case OP_I32_SHR_S:
                    popOperandType();
                    method.visitInsn(Opcodes.ISHR);
                    break;

                case OP_I32_SHR_U:
                    popOperandType();
                    method.visitInsn(Opcodes.IUSHR);
                    break;

                case OP_I32_ROTL:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false);
                    break;

                case OP_I32_ROTR:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "rotateRight", "(II)I", false);
                    break;

                case OP_I64_CLZ:
                    emitHelperCall("clz", "(J)J");
                    break;

                case OP_I64_CTZ:
                    emitHelperCall("ctz", "(J)J");
                    break;

                case OP_I64_POPCNT:
                    emitHelperCall("popcnt", "(J)J");
                    break;

                case OP_I64_ADD:
                    popOperandType();
                    method.visitInsn(Opcodes.LADD);
                    break;

                case OP_I64_SUB:
                    popOperandType();
                    method.visitInsn(Opcodes.LSUB);
                    break;

                case OP_I64_MUL:
                    popOperandType();
                    method.visitInsn(Opcodes.LMUL);
                    break;

                case OP_I64_DIV_S:
                    popOperandType();
                    emitHelperCall("divS", "(JJ)J");
                    break;

                case OP_I64_DIV_U:
                    popOperandType();
                    emitHelperCall("divU", "(JJ)J");
                    break;

                case OP_I64_REM_S:
                    popOperandType();
                    emitHelperCall("remS", "(JJ)J");
                    break;

                case OP_I64_REM_U:
                    popOperandType();
                    emitHelperCall("remU", "(JJ)J");
                    break;

                case OP_I64_AND:
                    popOperandType();
                    method.visitInsn(Opcodes.LAND);
                    break;

                case OP_I64_OR:
                    popOperandType();
                    method.visitInsn(Opcodes.LOR);
                    break;

                case OP_I64_XOR:
                    popOperandType();
                    method.visitInsn(Opcodes.LXOR);
                    break;

                case OP_I64_SHL:
                    popOperandType();
                    method.visitInsn(Opcodes.LSHL);
                    break;

                case OP_I64_SHR_S:
                    popOperandType();
                    method.visitInsn(Opcodes.LSHR);
                    break;

                case OP_I64_SHR_U:
                    popOperandType();
                    method.visitInsn(Opcodes.LUSHR);
                    break;

                case OP_I64_ROTL:
                    popOperandType();
                    emitHelperCall("rotl", "(JJ)J");
                    break;

                case OP_I64_ROTR:
                    popOperandType();
                    emitHelperCall("rotr", "(JJ)J");
                    break;

                case OP_F32_ABS:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "abs", "(F)F", false);
                    break;

                case OP_F32_NEG:
                    method.visitInsn(Opcodes.FNEG);
                    break;

                case OP_F32_CEIL:
                    emitHelperCall("fceil", "(F)F");
                    break;

                case OP_F32_FLOOR:
                    emitHelperCall("ffloor", "(F)F");
                    break;

                case OP_F32_TRUNC:
                    emitHelperCall("ftrunc", "(F)F");
                    break;

                case OP_F32_NEAREST:
                    emitHelperCall("fnearest", "(F)F");
                    break;

                case OP_F32_SQRT:
                    emitHelperCall("fsqrt", "(F)F");
                    break;

                case OP_F32_ADD:
                    popOperandType();
                    method.visitInsn(Opcodes.FADD);
                    break;

                case OP_F32_SUB:
                    popOperandType();
                    method.visitInsn(Opcodes.FSUB);
                    break;

                case OP_F32_MUL:
                    popOperandType();
                    method.visitInsn(Opcodes.FMUL);
                    break;

                case OP_F32_DIV:
                    popOperandType();
                    method.visitInsn(Opcodes.FDIV);
                    break;

                case OP_F32_MIN:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(FF)F", false);
                    break;

                case OP_F32_MAX:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(FF)F", false);
                    break;

                case OP_F32_COPYSIGN:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "copySign", "(FF)F", false);
                    break;

                case OP_F64_ABS:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
                    break;

                case OP_F64_NEG:
                    method.visitInsn(Opcodes.DNEG);
                    break;

                case OP_F64_CEIL:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "ceil", "(D)D", false);
                    break;

                case OP_F64_FLOOR:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "floor", "(D)D", false);
                    break;

                case OP_F64_TRUNC:
                    emitHelperCall("ftrunc", "(D)D");
                    break;

                case OP_F64_NEAREST:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "rint", "(D)D", false);
                    break;

                case OP_F64_SQRT:
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "sqrt", "(D)D", false);
                    break;

                case OP_F64_ADD:
                    popOperandType();
                    method.visitInsn(Opcodes.DADD);
                    break;

                case OP_F64_SUB:
                    popOperandType();
                    method.visitInsn(Opcodes.DSUB);
                    break;

                case OP_F64_MUL:
                    popOperandType();
                    method.visitInsn(Opcodes.DMUL);
                    break;

                case OP_F64_DIV:
                    popOperandType();
                    method.visitInsn(Opcodes.DDIV);
                    break;

                case OP_F64_MIN:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(DD)D", false);
                    break;

                case OP_F64_MAX:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(DD)D", false);
                    break;

                case OP_F64_COPYSIGN:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "copySign", "(DD)D", false);
                    break;

                case OP_I32_WRAP_I64:
                    popOperandType();
                    method.visitInsn(Opcodes.L2I);
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I32_TRUNC_F32_S:
                    popOperandType();
                    emitHelperCall("i32TruncS", "(F)I");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I32_TRUNC_F32_U:
                    popOperandType();
                    emitHelperCall("i32TruncU", "(F)I");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I32_TRUNC_F64_S:
                    popOperandType();
                    emitHelperCall("i32TruncS", "(D)I");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I32_TRUNC_F64_U:
                    popOperandType();
                    emitHelperCall("i32TruncU", "(D)I");
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_EXTEND_I32_S:
                    popOperandType();
                    method.visitInsn(Opcodes.I2L);
                    operandStack.add(ValueType.I64);
                    break;

                case OP_I64_EXTEND_I32_U:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "toUnsignedLong", "(I)J", false);
                    operandStack.add(ValueType.I64);
                    break;

                case OP_I64_TRUNC_F32_S:
                    popOperandType();
                    emitHelperCall("i64TruncS", "(F)J");
                    operandStack.add(ValueType.I64);
                    break;

                case OP_I64_TRUNC_F32_U:
                    popOperandType();
                    emitHelperCall("i64TruncU", "(F)J");
                    operandStack.add(ValueType.I64);
                    break;

                case OP_I64_TRUNC_F64_S:
                    popOperandType();
                    emitHelperCall("i64TruncS", "(D)J");
                    operandStack.add(ValueType.I64);
                    break;

                case OP_I64_TRUNC_F64_U:
                    popOperandType();
                    emitHelperCall("i64TruncU", "(D)J");
                    operandStack.add(ValueType.I64);
                    break;

                case OP_F32_CONVERT_I32_S:
                    popOperandType();
                    method.visitInsn(Opcodes.I2F);
                    operandStack.add(ValueType.F32);
                    break;

                case OP_F32_CONVERT_I32_U:
                    popOperandType();
                    emitHelperCall("f32ConvertU", "(I)F");
                    operandStack.add(ValueType.F32);
                    break;

                case OP_F32_CONVERT_I64_S:
                    popOperandType();
                    method.visitInsn(Opcodes.L2F);
                    operandStack.add(ValueType.F32);
                    break;

                case OP_F32_CONVERT_I64_U:
                    popOperandType();
                    emitHelperCall("f32ConvertU", "(J)F");
                    operandStack.add(ValueType.F32);
                    break;

                case OP_F32_DEMOTE_F64:
                    popOperandType();
                    method.visitInsn(Opcodes.D2F);
                    operandStack.add(ValueType.F32);
                    break;

                case OP_F64_CONVERT_I32_S:
                    popOperandType();
                    method.visitInsn(Opcodes.I2D);
                    operandStack.add(ValueType.F64);
                    break;

                case OP_F64_CONVERT_I32_U:
                    popOperandType();
                    emitHelperCall("f64ConvertU", "(I)D");
                    operandStack.add(ValueType.F64);
                    break;

                case OP_F64_CONVERT_I64_S:
                    popOperandType();
                    method.visitInsn(Opcodes.L2D);
                    operandStack.add(ValueType.F64);
                    break;

                case OP_F64_CONVERT_I64_U:
                    popOperandType();
                    emitHelperCall("f64ConvertU", "(J)D");
                    operandStack.add(ValueType.F64);
                    break;

                case OP_F64_PROMOTE_F32:
                    popOperandType();
                    method.visitInsn(Opcodes.F2D);
                    operandStack.add(ValueType.F64);
                    break;

                case OP_I32_REINTERPRET_F32:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "floatToIntBits", "(F)I", false);
                    operandStack.add(ValueType.I32);
                    break;

                case OP_I64_REINTERPRET_F64:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "doubleToLongBits", "(D)J", false);
                    operandStack.add(ValueType.I32);
                    break;

                case OP_F32_REINTERPRET_I32:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false);
                    operandStack.add(ValueType.F32);
                    break;

                case OP_F64_REINTERPRET_I64:
                    popOperandType();
                    method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false);
                    operandStack.add(ValueType.F64);
                    break;

                case OP_I32_EXTEND8_S:
                    emitHelperCall("i32Extend8", "(I)I");
                    break;

                case OP_I32_EXTEND16_S:
                    emitHelperCall("i32Extend16", "(I)I");
                    break;

                case OP_I64_EXTEND8_S:
                    emitHelperCall("i64Extend8", "(J)J");
                    break;

                case OP_I64_EXTEND16_S:
                    emitHelperCall("i64Extend16", "(J)J");
                    break;

                case OP_I64_EXTEND32_S:
                    method.visitInsn(Opcodes.L2I);
                    method.visitInsn(Opcodes.I2L);
                    break;

                case OP_REF_NULL: {
                    method.visitInsn(Opcodes.ACONST_NULL);
                    operandStack.add(nextReferenceType().toValueType());
                    break;
                }

                case OP_REF_IS_NULL: {
                    popOperandType();
                    emitHelperCall("refIsNull", "(Ljava/lang/Object;)Z");
                    break;
                }

                case OP_REF_FUNC: {
                    var index = reader.nextUnsigned32();
                    var name = "f-" + index;

                    if (index < importedFunctions.size()) {
                        method.visitFieldInsn(Opcodes.GETFIELD, internalName, name, "Ljava/lang/invoke/MethodHandle;");
                    }
                    else {
                        var type = definedFunctions.get(index - importedFunctions.size());
                        var signature = type.getSignatureString();
                        var handle = new Handle(Opcodes.H_INVOKESPECIAL, internalName, name, signature, false);
                        method.visitLdcInsn(handle);
                    }

                    operandStack.add(ValueType.FUNCREF);
                    break;
                }

                case OP_CONT_PREFIX:
                    switch (reader.nextByte()) {
                        case OP_CONT_I32_TRUNC_SAT_F32_S:
                        case OP_CONT_I32_TRUNC_SAT_F32_U:
                        case OP_CONT_I32_TRUNC_SAT_F64_S:
                        case OP_CONT_I32_TRUNC_SAT_F64_U:
                        case OP_CONT_I64_TRUNC_SAT_F32_S:
                        case OP_CONT_I64_TRUNC_SAT_F32_U:
                        case OP_CONT_I64_TRUNC_SAT_F64_S:
                        case OP_CONT_I64_TRUNC_SAT_F64_U:
                        case OP_CONT_MEMORY_INIT:
                        case OP_CONT_DATA_DROP:
                        case OP_CONT_MEMORY_COPY:
                        case OP_CONT_MEMORY_FILL:
                        case OP_CONT_TABLE_INIT:
                        case OP_CONT_ELEM_DROP:
                        case OP_CONT_TABLE_COPY:
                        case OP_CONT_TABLE_GROW:
                        case OP_CONT_TABLE_SIZE:
                        case OP_CONT_TABLE_FILL:
                            // TODO
                            break;
                    }
            }
        }
    }

    private int saveValues(List<ValueType> types) {
        int localsOffset;

        if (locals.isEmpty()) {
            localsOffset = 1;
        } else {
            var lastLocal = locals.get(locals.size() - 1);
            localsOffset = lastLocal.index() + (lastLocal.type().isDoubleWidth() ? 2 : 1);
        }

        for (var i = types.size() - 1; i >= 0; i--) {
            var type = types.get(i);
            method.visitVarInsn(type.getLocalStoreOpcode(), localsOffset);
            localsOffset += type.getWidth();
        }

        return localsOffset;
    }

    private void restoreValues(List<ValueType> types, int localsOffset) {
        for (var type : types) {
            localsOffset -= type.getWidth();
            method.visitVarInsn(type.getLocalLoadOpcode(), localsOffset);
        }
    }

    private Local nextIndexedLocal() throws IOException {
        return locals.get(reader.nextUnsigned32());
    }

    private LabelScope nextBranchTarget() throws IOException {
        return labelStack.get(labelStack.size() - 1 - reader.nextUnsigned32());
    }

    private void emitBranch(LabelScope target) {
        int localOffset;
        if (locals.isEmpty()) {
            localOffset = 0;
        }
        else {
            var last = locals.get(locals.size() - 1);
            localOffset = last.index() + (last.type().isDoubleWidth() ? 2 : 1);
        }

        for (var i = 0; i < ResultTypes.length(target.parameterTypes()); i++) {
            switch (operandStack.get(operandStack.size() - i - 1)) {
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
        }

        for (var i = 0; i < ResultTypes.length(target.parameterTypes()); i++) {
            if (operandStack.get(operandStack.size() - i - 1).isDoubleWidth()) {
                method.visitInsn(Opcodes.POP2);
            }
            else {
                method.visitInsn(Opcodes.POP);
            }
        }

        for (var parameterType : ResultTypes.asList(target.parameterTypes())) {
            switch (parameterType) {
                case I32 -> method.visitVarInsn(Opcodes.ILOAD, (localOffset -= 1));
                case F32 -> method.visitVarInsn(Opcodes.FLOAD, (localOffset -= 1));
                case I64 -> method.visitVarInsn(Opcodes.LLOAD, (localOffset -= 2));
                case F64 -> method.visitVarInsn(Opcodes.DLOAD, (localOffset -= 2));
                case FUNCREF, EXTERNREF -> method.visitVarInsn(Opcodes.ALOAD, (localOffset -= 1));
            }
        }

        method.visitJumpInsn(Opcodes.GOTO, target.targetLabel());
    }

    private FunctionType nextBlockType() throws IOException, CompilationException {
        var code = reader.nextSigned33();
        if (code >= 0) {
            return types.get((int) code);
        }
        else {
            return switch ((byte) (code & 0x7F)) {
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
    }

    private void emitReturn() {
        var returnTypes = labelStack.get(0).parameterTypes();

        if (returnTypes == null) {
            method.visitInsn(Opcodes.RETURN);
        }
        else if (returnTypes instanceof ValueType returnType) {
            var opcode = switch (returnType) {
                case I32 -> Opcodes.IRETURN;
                case F32 -> Opcodes.FRETURN;
                case I64 -> Opcodes.LRETURN;
                case F64 -> Opcodes.DRETURN;
                case FUNCREF, EXTERNREF -> Opcodes.ARETURN;
            };

            method.visitInsn(opcode);
        }
        else {
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
                case 0x03 -> importedGlobals.add(new ImportedGlobal(
                    moduleName,
                    name,
                    new GlobalType(nextValueType(), switch (reader.nextByte()) {
                        case 0x00 -> Mutability.CONST;
                        case 0x01 -> Mutability.VAR;
                        default -> throw new CompilationException("Invalid mutability");
                    })
                ));
                default -> throw new CompilationException("Invalid import description");
            }
        }
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
            if (reader.nextByte() != TYPE_FUNCTION) {
                throw new CompilationException("Invalid function type");
            }

            types.add(new FunctionType(nextValueTypeVector(), nextValueTypeVector()));
        }
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
        return operandStack.remove(operandStack.size() - 1);
    }

    private LabelScope popLabelScope() {
        return labelStack.remove(labelStack.size() - 1);
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
    private static final byte OP_CONT_PREFIX = (byte) 0xfc;

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
    private static final int OP_CONT_TABLE_COPY = 14;
    private static final int OP_CONT_TABLE_GROW = 15;
    private static final int OP_CONT_TABLE_SIZE = 16;
    private static final int OP_CONT_TABLE_FILL = 17;
}
