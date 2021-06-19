package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.EOFException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Byte.toUnsignedLong;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;
import static java.lang.Thread.onSpinWait;
import static jdk.incubator.foreign.MemoryAccess.setByteAtOffset;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.D2F;
import static org.objectweb.asm.Opcodes.D2I;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DCMPG;
import static org.objectweb.asm.Opcodes.DCMPL;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.DUP2_X2;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.F2L;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FCMPG;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.H_INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.I2B;
import static org.objectweb.asm.Opcodes.I2D;
import static org.objectweb.asm.Opcodes.I2F;
import static org.objectweb.asm.Opcodes.I2L;
import static org.objectweb.asm.Opcodes.I2S;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IAND;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IOR;
import static org.objectweb.asm.Opcodes.IREM;
import static org.objectweb.asm.Opcodes.ISHL;
import static org.objectweb.asm.Opcodes.ISHR;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.IUSHR;
import static org.objectweb.asm.Opcodes.IXOR;
import static org.objectweb.asm.Opcodes.L2D;
import static org.objectweb.asm.Opcodes.L2F;
import static org.objectweb.asm.Opcodes.L2I;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LAND;
import static org.objectweb.asm.Opcodes.LCMP;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LOR;
import static org.objectweb.asm.Opcodes.LREM;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.LUSHR;
import static org.objectweb.asm.Opcodes.LXOR;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.objectweb.asm.Opcodes.V17;
import static org.wastastic.CodegenUtils.pushF32Constant;
import static org.wastastic.CodegenUtils.pushF64Constant;
import static org.wastastic.CodegenUtils.pushI32Constant;
import static org.wastastic.CodegenUtils.pushI64Constant;
import static org.wastastic.Empties.EMPTY_DATA_NAME;
import static org.wastastic.Empties.EMPTY_ELEMENTS_NAME;
import static org.wastastic.Importers.IMPORT_FUNCTION_DESCRIPTOR;
import static org.wastastic.Importers.IMPORT_FUNCTION_NAME;
import static org.wastastic.Importers.IMPORT_MEMORY_DESCRIPTOR;
import static org.wastastic.Importers.IMPORT_MEMORY_NAME;
import static org.wastastic.Importers.IMPORT_TABLE_DESCRIPTOR;
import static org.wastastic.Importers.IMPORT_TABLE_NAME;
import static org.wastastic.InstructionImpls.F32_CONVERT_I64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.F32_CONVERT_I64_U_NAME;
import static org.wastastic.InstructionImpls.F32_TRUNC_DESCRIPTOR;
import static org.wastastic.InstructionImpls.F32_TRUNC_NAME;
import static org.wastastic.InstructionImpls.F64_CONVERT_I64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.F64_CONVERT_I64_U_NAME;
import static org.wastastic.InstructionImpls.F64_TRUNC_DESCRIPTOR;
import static org.wastastic.InstructionImpls.F64_TRUNC_NAME;
import static org.wastastic.InstructionImpls.I32_DIV_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_DIV_S_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_F32_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_F32_S_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_F32_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_F32_U_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_F64_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_F64_S_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_F64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_F64_U_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_SAT_F32_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_SAT_F32_U_NAME;
import static org.wastastic.InstructionImpls.I32_TRUNC_SAT_F64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I32_TRUNC_SAT_F64_U_NAME;
import static org.wastastic.InstructionImpls.I64_DIV_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_DIV_S_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_F32_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_F32_S_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_F32_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_F32_U_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_F64_S_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_F64_S_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_F64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_F64_U_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_SAT_F32_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_SAT_F32_U_NAME;
import static org.wastastic.InstructionImpls.I64_TRUNC_SAT_F64_U_DESCRIPTOR;
import static org.wastastic.InstructionImpls.I64_TRUNC_SAT_F64_U_NAME;
import static org.wastastic.Lists.first;
import static org.wastastic.Lists.last;
import static org.wastastic.Lists.removeLast;
import static org.wastastic.Lists.replaceLast;
import static org.wastastic.Names.DOUBLE_INTERNAL_NAME;
import static org.wastastic.Names.FLOAT_INTERNAL_NAME;
import static org.wastastic.Names.GENERATED_INSTANCE_CONSTRUCTOR_DESCRIPTOR;
import static org.wastastic.Names.GENERATED_INSTANCE_INTERNAL_NAME;
import static org.wastastic.Names.INTEGER_INTERNAL_NAME;
import static org.wastastic.Names.LONG_INTERNAL_NAME;
import static org.wastastic.Names.MATH_INTERNAL_NAME;
import static org.wastastic.Names.MEMORY_SEGMENT_DESCRIPTOR;
import static org.wastastic.Names.METHOD_HANDLE_DESCRIPTOR;
import static org.wastastic.Names.METHOD_HANDLE_INTERNAL_NAME;
import static org.wastastic.Names.MODULE_INSTANCE_INTERNAL_NAME;
import static org.wastastic.Names.OBJECT_ARRAY_DESCRIPTOR;
import static org.wastastic.Names.OBJECT_INTERNAL_NAME;
import static org.wastastic.Names.dataFieldName;
import static org.wastastic.Names.elementFieldName;
import static org.wastastic.Names.functionMethodHandleFieldName;
import static org.wastastic.Names.functionMethodName;
import static org.wastastic.Names.globalFieldName;
import static org.wastastic.Names.globalGetterMethodName;
import static org.wastastic.Names.globalSetterMethodName;
import static org.wastastic.Names.memoryFieldName;
import static org.wastastic.Names.tableFieldName;

final class ModuleTranslator {
    private final @NotNull ReadableByteChannel inputChannel;
    private final @NotNull ByteBuffer inputBuffer;

    private final ClassWriter classWriter = new ClassWriter(0);
    private final ClassVisitor classVisitor = classWriter;
    private MethodVisitor functionWriter;

    private final ArrayList<FunctionType> types = new ArrayList<>();
    private final ArrayList<ImportedFunction> importedFunctions = new ArrayList<>();
    private final ArrayList<FunctionType> definedFunctions = new ArrayList<>();
    private final ArrayList<ImportedTable> importedTables = new ArrayList<>();
    private final ArrayList<TableType> definedTables = new ArrayList<>();
    private final ArrayList<ImportedMemory> importedMemories = new ArrayList<>();
    private final ArrayList<MemoryType> definedMemories = new ArrayList<>();
    private final ArrayList<ImportedGlobal> importedGlobals = new ArrayList<>();
    private final ArrayList<DefinedGlobal> definedGlobals = new ArrayList<>();
    private final ArrayList<Export> exports = new ArrayList<>();
    private final ArrayList<ElementSegment> elementSegments = new ArrayList<>();
    private final ArrayList<DataSegment> dataSegments = new ArrayList<>();

    private int nextFunctionIndex;

    private final ResourceScope dataSegmentsScope = ResourceScope.newImplicitScope();

    private final Set<SpecializedLoad> loadOps = new LinkedHashSet<>();
    private final Set<SpecializedStore> storeOps = new LinkedHashSet<>();

    private boolean atUnreachablePoint;
    private int selfArgumentLocalIndex;
    private int firstScratchLocalIndex;
    private final ArrayList<Local> locals = new ArrayList<>();
    private final ArrayList<StackEntry> stack = new ArrayList<>();

    private int startFunctionIndex = -1;

    ModuleTranslator(@NotNull ReadableByteChannel inputChannel) {
        this.inputChannel = inputChannel;
        this.inputBuffer = ByteBuffer.allocateDirect(256).limit(0);
    }

    @NotNull ModuleImpl translate() throws TranslationException, IOException {
        classVisitor.visit(V17, 0, GENERATED_INSTANCE_INTERNAL_NAME, null, OBJECT_INTERNAL_NAME, new String[]{MODULE_INSTANCE_INTERNAL_NAME});

        if (nextByte() != 0x00 ||
            nextByte() != 0x61 ||
            nextByte() != 0x73 ||
            nextByte() != 0x6d
        ) {
            throw new TranslationException("Invalid magic number");
        }

        if (nextByte() != 0x01 ||
            nextByte() != 0x00 ||
            nextByte() != 0x00 ||
            nextByte() != 0x00
        ) {
            throw new TranslationException("Unsupported version");
        }

        while (true) {
            byte sectionId;

            try {
                sectionId = nextByte();
            }
            catch (EOFException ignored) {
                break;
            }

            var sectionSize = nextUnsigned32();

            switch (sectionId) {
                case SECTION_CUSTOM -> skipInput(sectionSize);
                case SECTION_TYPE -> translateTypeSection();
                case SECTION_IMPORT -> translateImportSection();
                case SECTION_FUNCTION -> translateFunctionSection();
                case SECTION_TABLE -> translateTableSection();
                case SECTION_MEMORY -> translateMemorySection();
                case SECTION_GLOBAL -> translateGlobalSection();
                case SECTION_EXPORT -> translateExportSection();
                case SECTION_START -> translateStartSection();
                case SECTION_ELEMENT -> translateElementSection();
                case SECTION_CODE -> translateCodeSection();
                case SECTION_DATA -> translateDataSection();
                case SECTION_DATA_COUNT -> translateDataCountSection();
                default -> throw new TranslationException("Invalid section ID");
            }
        }

        var constructorWriter = classVisitor.visitMethod(ACC_PRIVATE, "<init>", GENERATED_INSTANCE_CONSTRUCTOR_DESCRIPTOR, null, new String[]{ModuleInstantiationException.INTERNAL_NAME});
        constructorWriter.visitCode();
        constructorWriter.visitVarInsn(ALOAD, 0);
        constructorWriter.visitMethodInsn(INVOKESPECIAL, OBJECT_INTERNAL_NAME, "<init>", "()V", false);

        for (var i = 0; i < importedFunctions.size(); i++) {
            var importedFunction = importedFunctions.get(i);
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitVarInsn(ALOAD, 1);
            constructorWriter.visitLdcInsn(importedFunction.qualifiedName().moduleName());
            constructorWriter.visitLdcInsn(importedFunction.qualifiedName().name());
            constructorWriter.visitLdcInsn(importedFunction.type().asmType());
            constructorWriter.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_FUNCTION_NAME, IMPORT_FUNCTION_DESCRIPTOR, false);
            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, functionMethodHandleFieldName(i), METHOD_HANDLE_DESCRIPTOR);
        }

        for (var i = 0; i < importedTables.size(); i++) {
            var importedTable = importedTables.get(i);
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitVarInsn(ALOAD, 1);
            constructorWriter.visitLdcInsn(importedTable.qualifiedName().moduleName());
            constructorWriter.visitLdcInsn(importedTable.qualifiedName().name());
            constructorWriter.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_TABLE_NAME, IMPORT_TABLE_DESCRIPTOR, false);
            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableFieldName(i), Table.DESCRIPTOR);
        }

        for (var i = 0; i < definedTables.size(); i++) {
            var tableType = definedTables.get(i);
            var tableIndex = importedTables.size() + i;
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitTypeInsn(NEW, Table.INTERNAL_NAME);
            constructorWriter.visitInsn(DUP);
            constructorWriter.visitLdcInsn(tableType.limits().unsignedMinimum());
            constructorWriter.visitLdcInsn(tableType.limits().unsignedMaximum());
            constructorWriter.visitMethodInsn(INVOKESPECIAL, Table.INTERNAL_NAME, "<init>", "(II)V", false);
            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableFieldName(tableIndex), Table.DESCRIPTOR);
        }

        for (var i = 0; i < importedMemories.size(); i++) {
            var importedMemory = importedMemories.get(i);
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitVarInsn(ALOAD, 1);
            constructorWriter.visitLdcInsn(importedMemory.qualifiedName().moduleName());
            constructorWriter.visitLdcInsn(importedMemory.qualifiedName().name());
            constructorWriter.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_MEMORY_NAME, IMPORT_MEMORY_DESCRIPTOR, false);
            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryFieldName(i), Memory.DESCRIPTOR);
        }

        for (var i = 0; i < definedMemories.size(); i++) {
            var memoryType = definedMemories.get(i);
            var memoryIndex = importedMemories.size() + i;
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitTypeInsn(NEW, Memory.INTERNAL_NAME);
            constructorWriter.visitInsn(DUP);
            constructorWriter.visitLdcInsn(memoryType.limits().unsignedMinimum());
            constructorWriter.visitLdcInsn(memoryType.limits().unsignedMaximum());
            constructorWriter.visitMethodInsn(INVOKESPECIAL, Memory.INTERNAL_NAME, "<init>", "(II)V", false);
            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryFieldName(memoryIndex), Memory.DESCRIPTOR);
        }

        // TODO: implement global imports

        for (var i = 0; i < definedGlobals.size(); i++) {
            var global = definedGlobals.get(i);
            var globalIndex = importedGlobals.size() + i;
            constructorWriter.visitVarInsn(ALOAD, 0);

            if (global.initialValue() instanceof F32Constant f32Constant) {
                constructorWriter.visitLdcInsn(f32Constant.value());
            }
            else if (global.initialValue() instanceof F64Constant f64Constant) {
                constructorWriter.visitLdcInsn(f64Constant.value());
            }
            else if (global.initialValue() instanceof I32Constant i32Constant) {
                constructorWriter.visitLdcInsn(i32Constant.value());
            }
            else if (global.initialValue() instanceof I64Constant i64Constant) {
                constructorWriter.visitLdcInsn(i64Constant.value());
            }
            else if (global.initialValue() == NullConstant.INSTANCE) {
                constructorWriter.visitInsn(ACONST_NULL);
            }
            else if (global.initialValue() instanceof FunctionRefConstant functionRefConstant) {
                if (functionRefConstant.index() < importedFunctions.size()) {
                    constructorWriter.visitInsn(DUP);
                    functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, functionMethodHandleFieldName(functionRefConstant.index()), METHOD_HANDLE_DESCRIPTOR);
                }
                else {
                    var type = definedFunctions.get(functionRefConstant.index() - importedFunctions.size());
                    var handle = new Handle(H_INVOKESPECIAL, GENERATED_INSTANCE_INTERNAL_NAME, functionMethodName(functionRefConstant.index()), type.descriptor(), false);
                    functionWriter.visitLdcInsn(handle);
                }
            }
            else {
                throw new ClassCastException();
            }

            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, globalFieldName(globalIndex), global.type().valueType().descriptor());
        }

        var nextElementSegmentBootstrapIndex = 0;
        var constantWhatevers = new ArrayList<Constant[]>();

        for (var i = 0; i < elementSegments.size(); i++) {
            var element = elementSegments.get(i);

            if (element.mode() == ElementSegment.Mode.DECLARATIVE) {
                continue;
            }

            constantWhatevers.add(element.values());

            var fieldName = elementFieldName(i);
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitLdcInsn(new ConstantDynamic(fieldName, OBJECT_ARRAY_DESCRIPTOR, GeneratedInstanceClassData.ELEMENT_BOOTSTRAP, nextElementSegmentBootstrapIndex++));

            if (element.mode() == ElementSegment.Mode.ACTIVE) {
                constructorWriter.visitInsn(DUP);
                constructorWriter.visitLdcInsn(element.tableOffset());
                constructorWriter.visitVarInsn(ALOAD, 0);
                constructorWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableFieldName(element.tableIndex()), Table.DESCRIPTOR);
                constructorWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.INIT_FROM_ACTIVE_NAME, Table.INIT_FROM_ACTIVE_DESCRIPTOR, false);
            }

            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, fieldName, OBJECT_ARRAY_DESCRIPTOR);
        }

        for (var i = 0; i < dataSegments.size(); i++) {
            var data = dataSegments.get(i);
            var fieldName = dataFieldName(i);
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitLdcInsn(new ConstantDynamic(fieldName, MEMORY_SEGMENT_DESCRIPTOR, GeneratedInstanceClassData.DATA_BOOTSTRAP, i));

            if (data.mode() == DataSegment.Mode.ACTIVE) {
                constructorWriter.visitInsn(DUP);
                constructorWriter.visitLdcInsn(data.memoryOffset());
                constructorWriter.visitVarInsn(ALOAD, 0);
                constructorWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryFieldName(data.memoryIndex()), Memory.DESCRIPTOR);
                constructorWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.INIT_FROM_ACTIVE_NAME, Memory.INIT_FROM_ACTIVE_DESCRIPTOR, false);
            }

            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, fieldName, MEMORY_SEGMENT_DESCRIPTOR);
        }

        constructorWriter.visitInsn(RETURN);
        constructorWriter.visitMaxs(0, 0);
        constructorWriter.visitEnd();

        for (var loadOp : loadOps) {
            loadOp.writeMethod(classVisitor);
        }

        for (var storeOp : storeOps) {
            storeOp.writeMethod(classVisitor);
        }

        var dataMemorySegments = new MemorySegment[dataSegments.size()];
        for (var i = 0; i < dataSegments.size(); i++) {
            dataMemorySegments[i] = dataSegments.get(i).contents();
        }

        var functionTypes = new FunctionType[importedFunctions.size() + definedFunctions.size()];
        for (var i = 0; i < importedFunctions.size(); i++) {
            functionTypes[i] = importedFunctions.get(i).type();
        }

        for (var i = 0; i < definedFunctions.size(); i++) {
            functionTypes[importedFunctions.size() + i] = definedFunctions.get(i);
        }

        var classData = new GeneratedInstanceClassData(functionTypes, dataMemorySegments, constantWhatevers.toArray(Constant[][]::new));

        classVisitor.visitEnd();
        var bytes = classWriter.toByteArray();
        Files.write(Path.of("GeneratedModuleInstance.class"), bytes);

        MethodHandles.Lookup lookup;
        try {
            lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, classData, false);
        } catch (Throwable e) {
            throw new TranslationException(e);
        }

        var exportedFunctions = new LinkedHashMap<String, ExportedFunction>();
        var exportedTableIndices = new LinkedHashMap<String, Integer>();
        var exportedMemoryIndices = new LinkedHashMap<String, Integer>();

        for (var export : exports) {
            // FIXME: deal with inconsistency in imported method handles v.s. handles to wrapper methods
            switch (export.kind()) {
                case TABLE -> exportedTableIndices.put(export.name(), export.index());
                case FUNCTION -> exportedFunctions.put(export.name(), new ExportedFunction(export.index(), functionTypes[export.index()]));
                case MEMORY -> exportedMemoryIndices.put(export.name(), export.index());
            }
        }

        // FIXME: implement start method

        return new ModuleImpl(lookup, exportedFunctions, exportedTableIndices, exportedMemoryIndices);
    }

    private void translateTypeSection() throws IOException, TranslationException {
        var remaining = nextUnsigned32();
        types.ensureCapacity(types.size() + remaining);
        for (; remaining != 0; remaining--) {
            if (nextByte() != TYPE_FUNCTION) {
                throw new TranslationException("Invalid function type");
            }
            types.add(new FunctionType(nextResultType(), nextResultType()));
        }
    }

    private void translateImportSection() throws IOException, TranslationException {
        for (var remaining = nextUnsigned32(); remaining != 0; remaining--) {
            var moduleName = nextName();
            var name = nextName();
            switch (nextByte()) {
                case 0x00 -> translateFunctionImport(moduleName, name);
                case 0x01 -> translateTableImport(moduleName, name);
                case 0x02 -> translateMemoryImport(moduleName, name);
                case 0x03 -> translateGlobalImport(moduleName, name);
                default -> throw new TranslationException("Invalid import description");
            }
        }
    }

    private void translateFunctionImport(@NotNull String moduleName, @NotNull String name) throws IOException {
        var type = nextIndexedType();
        var index = nextFunctionIndex++;

        var handleFieldName = functionMethodHandleFieldName(index);
        classVisitor.visitField(ACC_PRIVATE | ACC_FINAL, handleFieldName, METHOD_HANDLE_DESCRIPTOR, null, null).visitEnd();

        var wrapperMethod = classVisitor.visitMethod(ACC_PRIVATE | ACC_STATIC, functionMethodName(index), type.descriptor(), null, null);
        wrapperMethod.visitCode();

        var selfArgIndex = 0;
        for (var parameterType : type.parameterTypes()) {
            selfArgIndex += parameterType.width();
        }

        wrapperMethod.visitVarInsn(ALOAD, selfArgIndex);
        wrapperMethod.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, handleFieldName, METHOD_HANDLE_DESCRIPTOR);

        var nextArgIndex = 0;
        for (var parameterType : type.parameterTypes()) {
            wrapperMethod.visitVarInsn(parameterType.localLoadOpcode(), nextArgIndex);
            nextArgIndex += parameterType.width();
        }

        wrapperMethod.visitVarInsn(ALOAD, selfArgIndex);
        wrapperMethod.visitMethodInsn(INVOKEVIRTUAL, METHOD_HANDLE_INTERNAL_NAME, "invokeExact", type.descriptor(), false);
        wrapperMethod.visitInsn(type.returnOpcode());

        wrapperMethod.visitMaxs(0, 0);
        wrapperMethod.visitEnd();

        importedFunctions.add(new ImportedFunction(new QualifiedName(moduleName, name), type));
    }

    private void translateTableImport(@NotNull String moduleName, @NotNull String name) throws TranslationException, IOException {
        var index = importedTables.size();
        classVisitor.visitField(ACC_PRIVATE | ACC_FINAL, tableFieldName(index), Table.DESCRIPTOR, null, null).visitEnd();
        importedTables.add(new ImportedTable(new QualifiedName(moduleName, name), nextTableType()));
    }

    private void translateMemoryImport(@NotNull String moduleName, @NotNull String name) throws TranslationException, IOException {
        var index = importedMemories.size();
        classVisitor.visitField(ACC_PRIVATE | ACC_FINAL, memoryFieldName(index), Memory.DESCRIPTOR, null, null).visitEnd();
        importedMemories.add(new ImportedMemory(new QualifiedName(moduleName, name), nextMemoryType()));
    }

    private void translateGlobalImport(@NotNull String moduleName, @NotNull String name) throws TranslationException, IOException {
        throw new TranslationException("TODO implement global imports");

        // var index = importedGlobals.size();
        // var valueType = nextValueType();

        // var getterFieldName = "g-" + index + "-get-mh";
        // classWriter.visitField(ACC_PRIVATE | ACC_FINAL, getterFieldName, METHOD_HANDLE_DESCRIPTOR, null, null);

        // var getterMethod = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "g-" + index + "-get", '(' + GENERATED_INSTANCE_DESCRIPTOR + ')' + valueType.descriptor(), null, null);
        // getterMethod.visitCode();
        // getterMethod.visitVarInsn(ALOAD, 0);
        // getterMethod.visitInsn(DUP);
        // getterMethod.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, getterFieldName, METHOD_HANDLE_DESCRIPTOR);
        // getterMethod.visitInsn(SWAP);
        // getterMethod.visitMethodInsn(INVOKEVIRTUAL, METHOD_HANDLE_INTERNAL_NAME, "invokeExact", '(' + GENERATED_INSTANCE_DESCRIPTOR + ')' + valueType.descriptor(), false);
        // getterMethod.visitInsn(valueType.returnOpcode());
        // getterMethod.visitMaxs(0, 0);
        // getterMethod.visitEnd();

        // var mutability = switch (nextByte()) {
        //     case 0x00 -> {
        //         yield Mutability.CONST;
        //     }

        //     case 0x01 -> {
        //         var setterFieldName = "g-" + index + "-set-mh";
        //         classWriter.visitField(ACC_PRIVATE | ACC_FINAL, setterFieldName, METHOD_HANDLE_DESCRIPTOR, null, null);

        //         var setterMethod = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "g-" + index + "-set", '(' + valueType.descriptor() + GENERATED_INSTANCE_DESCRIPTOR + ")V", null, null);
        //         setterMethod.visitCode();
        //         setterMethod.visitVarInsn(ALOAD, valueType.width());
        //         setterMethod.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, setterFieldName, METHOD_HANDLE_DESCRIPTOR);
        //         setterMethod.visitVarInsn(valueType.localLoadOpcode(), 0);
        //         setterMethod.visitVarInsn(ALOAD, valueType.width());
        //         setterMethod.visitMethodInsn(INVOKEVIRTUAL, METHOD_HANDLE_INTERNAL_NAME, "invokeExact", '(' + valueType.descriptor() + GENERATED_INSTANCE_DESCRIPTOR + ")V", false);
        //         setterMethod.visitInsn(RETURN);
        //         setterMethod.visitMaxs(0, 0);
        //         setterMethod.visitEnd();

        //         yield Mutability.VAR;
        //     }

        //     default -> throw new TranslationException("Invalid mutability");
        // };

        // importedGlobals.add(new ImportedGlobal(new QualifiedName(moduleName, name), new GlobalType(valueType, mutability)));
    }

    private void translateFunctionSection() throws IOException {
        var remaining = nextUnsigned32();
        definedFunctions.ensureCapacity(definedFunctions.size() + remaining);
        for (; remaining != 0; remaining--) {
            definedFunctions.add(nextIndexedType());
        }
    }

    private void translateTableSection() throws IOException, TranslationException {
        var remaining = nextUnsigned32();
        definedTables.ensureCapacity(definedTables.size() + remaining);
        for (; remaining != 0; remaining--) {
            var index = definedTables.size() + importedTables.size();
            classVisitor.visitField(ACC_PRIVATE | ACC_FINAL, tableFieldName(index), Table.DESCRIPTOR, null, null).visitEnd();
            definedTables.add(nextTableType());
        }
    }

    private void translateMemorySection() throws IOException, TranslationException {
        var remaining = nextUnsigned32();
        definedMemories.ensureCapacity(definedMemories.size() + remaining);
        for (; remaining != 0; remaining--) {
            var index = definedMemories.size() + importedMemories.size();
            classVisitor.visitField(ACC_PRIVATE | ACC_FINAL, memoryFieldName(index), Memory.DESCRIPTOR, null, null).visitEnd();
            definedMemories.add(nextMemoryType());
        }
    }

    private void translateGlobalSection() throws IOException, TranslationException {
        var remaining = nextUnsigned32();
        definedGlobals.ensureCapacity(definedGlobals.size() + remaining);
        for (; remaining != 0; remaining--) {
            var type = nextValueType();
            var index = definedGlobals.size() + importedGlobals.size();
            var access = ACC_PRIVATE;
            var fieldName = globalFieldName(index);

            var getterMethod = classVisitor.visitMethod(ACC_PRIVATE | ACC_STATIC, globalGetterMethodName(index), type.globalGetterDescriptor(), null, null);
            getterMethod.visitCode();
            getterMethod.visitVarInsn(ALOAD, 0);
            getterMethod.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
            getterMethod.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, fieldName, type.descriptor());
            getterMethod.visitInsn(type.returnOpcode());
            getterMethod.visitMaxs(0, 0);
            getterMethod.visitEnd();

            var mutability = switch (nextByte()) {
                case 0x00 -> {
                    access |= ACC_FINAL;
                    yield Mutability.CONST;
                }

                case 0x01 -> {
                    var setterMethod = classVisitor.visitMethod(ACC_PRIVATE | ACC_STATIC, globalSetterMethodName(index), type.globalSetterDescriptor(), null, null);
                    setterMethod.visitCode();
                    setterMethod.visitVarInsn(ALOAD, type.width());
                    setterMethod.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
                    setterMethod.visitVarInsn(type.localLoadOpcode(), 0);
                    setterMethod.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, fieldName, type.descriptor());
                    setterMethod.visitInsn(RETURN);
                    setterMethod.visitMaxs(0, 0);
                    setterMethod.visitEnd();

                    yield Mutability.VAR;
                }

                default -> throw new TranslationException("Invalid mutability");
            };

            classVisitor.visitField(access, fieldName, type.descriptor(), null, null).visitEnd();
            definedGlobals.add(new DefinedGlobal(new GlobalType(type, mutability), nextConstantExpression()));
        }
    }

    private void translateExportSection() throws IOException, TranslationException {
        var remaining = nextUnsigned32();
        exports.ensureCapacity(exports.size() + remaining);
        for (; remaining != 0; remaining--) {
            var name = nextName();

            var kind = switch (nextByte()) {
                case 0x00 -> ExportKind.FUNCTION;
                case 0x01 -> ExportKind.TABLE;
                case 0x02 -> ExportKind.MEMORY;
                case 0x03 -> ExportKind.GLOBAL;
                default -> throw new TranslationException("Invalid export description");
            };

            exports.add(new Export(name, kind, nextUnsigned32()));
        }
    }

    private void translateStartSection() throws IOException {
        startFunctionIndex = nextUnsigned32();
    }

    private void translateElementSection() throws IOException, TranslationException {
        for (var remaining = nextUnsigned32(); remaining != 0; remaining--) {
            translateElementSegment();
        }
    }

    private void translateElementSegment() throws IOException, TranslationException {
        Constant[] values;
        ElementSegment.Mode mode;
        int tableIndex, tableOffset;

        switch (nextByte()) {
            case 0x00 -> {
                mode = ElementSegment.Mode.ACTIVE;
                tableIndex = 0;
                tableOffset = nextI32ConstantExpression();
                var functionIndexCount = nextUnsigned32();
                values = new Constant[functionIndexCount];
                for (var i = 0; i < functionIndexCount; i++) {
                    values[i] = new FunctionRefConstant(nextUnsigned32());
                }
            }

            case 0x01 -> {
                mode = ElementSegment.Mode.PASSIVE;
                tableIndex = 0;
                tableOffset = 0;
                nextElementKind();
                var functionIndexCount = nextUnsigned32();
                values = new Constant[functionIndexCount];
                for (var i = 0; i < functionIndexCount; i++) {
                    values[i] = new FunctionRefConstant(nextUnsigned32());
                }
            }

            case 0x02 -> {
                mode = ElementSegment.Mode.ACTIVE;
                tableIndex = nextUnsigned32();
                tableOffset = nextI32ConstantExpression();
                nextElementKind();
                var functionIndexCount = nextUnsigned32();
                values = new Constant[functionIndexCount];
                for (var i = 0; i < functionIndexCount; i++) {
                    values[i] = new FunctionRefConstant(nextUnsigned32());
                }
            }

            case 0x03 -> {
                mode = ElementSegment.Mode.DECLARATIVE;
                tableIndex = 0;
                tableOffset = 0;
                values = ElementSegment.EMPTY_VALUES;
                nextElementKind();
                var functionIndexCount = nextUnsigned32();
                for (var i = 0; i < functionIndexCount; i++) {
                    nextUnsigned32();
                }
            }

            case 0x04 -> {
                mode = ElementSegment.Mode.ACTIVE;
                tableIndex = 0;
                tableOffset = nextI32ConstantExpression();
                var exprCount = nextUnsigned32();
                values = new Constant[exprCount];
                for (var i = 0; i < exprCount; i++) {
                    values[i] = nextFunctionRefConstantExpression();
                }
            }

            case 0x05 -> {
                mode = ElementSegment.Mode.PASSIVE;
                tableIndex = 0;
                tableOffset = 0;
                var type = nextReferenceType();
                var exprCount = nextUnsigned32();
                values = new Constant[exprCount];
                switch (type) {
                    case FUNCREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            values[i] = nextFunctionRefConstantExpression();
                        }
                    }

                    case EXTERNREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            values[i] = nextExternRefConstantExpression();
                        }
                    }
                }
            }

            case 0x06 -> {
                mode = ElementSegment.Mode.ACTIVE;
                tableIndex = nextUnsigned32();
                tableOffset = nextI32ConstantExpression();
                var type = nextReferenceType();
                var exprCount = nextUnsigned32();
                values = new Constant[exprCount];
                switch (type) {
                    case FUNCREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            values[i] = nextFunctionRefConstantExpression();
                        }
                    }

                    case EXTERNREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            values[i] = nextExternRefConstantExpression();
                        }
                    }
                }
            }

            case 0x07 -> {
                mode = ElementSegment.Mode.DECLARATIVE;
                tableIndex = 0;
                tableOffset = 0;
                values = ElementSegment.EMPTY_VALUES;
                nextReferenceType();
                var type = nextReferenceType();
                var exprCount = nextUnsigned32();
                switch (type) {
                    case FUNCREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            nextFunctionRefConstantExpression();
                        }
                    }

                    case EXTERNREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            nextExternRefConstantExpression();
                        }
                    }
                }
            }

            default -> throw new TranslationException("Invalid element segment");
        }

        if (mode != ElementSegment.Mode.DECLARATIVE) {
            var index = elementSegments.size();
            classVisitor.visitField(ACC_PRIVATE, elementFieldName(index), OBJECT_ARRAY_DESCRIPTOR, null, null).visitEnd();
        }

        elementSegments.add(new ElementSegment(values, mode, tableIndex, tableOffset));
    }

    private void translateCodeSection() throws IOException, TranslationException {
        var functionCount = nextUnsigned32();
        for (; functionCount != 0; functionCount--) {
            nextUnsigned32(); // size, ignored
            translateFunction();
        }
    }

    private void translateFunction() throws IOException, TranslationException {
        atUnreachablePoint = false;
        locals.clear();
        stack.clear();

        // fn 205
        var index = nextFunctionIndex++;
        System.out.println("\nFunction: " + index);
        var type = definedFunctions.get(index);

        functionWriter = classVisitor.visitMethod(ACC_PRIVATE | ACC_STATIC, functionMethodName(index), type.descriptor(), null, null);
        functionWriter.visitCode();

        var nextLocalIndex = 0;

        for (var parameterType : type.parameterTypes()) {
            locals.add(new Local(parameterType, nextLocalIndex));
            nextLocalIndex += parameterType.width();
        }

        selfArgumentLocalIndex = nextLocalIndex;
        nextLocalIndex += 1;

        for (var fieldVectorsRemaining = nextUnsigned32(); fieldVectorsRemaining != 0; fieldVectorsRemaining--) {
            var fieldsRemaining = nextUnsigned32();
            var fieldType = nextValueType();
            for (; fieldsRemaining != 0; fieldsRemaining--) {
                locals.add(new Local(fieldType, nextLocalIndex));
                functionWriter.visitInsn(fieldType.zeroConstantOpcode());
                functionWriter.visitVarInsn(fieldType.localStoreOpcode(), nextLocalIndex);
                nextLocalIndex += fieldType.width();
            }
        }

        firstScratchLocalIndex = nextLocalIndex;

        // FIXME: detect attempt to directly branch to here?
        var block = new BlockScope(new Label(), type);
        stack.add(block);

        while (!stack.isEmpty() && first(stack).equals(block)) {
            translateInstruction();
        }

        // if (!atUnreachablePoint) {
        functionWriter.visitInsn(type.returnOpcode());
        // }

        functionWriter.visitMaxs(0, 0);
        functionWriter.visitEnd();
    }

    private void translateDataSection() throws IOException, TranslationException {
        var dataCount = nextUnsigned32();
        dataSegments.ensureCapacity(dataSegments.size() + dataCount);
        for (; dataCount != 0; dataCount--) {
            var index = dataSegments.size();
            classVisitor.visitField(ACC_PRIVATE, dataFieldName(index), MEMORY_SEGMENT_DESCRIPTOR, null, null).visitEnd();

            DataSegment.Mode mode;
            int memoryIndex, memoryOffset;

            switch (nextByte()) {
                case 0x00 -> {
                    mode = DataSegment.Mode.ACTIVE;
                    memoryIndex = 0;
                    memoryOffset = nextI32ConstantExpression();
                }

                case 0x01 -> {
                    mode = DataSegment.Mode.PASSIVE;
                    memoryIndex = 0;
                    memoryOffset = 0;
                }

                case 0x02 -> {
                    mode = DataSegment.Mode.ACTIVE;
                    memoryIndex = nextUnsigned32();
                    memoryOffset = nextI32ConstantExpression();
                }

                default -> throw new TranslationException("Invalid data segment");
            }

            var contentsSize = nextUnsigned32();
            var contents = nextBytes(Integer.toUnsignedLong(contentsSize), dataSegmentsScope);
            dataSegments.add(new DataSegment(contents, mode, memoryIndex, memoryOffset));
        }
    }

    private void translateDataCountSection() {
        // TODO
    }

    private void translateInstruction() throws IOException, TranslationException {
        var opcode = nextByte();
        // System.out.println("0x" + Integer.toHexString(Byte.toUnsignedInt(opcode)));
        switch (opcode) {
            case OP_UNREACHABLE -> translateUnreachable();
            case OP_NOP -> translateNop();
            case OP_BLOCK -> translateBlock();
            case OP_LOOP -> translateLoop();
            case OP_IF -> translateIf();
            case OP_ELSE -> translateElse();
            case OP_END -> translateEnd();
            case OP_BR -> translateBr();
            case OP_BR_IF -> translateBrIf();
            case OP_BR_TABLE -> translateBrTable();
            case OP_RETURN -> translateReturn();
            case OP_CALL -> translateCall();
            case OP_CALL_INDIRECT -> translateCallIndirect();
            case OP_DROP -> translateDrop();
            case OP_SELECT -> translateSelect();
            case OP_SELECT_VEC -> translateSelectVec();
            case OP_LOCAL_GET -> translateLocalGet();
            case OP_LOCAL_SET -> translateLocalSet();
            case OP_LOCAL_TEE -> translateLocalTee();
            case OP_GLOBAL_GET -> translateGlobalGet();
            case OP_GLOBAL_SET -> translateGlobalSet();
            case OP_TABLE_GET -> translateTableGet();
            case OP_TABLE_SET -> translateTableSet();
            case OP_I32_LOAD -> translateI32Load();
            case OP_I64_LOAD -> translateI64Load();
            case OP_F32_LOAD -> translateF32Load();
            case OP_F64_LOAD -> translateF64Load();
            case OP_I32_LOAD8_S -> translateI32Load8S();
            case OP_I32_LOAD8_U -> translateI32Load8U();
            case OP_I32_LOAD16_S -> translateI32Load16S();
            case OP_I32_LOAD16_U -> translateI32Load16U();
            case OP_I64_LOAD8_S -> translateI64Load8S();
            case OP_I64_LOAD8_U -> translateI64Load8U();
            case OP_I64_LOAD16_S -> translateI64Load16S();
            case OP_I64_LOAD16_U -> translateI64Load16U();
            case OP_I64_LOAD32_S -> translateI64Load32S();
            case OP_I64_LOAD32_U -> translateI64Load32U();
            case OP_I32_STORE -> translateI32Store();
            case OP_I64_STORE -> translateI64Store();
            case OP_F32_STORE -> translateF32Store();
            case OP_F64_STORE -> translateF64Store();
            case OP_I32_STORE8 -> translateI32Store8();
            case OP_I32_STORE16 -> translateI32Store16();
            case OP_I64_STORE8 -> translateI64Store8();
            case OP_I64_STORE16 -> translateI64Store16();
            case OP_I64_STORE32 -> translateI64Store32();
            case OP_MEMORY_SIZE -> translateMemorySize();
            case OP_MEMORY_GROW -> translateMemoryGrow();
            case OP_I32_CONST -> translateI32Const();
            case OP_I64_CONST -> translateI64Const();
            case OP_F32_CONST -> translateF32Const();
            case OP_F64_CONST -> translateF64Const();
            case OP_I32_EQZ -> translateI32Eqz();
            case OP_I32_EQ -> translateI32Eq();
            case OP_I32_NE -> translateI32Ne();
            case OP_I32_LT_S -> translateI32LtS();
            case OP_I32_LT_U -> translateI32LtU();
            case OP_I32_GT_S -> translateI32GtS();
            case OP_I32_GT_U -> translateI32GtU();
            case OP_I32_LE_S -> translateI32LeS();
            case OP_I32_LE_U -> translateI32LeU();
            case OP_I32_GE_S -> translateI32GeS();
            case OP_I32_GE_U -> translateI32GeU();
            case OP_I64_EQZ -> translateI64Eqz();
            case OP_I64_EQ -> translateI64Eq();
            case OP_I64_NE -> translateI64Ne();
            case OP_I64_LT_S -> translateI64LtS();
            case OP_I64_LT_U -> translateI64LtU();
            case OP_I64_GT_S -> translateI64GtS();
            case OP_I64_GT_U -> translateI64GtU();
            case OP_I64_LE_S -> translateI64LeS();
            case OP_I64_LE_U -> translateI64LeU();
            case OP_I64_GE_S -> translateI64GeS();
            case OP_I64_GE_U -> translateI64GeU();
            case OP_F32_EQ -> translateF32Eq();
            case OP_F32_NE -> translateF32Ne();
            case OP_F32_LT -> translateF32Lt();
            case OP_F32_GT -> translateF32Gt();
            case OP_F32_LE -> translateF32Le();
            case OP_F32_GE -> translateF32Ge();
            case OP_F64_EQ -> translateF64Eq();
            case OP_F64_NE -> translateF64Ne();
            case OP_F64_LT -> translateF64Lt();
            case OP_F64_GT -> translateF64Gt();
            case OP_F64_LE -> translateF64Le();
            case OP_F64_GE -> translateF64Ge();
            case OP_I32_CLZ -> translateI32Clz();
            case OP_I32_CTZ -> translateI32Ctz();
            case OP_I32_POPCNT -> translateI32Popcnt();
            case OP_I32_ADD -> translateI32Add();
            case OP_I32_SUB -> translateI32Sub();
            case OP_I32_MUL -> translateI32Mul();
            case OP_I32_DIV_S -> translateI32DivS();
            case OP_I32_DIV_U -> translateI32DivU();
            case OP_I32_REM_S -> translateI32RemS();
            case OP_I32_REM_U -> translateI32RemU();
            case OP_I32_AND -> translateI32And();
            case OP_I32_OR -> translateI32Or();
            case OP_I32_XOR -> translateI32Xor();
            case OP_I32_SHL -> translateI32Shl();
            case OP_I32_SHR_S -> translateI32ShrS();
            case OP_I32_SHR_U -> translateI32ShrU();
            case OP_I32_ROTL -> translateI32Rotl();
            case OP_I32_ROTR -> translateI32Rotr();
            case OP_I64_CLZ -> translateI64Clz();
            case OP_I64_CTZ -> translateI64Ctz();
            case OP_I64_POPCNT -> translateI64Popcnt();
            case OP_I64_ADD -> translateI64Add();
            case OP_I64_SUB -> translateI64Sub();
            case OP_I64_MUL -> translateI64Mul();
            case OP_I64_DIV_S -> translateI64DivS();
            case OP_I64_DIV_U -> translateI64DivU();
            case OP_I64_REM_S -> translateI64RemS();
            case OP_I64_REM_U -> translateI64RemU();
            case OP_I64_AND -> translateI64And();
            case OP_I64_OR -> translateI64Or();
            case OP_I64_XOR -> translateI64Xor();
            case OP_I64_SHL -> translateI64Shl();
            case OP_I64_SHR_S -> translateI64ShrS();
            case OP_I64_SHR_U -> translateI64ShrU();
            case OP_I64_ROTL -> translateI64Rotl();
            case OP_I64_ROTR -> translateI64Rotr();
            case OP_F32_ABS -> translateF32Abs();
            case OP_F32_NEG -> translateF32Neg();
            case OP_F32_CEIL -> translateF32Ceil();
            case OP_F32_FLOOR -> translateF32Floor();
            case OP_F32_TRUNC -> translateF32Trunc();
            case OP_F32_NEAREST -> translateF32Nearest();
            case OP_F32_SQRT -> translateF32Sqrt();
            case OP_F32_ADD -> translateF32Add();
            case OP_F32_SUB -> translateF32Sub();
            case OP_F32_MUL -> translateF32Mul();
            case OP_F32_DIV -> translateF32Div();
            case OP_F32_MIN -> translateF32Min();
            case OP_F32_MAX -> translateF32Max();
            case OP_F32_COPYSIGN -> translateF32Copysign();
            case OP_F64_ABS -> translateF64Abs();
            case OP_F64_NEG -> translateF64Neg();
            case OP_F64_CEIL -> translateF64Ceil();
            case OP_F64_FLOOR -> translateF64Floor();
            case OP_F64_TRUNC -> translateF64Trunc();
            case OP_F64_NEAREST -> translateF64Nearest();
            case OP_F64_SQRT -> translateF64Sqrt();
            case OP_F64_ADD -> translateF64Add();
            case OP_F64_SUB -> translateF64Sub();
            case OP_F64_MUL -> translateF64Mul();
            case OP_F64_DIV -> translateF64Div();
            case OP_F64_MIN -> translateF64Min();
            case OP_F64_MAX -> translateF64Max();
            case OP_F64_COPYSIGN -> translateF64Copysign();
            case OP_I32_WRAP_I64 -> translateI32WrapI64();
            case OP_I32_TRUNC_F32_S -> translateI32TruncF32S();
            case OP_I32_TRUNC_F32_U -> translateI32TruncF32U();
            case OP_I32_TRUNC_F64_S -> translateI32TruncF64S();
            case OP_I32_TRUNC_F64_U -> translateI32TruncF64U();
            case OP_I64_EXTEND_I32_S -> translateI64ExtendI32S();
            case OP_I64_EXTEND_I32_U -> translateI64ExtendI32U();
            case OP_I64_TRUNC_F32_S -> translateI64TruncF32S();
            case OP_I64_TRUNC_F32_U -> translateI64TruncF32U();
            case OP_I64_TRUNC_F64_S -> translateI64TruncF64S();
            case OP_I64_TRUNC_F64_U -> translateI64TruncF64U();
            case OP_F32_CONVERT_I32_S -> translateF32ConvertI32S();
            case OP_F32_CONVERT_I32_U -> translateF32ConvertI32U();
            case OP_F32_CONVERT_I64_S -> translateF32ConvertI64S();
            case OP_F32_CONVERT_I64_U -> translateF32ConvertI64U();
            case OP_F32_DEMOTE_F64 -> translateF32DemoteF64();
            case OP_F64_CONVERT_I32_S -> translateF64ConvertI32S();
            case OP_F64_CONVERT_I32_U -> translateF64ConvertI32U();
            case OP_F64_CONVERT_I64_S -> translateF64ConvertI64S();
            case OP_F64_CONVERT_I64_U -> translateF64ConvertI64U();
            case OP_F64_PROMOTE_F32 -> translateF64PromoteF32();
            case OP_I32_REINTERPRET_F32 -> translateI32ReinterpretF32();
            case OP_I64_REINTERPRET_F64 -> translateI64ReinterpretF64();
            case OP_F32_REINTERPRET_I32 -> translateF32ReinterpretI32();
            case OP_F64_REINTERPRET_I64 -> translateF64ReinterpretI64();
            case OP_I32_EXTEND8_S -> translateI32Extend8S();
            case OP_I32_EXTEND16_S -> translateI32Extend16S();
            case OP_I64_EXTEND8_S -> translateI64Extend8S();
            case OP_I64_EXTEND16_S -> translateI64Extend16S();
            case OP_I64_EXTEND32_S -> translateI64Extend32S();
            case OP_REF_NULL -> translateRefNull();
            case OP_REF_IS_NULL -> translateRefIsNull();
            case OP_REF_FUNC -> translateRefFunc();
            case OP_CONT_PREFIX -> translateCont();
            default -> throw new TranslationException("Invalid opcode: 0x" + Integer.toHexString(Byte.toUnsignedInt(opcode)));
        }
    }

    private void translateUnreachable() {
        functionWriter.visitTypeInsn(NEW, UnreachableException.INTERNAL_NAME);
        functionWriter.visitInsn(DUP);
        functionWriter.visitMethodInsn(INVOKESPECIAL, UnreachableException.INTERNAL_NAME, "<init>", "()V", false);
        functionWriter.visitInsn(ATHROW);
        atUnreachablePoint = true;
    }

    private void translateNop() {
        // Nothing to do
    }

    private void translateBlock() throws TranslationException, IOException {
        var type = nextBlockType();
        // System.out.println("Beginning block: " + type + ", current stack: " + stack);
        checkTopOperands(type.parameterTypes());
        stack.add(stack.size() - type.parameterTypes().size(), new BlockScope(new Label(), type));
    }

    private void translateLoop() throws TranslationException, IOException {
        var startLabel = new Label();
        var type = nextBlockType();
        // System.out.println("Beginning loop: " + type + ", current stack: " + stack);

        checkTopOperands(type.parameterTypes());
        stack.add(stack.size() - type.parameterTypes().size(), new LoopScope(startLabel, type));

        functionWriter.visitLabel(startLabel);
    }

    private void translateIf() throws TranslationException, IOException {
        popOperand(ValueType.I32);

        var elseLabel = new Label();
        var endLabel = new Label();
        var type = nextBlockType();

        checkTopOperands(type.parameterTypes());
        stack.add(stack.size() - type.parameterTypes().size(), new IfScope(elseLabel, endLabel, type));

        functionWriter.visitJumpInsn(IFEQ, elseLabel);
    }

    private void translateElse() throws TranslationException {
        IfScope scope;
        var scopeIndex = stack.size();

        while (true) {
            if (--scopeIndex == -1) {
                throw new TranslationException("Missing IfScope on stack");
            }

            var entry = stack.get(scopeIndex);

            if (entry instanceof ValueType) {
                continue;
            }

            if (entry instanceof IfScope ifScope) {
                scope = ifScope;
                break;
            }

            throw new TranslationException("Unexpected stack entry: " + entry);
        }

        if (!atUnreachablePoint) {
            var actualReturnTypes = stack.subList(scopeIndex + 1, stack.size());
            if (!actualReturnTypes.equals(scope.type().returnTypes())) {
                throw new TranslationException("Return types mismatch at end of IfScope: expected " + scope.type().returnTypes() + ", found " + actualReturnTypes);
            }
        }

        atUnreachablePoint = false;
        stack.subList(scopeIndex, stack.size()).clear();
        stack.add(new BlockScope(scope.endLabel(), scope.type()));
        stack.addAll(scope.type().parameterTypes());

        functionWriter.visitJumpInsn(GOTO, scope.endLabel());
        functionWriter.visitLabel(scope.elseLabel());
    }

    private void translateEnd() throws TranslationException {
        Scope scope;
        var scopeIndex = stack.size();

        while (true) {
            if (--scopeIndex == -1) {
                throw new TranslationException("Missing scope on stack");
            }

            var entry = stack.get(scopeIndex);

            if (entry instanceof ValueType) {
                continue;
            }

            if (entry instanceof Scope s) {
                scope = s;
                break;
            }

            throw new TranslationException("Unexpected stack entry: " + entry);
        }

        // System.out.println("Ending scope: " + scope + ", current stack: " + stack);

        if (!atUnreachablePoint) {
            var actualReturnTypes = stack.subList(scopeIndex + 1, stack.size());
            if (!actualReturnTypes.equals(scope.type().returnTypes())) {
                throw new TranslationException("Invalid stack at end of scope: expected " + scope.type().returnTypes() + " at top of stack, found " + actualReturnTypes);
            }
        }

        stack.subList(scopeIndex, stack.size()).clear();
        stack.addAll(scope.type().returnTypes());

        if (scope instanceof IfScope ifScope) {
            functionWriter.visitLabel(ifScope.elseLabel());
            functionWriter.visitLabel(ifScope.endLabel());
        }
        else if (scope instanceof BlockScope blockScope) {
            functionWriter.visitLabel(blockScope.endLabel());
        }

        atUnreachablePoint = false;
    }

    private void translateBr() throws IOException, TranslationException {
        emitBranch(nextUnsigned32());
        atUnreachablePoint = true;
    }

    private void translateBrIf() throws IOException, TranslationException {
        popOperand(ValueType.I32);

        var pastBranchLabel = new Label();
        functionWriter.visitJumpInsn(IFEQ, pastBranchLabel);
        emitBranch(nextUnsigned32());
        functionWriter.visitLabel(pastBranchLabel);
    }

    private void translateBrTable() throws IOException, TranslationException {
        popOperand(ValueType.I32);

        var indexedTargetCount = nextUnsigned32();
        var indexedTargets = new int[indexedTargetCount];

        for (var i = 0; i < indexedTargetCount; i++) {
            indexedTargets[i] = nextUnsigned32();
        }

        var defaultTarget = nextUnsigned32();

        var indexedAdapterLabels = new Label[indexedTargetCount];
        for (var i = 0; i < indexedTargetCount; i++) {
            indexedAdapterLabels[i] = new Label();
        }

        var defaultAdapterLabel = new Label();

        functionWriter.visitTableSwitchInsn(0, indexedTargetCount - 1, defaultAdapterLabel, indexedAdapterLabels);

        for (var i = 0; i < indexedTargetCount; i++) {
            functionWriter.visitLabel(indexedAdapterLabels[i]);
            emitBranch(indexedTargets[i]);
        }

        functionWriter.visitLabel(defaultAdapterLabel);
        emitBranch(defaultTarget);

        atUnreachablePoint = true;
    }

    private void translateReturn() throws TranslationException {
        var functionScope = (BlockScope) first(stack);
        checkTopOperands(functionScope.type().returnTypes());
        functionWriter.visitInsn(functionScope.type().returnOpcode());
        atUnreachablePoint = true;
    }

    private void translateCall() throws IOException, TranslationException {
        var index = nextUnsigned32();

        FunctionType type;
        if (index < importedFunctions.size()) {
            type = importedFunctions.get(index).type();
        }
        else {
            type = definedFunctions.get(index - importedFunctions.size());
        }

        checkTopOperands(type.parameterTypes());
        removeLast(stack, type.parameterTypes().size());
        stack.addAll(type.returnTypes());

        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, functionMethodName(index), type.descriptor(), false);
    }

    private void translateCallIndirect() throws IOException, TranslationException {
        var type = nextIndexedType();

        popOperand(ValueType.I32);
        checkTopOperands(type.parameterTypes());
        removeLast(stack, type.parameterTypes().size());
        stack.addAll(type.returnTypes());

        emitTableFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.GET_METHOD_NAME, Table.GET_METHOD_DESCRIPTOR, false);
        functionWriter.visitTypeInsn(CHECKCAST, METHOD_HANDLE_INTERNAL_NAME);

        var calleeLocalIndex = firstScratchLocalIndex;
        functionWriter.visitVarInsn(ASTORE, calleeLocalIndex);

        var nextLocalIndex = calleeLocalIndex + 1;
        for (var i = type.parameterTypes().size() - 1; i >= 0; i--) {
            // FIXME: is this okay?
            var parameterType = type.parameterTypes().get(i);
            functionWriter.visitVarInsn(parameterType.localStoreOpcode(), nextLocalIndex);
            nextLocalIndex += parameterType.width();
        }

        functionWriter.visitVarInsn(ALOAD, calleeLocalIndex);

        for (var parameterType : type.parameterTypes()) {
            nextLocalIndex -= parameterType.width();
            functionWriter.visitVarInsn(parameterType.localLoadOpcode(), nextLocalIndex);
        }

        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitMethodInsn(INVOKEVIRTUAL, METHOD_HANDLE_INTERNAL_NAME, "invokeExact", type.descriptor(), false);
    }

    private void translateDrop() throws TranslationException {
        functionWriter.visitInsn(popAnyOperand().isDoubleWidth() ? POP2 : POP);
    }

    private void translateSelect() throws TranslationException {
        popOperand(ValueType.I32);
        var type = popAnyOperand();
        checkTopOperand(type);

        var pastSwapLabel = new Label();
        functionWriter.visitJumpInsn(IFNE, pastSwapLabel);

        if (type.isDoubleWidth()) {
            functionWriter.visitInsn(DUP2_X2);
            functionWriter.visitInsn(POP2);
            functionWriter.visitLabel(pastSwapLabel);
            functionWriter.visitInsn(POP2);
        }
        else {
            functionWriter.visitInsn(SWAP);
            functionWriter.visitLabel(pastSwapLabel);
            functionWriter.visitInsn(POP);
        }
    }

    private void translateSelectVec() throws TranslationException, IOException {
        for (var i = nextUnsigned32(); i != 0; i--) {
            nextValueType();
        }

        translateSelect();
    }

    private void translateLocalGet() throws IOException {
        var local = nextIndexedLocal();
        stack.add(local.type());
        functionWriter.visitVarInsn(local.type().localLoadOpcode(), local.index());
    }

    private void translateLocalSet() throws IOException, TranslationException {
        var local = nextIndexedLocal();
        popOperand(local.type());
        functionWriter.visitVarInsn(local.type().localStoreOpcode(), local.index());
    }

    private void translateLocalTee() throws IOException, TranslationException {
        var local = nextIndexedLocal();
        applyUnaryOp(local.type());
        functionWriter.visitInsn(local.type().isDoubleWidth() ? DUP2 : DUP);
        functionWriter.visitVarInsn(local.type().localStoreOpcode(), local.index());
    }

    private void translateGlobalGet() throws IOException {
        var globalIndex = nextUnsigned32();
        ValueType type;

        if (globalIndex < importedGlobals.size()) {
            type = importedGlobals.get(globalIndex).getType().valueType();
        }
        else {
            type = definedGlobals.get(globalIndex - importedGlobals.size()).type().valueType();
        }

        stack.add(type);

        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, globalGetterMethodName(globalIndex), type.globalGetterDescriptor(), false);
    }

    private void translateGlobalSet() throws IOException, TranslationException {
        var globalIndex = nextUnsigned32();
        ValueType type;

        if (globalIndex < importedGlobals.size()) {
            type = importedGlobals.get(globalIndex).getType().valueType();
        }
        else {
            type = definedGlobals.get(globalIndex - importedGlobals.size()).type().valueType();
        }

        popOperand(type);

        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, globalSetterMethodName(globalIndex), type.globalSetterDescriptor(), false);
    }

    private void translateTableGet() throws IOException, TranslationException {
        var index = nextUnsigned32();

        ValueType elementType;
        if (index < importedTables.size()) {
            elementType = importedTables.get(index).type().elementType();
        } else {
            elementType = definedTables.get(index - importedTables.size()).elementType();
        }

        applyUnaryOp(ValueType.I32, elementType);

        emitTableFieldLoad(index);
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.GET_METHOD_NAME, Table.GET_METHOD_DESCRIPTOR, false);
    }

    private void translateTableSet() throws IOException, TranslationException {
        popReferenceOperand();
        popOperand(ValueType.I32);

        emitTableFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.SET_METHOD_NAME, Table.SET_METHOD_DESCRIPTOR, false);
    }

    private void translateLoad(@NotNull SpecializedLoad.Op op) throws IOException, TranslationException {
        nextUnsigned32(); // expected alignment (ignored)
        var offset = nextUnsigned32();
        var specializedOp = new SpecializedLoad(op, 0, offset);
        loadOps.add(specializedOp);

        applyUnaryOp(ValueType.I32, specializedOp.returnType());

        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, specializedOp.methodName(), specializedOp.methodDescriptor(), false);
    }

    private void translateI32Load() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.I32);
    }

    private void translateI64Load() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.I64);
    }

    private void translateF32Load() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.F32);
    }

    private void translateF64Load() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.F64);
    }

    private void translateI32Load8S() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.I8_AS_I32);
    }

    private void translateI32Load8U() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.U8_AS_I32);
    }

    private void translateI32Load16S() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.I16_AS_I32);
    }

    private void translateI32Load16U() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.U16_AS_I32);
    }

    private void translateI64Load8S() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.I8_AS_I64);
    }

    private void translateI64Load8U() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.U8_AS_I64);
    }

    private void translateI64Load16S() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.I16_AS_I64);
    }

    private void translateI64Load16U() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.U16_AS_I64);
    }

    private void translateI64Load32S() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.I32_AS_I64);
    }

    private void translateI64Load32U() throws IOException, TranslationException {
        translateLoad(SpecializedLoad.Op.U32_AS_I64);
    }

    private void translateStore(@NotNull SpecializedStore.Op op) throws IOException, TranslationException {
        nextUnsigned32(); // expected alignment (ignored)
        var offset = nextUnsigned32();
        var specializedOp = new SpecializedStore(op, 0, offset);
        storeOps.add(specializedOp);

        popOperand(specializedOp.argumentType());
        popOperand(ValueType.I32);

        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, specializedOp.methodName(), specializedOp.methodDescriptor(), false);
    }

    private void translateI32Store() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.I32);
    }

    private void translateI64Store() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.I64);
    }

    private void translateF32Store() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.F32);
    }

    private void translateF64Store() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.F64);
    }

    private void translateI32Store8() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.I32_AS_I8);
    }

    private void translateI32Store16() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.I32_AS_I16);
    }

    private void translateI64Store8() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.I64_AS_I8);
    }

    private void translateI64Store16() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.I64_AS_I16);
    }

    private void translateI64Store32() throws IOException, TranslationException {
        translateStore(SpecializedStore.Op.I64_AS_I32);
    }

    private void translateMemorySize() throws IOException {
        stack.add(ValueType.I32);
        emitMemoryFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.SIZE_METHOD_NAME, Memory.SIZE_METHOD_DESCRIPTOR, false);
    }

    private void translateMemoryGrow() throws IOException, TranslationException {
        applyUnaryOp(ValueType.I32);
        emitMemoryFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.GROW_METHOD_NAME, Memory.GROW_METHOD_DESCRIPTOR, false);
    }

    private void translateI32Const() throws IOException {
        pushI32Constant(functionWriter, nextSigned32());
        stack.add(ValueType.I32);
    }

    private void translateI64Const() throws IOException {
        pushI64Constant(functionWriter, nextSigned64());
        stack.add(ValueType.I64);
    }

    private void translateF32Const() throws IOException {
        pushF32Constant(functionWriter, nextFloat32());
        stack.add(ValueType.F32);
    }

    private void translateF64Const() throws IOException {
        pushF64Constant(functionWriter, nextFloat64());
        stack.add(ValueType.F64);
    }

    private void translateConditionalBoolean(int opcode) {
        var trueLabel = new Label();
        var mergeLabel = new Label();

        functionWriter.visitJumpInsn(opcode, trueLabel);
        functionWriter.visitInsn(ICONST_0);
        functionWriter.visitJumpInsn(GOTO, mergeLabel);

        functionWriter.visitLabel(trueLabel);
        functionWriter.visitInsn(ICONST_1);

        functionWriter.visitLabel(mergeLabel);
    }

    private void translateI32Eqz() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        translateConditionalBoolean(IFEQ);
    }

    private void translateI32ComparisonS(int opcode) throws TranslationException {
        applyBinaryOp(ValueType.I32);
        translateConditionalBoolean(opcode);
    }

    private void translateI32ComparisonU(int opcode) throws TranslationException {
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "compareUnsigned", "(II)I", false);
        translateI32ComparisonS(opcode);
    }

    private void translateI32Eq() throws TranslationException {
        translateI32ComparisonS(IF_ICMPEQ);
    }

    private void translateI32Ne() throws TranslationException {
        translateI32ComparisonS(IF_ICMPNE);
    }

    private void translateI32LtS() throws TranslationException {
        translateI32ComparisonS(IF_ICMPLT);
    }

    private void translateI32LtU() throws TranslationException {
        translateI32ComparisonU(IFLT);
    }

    private void translateI32GtS() throws TranslationException {
        translateI32ComparisonS(IF_ICMPGT);
    }

    private void translateI32GtU() throws TranslationException {
        translateI32ComparisonU(IFGT);
    }

    private void translateI32LeS() throws TranslationException {
        translateI32ComparisonS(IF_ICMPLE);
    }

    private void translateI32LeU() throws TranslationException {
        translateI32ComparisonU(IFLE);
    }

    private void translateI32GeS() throws TranslationException {
        translateI32ComparisonS(IF_ICMPGE);
    }

    private void translateI32GeU() throws TranslationException {
        translateI32ComparisonU(IFGE);
    }

    private void translateI64Eqz() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.I32);
        functionWriter.visitInsn(LCMP);
        translateConditionalBoolean(IFEQ);
    }

    private void translateI64ComparisonS(int opcode) throws TranslationException {
        applyBinaryOp(ValueType.I64, ValueType.I32);
        functionWriter.visitInsn(LCMP);
        translateConditionalBoolean(opcode);
    }

    private void translateI64ComparisonU(int opcode) throws TranslationException {
        applyBinaryOp(ValueType.I64, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "compareUnsigned", "(JJ)I", false);
        translateConditionalBoolean(opcode);
    }

    private void translateI64Eq() throws TranslationException {
        translateI64ComparisonS(IFEQ);
    }

    private void translateI64Ne() throws TranslationException {
        translateI64ComparisonS(IFNE);
    }

    private void translateI64LtS() throws TranslationException {
        translateI64ComparisonS(IFLT);
    }

    private void translateI64LtU() throws TranslationException {
        translateI64ComparisonU(IFLT);
    }

    private void translateI64GtS() throws TranslationException {
        translateI64ComparisonS(IFGT);
    }

    private void translateI64GtU() throws TranslationException {
        translateI64ComparisonU(IFGT);
    }

    private void translateI64LeS() throws TranslationException {
        translateI64ComparisonS(IFLE);
    }

    private void translateI64LeU() throws TranslationException {
        translateI64ComparisonU(IFLE);
    }

    private void translateI64GeS() throws TranslationException {
        translateI64ComparisonS(IFGE);
    }

    private void translateI64GeU() throws TranslationException {
        translateI64ComparisonU(IFGE);
    }

    private void translateFpComparison(@NotNull ValueType type, int cmpOpcode, int branchOpcode) throws TranslationException {
        applyBinaryOp(type, ValueType.I32);
        functionWriter.visitInsn(cmpOpcode);
        translateConditionalBoolean(branchOpcode);
    }

    private void translateF32Eq() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPL, IFEQ);
    }

    private void translateF32Ne() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPL, IFNE);
    }

    private void translateF32Lt() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPG, IFLT);
    }

    private void translateF32Gt() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPL, IFGT);
    }

    private void translateF32Le() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPG, IFLE);
    }

    private void translateF32Ge() throws TranslationException {
        translateFpComparison(ValueType.F32, FCMPL, IFGE);
    }

    private void translateF64Eq() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPL, IFEQ);
    }

    private void translateF64Ne() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPL, IFNE);
    }

    private void translateF64Lt() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPG, IFLT);
    }

    private void translateF64Gt() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPL, IFGE);
    }

    private void translateF64Le() throws TranslationException {
        translateFpComparison(ValueType.F64, FCMPG, IFLE);
    }

    private void translateF64Ge() throws TranslationException {
        translateFpComparison(ValueType.F64, DCMPL, IFGE);
    }

    private void translateI32Clz() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "numberOfLeadingZeros", "(I)I", false);
    }

    private void translateI32Ctz() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "numberOfTrailingZeros", "(I)I", false);
    }

    private void translateI32Popcnt() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "bitCount", "(I)I", false);
    }

    private void translateI32Add() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IADD);
    }

    private void translateI32Sub() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(ISUB);
    }

    private void translateI32Mul() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IMUL);
    }

    private void translateI32DivS() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_DIV_S_NAME, I32_DIV_S_DESCRIPTOR, false);
    }

    private void translateI32DivU() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "divideUnsigned", "(II)I", false);
    }

    private void translateI32RemS() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IREM);
    }

    private void translateI32RemU() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "remainderUnsigned", "(II)I", false);
    }

    private void translateI32And() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IAND);
    }

    private void translateI32Or() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IOR);
    }

    private void translateI32Xor() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IXOR);
    }

    private void translateI32Shl() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(ISHL);
    }

    private void translateI32ShrS() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(ISHR);
    }

    private void translateI32ShrU() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitInsn(IUSHR);
    }

    private void translateI32Rotl() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "rotateLeft", "(II)I", false);
    }

    private void translateI32Rotr() throws TranslationException {
        applyBinaryOp(ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "rotateRight", "(II)I", false);
    }

    private void translateI64Clz() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "numberOfLeadingZeros", "(L)I", false);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Ctz() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "numberOfTrailingZeros", "(L)I", false);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Popcnt() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "bitCount", "(L)I", false);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Add() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LADD);
    }

    private void translateI64Sub() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LSUB);
    }

    private void translateI64Mul() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LMUL);
    }

    private void translateI64DivS() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_DIV_S_NAME, I64_DIV_S_DESCRIPTOR, false);
    }

    private void translateI64DivU() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "divideUnsigned", "(JJ)J", false);
    }

    private void translateI64RemS() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LREM);
    }

    private void translateI64RemU() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "remainderUnsigned", "(JJ)J", false);
    }

    private void translateI64And() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LAND);
    }

    private void translateI64Or() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LOR);
    }

    private void translateI64Xor() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LXOR);
    }

    private void translateI64Shl() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LSHL);
    }

    private void translateI64ShrS() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LSHR);
    }

    private void translateI64ShrU() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(LUSHR);
    }

    private void translateI64Rotl() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "rotateLeft", "(JI)J", false);
    }

    private void translateI64Rotr() throws TranslationException {
        applyBinaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitMethodInsn(INVOKESTATIC, LONG_INTERNAL_NAME, "rotateRight", "(JI)J", false);
    }

    private void translateF32Abs() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "abs", "(F)F", false);
    }

    private void translateF32Neg() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(FNEG);
    }

    private void translateF32Ceil() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(F2D);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "ceil", "(D)D", false);
        functionWriter.visitInsn(D2F);
    }

    private void translateF32Floor() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(F2D);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "floor", "(D)D", false);
        functionWriter.visitInsn(D2F);
    }

    private void translateF32Trunc() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, F32_TRUNC_NAME, F32_TRUNC_DESCRIPTOR, false);
    }

    private void translateF32Nearest() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(F2D);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "rint", "(D)D", false);
        functionWriter.visitInsn(D2F);
    }

    private void translateF32Sqrt() throws TranslationException {
        applyUnaryOp(ValueType.F32);
        functionWriter.visitInsn(F2D);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "sqrt", "(D)D", false);
        functionWriter.visitInsn(D2F);
    }

    private void translateF32Add() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitInsn(FADD);
    }

    private void translateF32Sub() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitInsn(FSUB);
    }

    private void translateF32Mul() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitInsn(FMUL);
    }

    private void translateF32Div() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitInsn(FDIV);
    }

    private void translateF32Min() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "min", "(FF)F", false);
    }

    private void translateF32Max() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "max", "(FF)F", false);
    }

    private void translateF32Copysign() throws TranslationException {
        applyBinaryOp(ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "copySign", "(FF)F", false);
    }

    private void translateF64Abs() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "abs", "(D)D", false);
    }

    private void translateF64Neg() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitInsn(DNEG);
    }

    private void translateF64Ceil() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "ceil", "(D)D", false);
    }

    private void translateF64Floor() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "floor", "(D)D", false);
    }

    private void translateF64Trunc() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, F64_TRUNC_NAME, F64_TRUNC_DESCRIPTOR, false);
    }

    private void translateF64Nearest() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "rint", "(D)D", false);
    }

    private void translateF64Sqrt() throws TranslationException {
        applyUnaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "sqrt", "(D)D", false);
    }

    private void translateF64Add() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitInsn(DADD);
    }

    private void translateF64Sub() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitInsn(DSUB);
    }

    private void translateF64Mul() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitInsn(DMUL);
    }

    private void translateF64Div() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitInsn(DDIV);
    }

    private void translateF64Min() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "min", "(DD)D", false);
    }

    private void translateF64Max() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "max", "(DD)D", false);
    }

    private void translateF64Copysign() throws TranslationException {
        applyBinaryOp(ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, MATH_INTERNAL_NAME, "copySign", "(DD)D", false);
    }

    private void translateI32WrapI64() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.I32);
        functionWriter.visitInsn(L2I);
    }

    private void translateI32TruncF32S() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_F32_S_NAME, I32_TRUNC_F32_S_DESCRIPTOR, false);
    }

    private void translateI32TruncF32U() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_F32_U_NAME, I32_TRUNC_F32_U_DESCRIPTOR, false);
    }

    private void translateI32TruncF64S() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_F64_S_NAME, I32_TRUNC_F64_S_DESCRIPTOR, false);
    }

    private void translateI32TruncF64U() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_F64_U_NAME, I32_TRUNC_F64_U_DESCRIPTOR, false);
    }

    private void translateI64ExtendI32S() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.I64);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64ExtendI32U() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);
    }

    private void translateI64TruncF32S() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_F32_S_NAME, I64_TRUNC_F32_S_DESCRIPTOR, false);
    }

    private void translateI64TruncF32U() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_F32_U_NAME, I64_TRUNC_F32_U_DESCRIPTOR, false);
    }

    private void translateI64TruncF64S() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_F64_S_NAME, I64_TRUNC_F64_S_DESCRIPTOR, false);
    }

    private void translateI64TruncF64U() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_F64_U_NAME, I64_TRUNC_F64_U_DESCRIPTOR, false);
    }

    private void translateF32ConvertI32S() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F32);
        functionWriter.visitInsn(I2F);
    }

    private void translateF32ConvertI32U() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);
        functionWriter.visitInsn(L2F);
    }

    private void translateF32ConvertI64S() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F32);
        functionWriter.visitInsn(L2F);
    }

    private void translateF32ConvertI64U() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, F32_CONVERT_I64_U_NAME, F32_CONVERT_I64_U_DESCRIPTOR, false);
    }

    private void translateF32DemoteF64() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.F32);
        functionWriter.visitInsn(D2F);
    }

    private void translateF64ConvertI32S() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F64);
        functionWriter.visitInsn(I2D);
    }

    private void translateF64ConvertI32U() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, INTEGER_INTERNAL_NAME, "toUnsignedLong", "(I)J", false);
        functionWriter.visitInsn(L2D);
    }

    private void translateF64ConvertI64S() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F64);
        functionWriter.visitInsn(L2D);
    }

    private void translateF64ConvertI64U() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, F64_CONVERT_I64_U_NAME, F64_CONVERT_I64_U_DESCRIPTOR, false);
    }

    private void translateF64PromoteF32() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.F64);
        functionWriter.visitInsn(F2D);
    }

    private void translateI32ReinterpretF32() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, FLOAT_INTERNAL_NAME, "floatToRawIntBits", "(F)I", false);
    }

    private void translateI64ReinterpretF64() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, DOUBLE_INTERNAL_NAME, "doubleToRawLongBits", "(D)J", false);
    }

    private void translateF32ReinterpretI32() throws TranslationException {
        applyUnaryOp(ValueType.I32, ValueType.F32);
        functionWriter.visitMethodInsn(INVOKESTATIC, FLOAT_INTERNAL_NAME, "intBitsToFloat", "(I)F", false);
    }

    private void translateF64ReinterpretI64() throws TranslationException {
        applyUnaryOp(ValueType.I64, ValueType.F64);
        functionWriter.visitMethodInsn(INVOKESTATIC, DOUBLE_INTERNAL_NAME, "longBitsToDouble", "(J)D", false);
    }

    private void translateI32Extend8S() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitInsn(I2B);
    }

    private void translateI32Extend16S() throws TranslationException {
        applyUnaryOp(ValueType.I32);
        functionWriter.visitInsn(I2S);
    }

    private void translateI64Extend8S() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2B);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Extend16S() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2S);
        functionWriter.visitInsn(L2I);
    }

    private void translateI64Extend32S() throws TranslationException {
        applyUnaryOp(ValueType.I64);
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2L);
    }

    private void translateRefNull() throws TranslationException, IOException {
        stack.add(nextReferenceType());
        functionWriter.visitInsn(ACONST_NULL);
    }

    private void translateRefIsNull() throws TranslationException {
        popReferenceOperand();
        stack.add(ValueType.I32);
        translateConditionalBoolean(IFNULL);
    }

    private void translateRefFunc() throws IOException {
        stack.add(ValueType.FUNCREF);

        var index = nextUnsigned32();

        if (index < importedFunctions.size()) {
            functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
            functionWriter.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
            functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, functionMethodHandleFieldName(index), METHOD_HANDLE_DESCRIPTOR);
        }
        else {
            var type = definedFunctions.get(index - importedFunctions.size());
            var handle = new Handle(H_INVOKESPECIAL, GENERATED_INSTANCE_INTERNAL_NAME, functionMethodName(index), type.descriptor(), false);
            functionWriter.visitLdcInsn(handle);
        }
    }

    private void translateCont() throws IOException, TranslationException {
        switch (nextByte()) {
            case OP_CONT_I32_TRUNC_SAT_F32_S -> translateI32TruncSatF32S();
            case OP_CONT_I32_TRUNC_SAT_F32_U -> translateI32TruncSatF32U();
            case OP_CONT_I32_TRUNC_SAT_F64_S -> translateI32TruncSatF64S();
            case OP_CONT_I32_TRUNC_SAT_F64_U -> translateI32TruncSatF64U();
            case OP_CONT_I64_TRUNC_SAT_F32_S -> translateI64TruncSatF32S();
            case OP_CONT_I64_TRUNC_SAT_F32_U -> translateI64TruncSatF32U();
            case OP_CONT_I64_TRUNC_SAT_F64_S -> translateI64TruncSatF64S();
            case OP_CONT_I64_TRUNC_SAT_F64_U -> translateI64TruncSatF64U();
            case OP_CONT_MEMORY_INIT -> translateMemoryInit();
            case OP_CONT_DATA_DROP -> translateDataDrop();
            case OP_CONT_MEMORY_COPY -> translateMemoryCopy();
            case OP_CONT_MEMORY_FILL -> translateMemoryFill();
            case OP_CONT_TABLE_INIT -> translateTableInit();
            case OP_CONT_ELEM_DROP -> translateElemDrop();
            case OP_CONT_TABLE_COPY -> translateTableCopy();
            case OP_CONT_TABLE_GROW -> translateTableGrow();
            case OP_CONT_TABLE_SIZE -> translateTableSize();
            case OP_CONT_TABLE_FILL -> translateTableFill();
            default -> throw new TranslationException("Invalid opcode");
        }
    }

    private void translateI32TruncSatF32S() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitInsn(F2I);
    }

    private void translateI32TruncSatF32U() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_SAT_F32_U_NAME, I32_TRUNC_SAT_F32_U_DESCRIPTOR, false);
    }

    private void translateI32TruncSatF64S() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I32);
        functionWriter.visitInsn(D2I);
    }

    private void translateI32TruncSatF64U() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I32);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I32_TRUNC_SAT_F64_U_NAME, I32_TRUNC_SAT_F64_U_DESCRIPTOR, false);
    }

    private void translateI64TruncSatF32S() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I64);
        functionWriter.visitInsn(F2L);
    }

    private void translateI64TruncSatF32U() throws TranslationException {
        applyUnaryOp(ValueType.F32, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_SAT_F32_U_NAME, I64_TRUNC_SAT_F32_U_DESCRIPTOR, false);
    }

    private void translateI64TruncSatF64S() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitInsn(D2L);
    }

    private void translateI64TruncSatF64U() throws TranslationException {
        applyUnaryOp(ValueType.F64, ValueType.I64);
        functionWriter.visitMethodInsn(INVOKESTATIC, InstructionImpls.INTERNAL_NAME, I64_TRUNC_SAT_F64_U_NAME, I64_TRUNC_SAT_F64_U_DESCRIPTOR, false);
    }

    private void translateMemoryInit() throws IOException, TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitDataFieldLoad(nextUnsigned32());
        emitMemoryFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.INIT_METHOD_NAME, Memory.INIT_METHOD_DESCRIPTOR, false);
    }

    private void translateDataDrop() throws IOException {
        var index = nextUnsigned32();
        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
        functionWriter.visitFieldInsn(GETSTATIC, Empties.INTERNAL_NAME, EMPTY_DATA_NAME, MEMORY_SEGMENT_DESCRIPTOR);
        functionWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, dataFieldName(index), MEMORY_SEGMENT_DESCRIPTOR);
    }

    private void translateMemoryCopy() throws IOException, TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitMemoryFieldLoad(nextUnsigned32());
        emitMemoryFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.COPY_METHOD_NAME, Memory.COPY_METHOD_DESCRIPTOR, false);
    }

    private void translateMemoryFill() throws IOException, TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitMemoryFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.FILL_METHOD_NAME, Memory.FILL_METHOD_DESCRIPTOR, false);
    }

    private void translateTableInit() throws IOException, TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitElementFieldLoad(nextUnsigned32());
        emitTableFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.INIT_METHOD_NAME, Table.INIT_METHOD_DESCRIPTOR, false);
    }

    private void translateElemDrop() throws IOException {
        var index = nextUnsigned32();
        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
        functionWriter.visitFieldInsn(GETSTATIC, Empties.INTERNAL_NAME, EMPTY_ELEMENTS_NAME, OBJECT_ARRAY_DESCRIPTOR);
        functionWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, elementFieldName(index), OBJECT_ARRAY_DESCRIPTOR);
    }

    private void translateTableCopy() throws IOException, TranslationException {
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);
        popOperand(ValueType.I32);

        emitTableFieldLoad(nextUnsigned32());
        emitTableFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.COPY_METHOD_NAME, Table.COPY_METHOD_DESCRIPTOR, false);
    }

    private void translateTableGrow() throws IOException, TranslationException {
        popOperand(ValueType.I32);
        popReferenceOperand();
        stack.add(ValueType.I32);

        emitTableFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.GROW_METHOD_NAME, Table.GROW_METHOD_DESCRIPTOR, false);
    }

    private void translateTableSize() throws IOException {
        stack.add(ValueType.I32);

        emitTableFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.SIZE_METHOD_NAME, Table.SIZE_METHOD_DESCRIPTOR, false);
    }

    private void translateTableFill() throws IOException, TranslationException {
        popOperand(ValueType.I32);
        popReferenceOperand();
        popOperand(ValueType.I32);

        emitTableFieldLoad(nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.FILL_METHOD_NAME, Table.FILL_METHOD_DESCRIPTOR, false);
    }

    //==================================================================================================================

    private void checkHasOperand() throws TranslationException {
        if (stack.isEmpty() || !(last(stack) instanceof ValueType)) {
            throw new TranslationException("Operand stack underflow");
        }
    }

    private void checkTopOperand(@NotNull ValueType requiredType) throws TranslationException {
        checkHasOperand();
        if (!last(stack).equals(requiredType)) {
            throw new TranslationException("Wrong type at top of operand stack: expected " + requiredType + ", found " + last(stack));
        }
    }

    private void popOperand(@NotNull ValueType requiredType) throws TranslationException {
        checkTopOperand(requiredType);
        removeLast(stack);
    }

    private @NotNull ValueType popReferenceOperand() throws TranslationException {
        checkHasOperand();
        if (!last(stack).equals(ValueType.EXTERNREF) || !last(stack).equals(ValueType.FUNCREF)) {
            throw new TranslationException("Wrong type at top of operand stack: expected reference type, found " + last(stack));
        }
        return (ValueType) removeLast(stack);
    }

    private @NotNull ValueType popAnyOperand() throws TranslationException {
        checkHasOperand();
        return (ValueType) removeLast(stack);
    }

    private void checkTopOperands(@NotNull List<ValueType> requiredTypes) throws TranslationException {
        var topTypes = stack.subList(Integer.max(stack.size() - requiredTypes.size(), 0), stack.size());
        if (!topTypes.equals(requiredTypes)) {
            throw new TranslationException("Wrong operand types at top of stack: expected " + requiredTypes + ", found " + topTypes);
        }
    }

    private void applyUnaryOp(@NotNull ValueType inType, @NotNull ValueType outType) throws TranslationException {
        popOperand(inType);
        stack.add(outType);
    }

    private void applyUnaryOp(@NotNull ValueType type) throws TranslationException {
        checkTopOperand(type);
    }

    private void applyBinaryOp(@NotNull ValueType inType, @NotNull ValueType outType) throws TranslationException {
        popOperand(inType);
        popOperand(inType);
        stack.add(outType);
    }

    private void applyBinaryOp(@NotNull ValueType type) throws TranslationException {
        popOperand(type);
        checkTopOperand(type);
    }

    //==================================================================================================================

    private void emitBranch(int index) throws TranslationException {
        var scopeIndex = stack.size();
        var scopesRemaining = index;

        while (scopesRemaining != -1) {
            if (--scopeIndex == -1) {
                throw new TranslationException("Label index out of bounds");
            }

            var entry = stack.get(scopeIndex);

            if (entry instanceof ValueType) {
                continue;
            }

            if (entry instanceof Scope) {
                scopesRemaining--;
            }
        }

        var scope = (Scope) stack.get(scopeIndex);

        Label targetLabel;
        List<ValueType> labelParameterTypes;

        if (scope instanceof BlockScope blockScope) {
            targetLabel = blockScope.endLabel();
            labelParameterTypes = blockScope.type().returnTypes();
        }
        else if (scope instanceof IfScope ifScope) {
            targetLabel = ifScope.endLabel();
            labelParameterTypes = ifScope.type().returnTypes();
        }
        else if (scope instanceof LoopScope loopScope) {
            targetLabel = loopScope.startLabel();
            labelParameterTypes = scope.type().parameterTypes();
        }
        else {
            throw new ClassCastException();
        }

        checkTopOperands(labelParameterTypes);

        var nextLocalIndex = firstScratchLocalIndex;

        for (var i = labelParameterTypes.size() - 1; i >= 0; i--) {
            var parameterType = labelParameterTypes.get(i);
            functionWriter.visitVarInsn(parameterType.localStoreOpcode(), nextLocalIndex);
            nextLocalIndex += parameterType.width();
        }

        for (var i = stack.size() - labelParameterTypes.size() - 1; i >= 0; i--) {
            if (stack.get(i).equals(scope)) {
                break;
            }

            if (stack.get(i) instanceof ValueType valueType) {
                functionWriter.visitInsn(valueType.isDoubleWidth() ? POP2 : POP);
            }
        }

        for (var parameterType : labelParameterTypes) {
            nextLocalIndex -= parameterType.width();
            functionWriter.visitVarInsn(parameterType.localLoadOpcode(), nextLocalIndex);
        }

        functionWriter.visitJumpInsn(GOTO, targetLabel);
    }

    private void emitDataFieldLoad(int index) {
        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
        functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, dataFieldName(index), MEMORY_SEGMENT_DESCRIPTOR);
    }

    private void emitElementFieldLoad(int index) {
        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
        functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, elementFieldName(index), OBJECT_ARRAY_DESCRIPTOR);
    }

    private void emitMemoryFieldLoad(int index) {
        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
        functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryFieldName(index), Memory.DESCRIPTOR);
    }

    private void emitTableFieldLoad(int index) {
        functionWriter.visitVarInsn(ALOAD, selfArgumentLocalIndex);
        functionWriter.visitTypeInsn(CHECKCAST, GENERATED_INSTANCE_INTERNAL_NAME);
        functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableFieldName(index), Table.DESCRIPTOR);
    }

    //==================================================================================================================

    private @NotNull FunctionType nextBlockType() throws IOException, TranslationException {
        var code = nextSigned33();
        if (code >= 0) {
            return types.get((int) code);
        }
        else {
            return switch ((byte) (code & 0x7F)) {
                case 0x40 -> new FunctionType(List.of(), List.of());
                case TYPE_I32 -> new FunctionType(List.of(), List.of(ValueType.I32));
                case TYPE_I64 -> new FunctionType(List.of(), List.of(ValueType.I64));
                case TYPE_F32 -> new FunctionType(List.of(), List.of(ValueType.F32));
                case TYPE_F64 -> new FunctionType(List.of(), List.of(ValueType.F64));
                case TYPE_EXTERNREF -> new FunctionType(List.of(), List.of(ValueType.EXTERNREF));
                case TYPE_FUNCREF -> new FunctionType(List.of(), List.of(ValueType.FUNCREF));
                default -> throw new TranslationException("Invalid block type");
            };
        }
    }

    private byte nextByte() throws IOException {
        if (!inputBuffer.hasRemaining()) {
            fetchInput();
        }

        return inputBuffer.get();
    }

    private @NotNull MemorySegment nextBytes(long length, @NotNull ResourceScope scope) throws IOException {
        var segment = MemorySegment.allocateNative(length, 8, scope);

        for (var i = 0L; i != length; i++) {
            setByteAtOffset(segment, i, nextByte());
        }

        return segment;
    }

    private @NotNull Constant nextConstantExpression() throws IOException, TranslationException {
        var value = switch (nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_I32_CONST -> new I32Constant(nextSigned32());
            case OP_I64_CONST -> new I64Constant(nextSigned64());
            case OP_REF_NULL -> NullConstant.INSTANCE;
            case OP_REF_FUNC -> new FunctionRefConstant(nextUnsigned32());
            default -> throw new TranslationException("Invalid constant expression");
        };

        if (nextByte() != OP_END) {
            throw new TranslationException("Invalid constant expression");
        }

        return value;
    }

    private void nextElementKind() throws IOException, TranslationException {
        if (nextByte() != 0) {
            throw new TranslationException("Unsupported elemkind");
        }
    }

    private @NotNull Constant nextExternRefConstantExpression() throws IOException, TranslationException {
        var value = switch (nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_REF_NULL -> NullConstant.INSTANCE;
            default -> throw new TranslationException("Invalid externref constant expression");
        };

        if (nextByte() != OP_END) {
            throw new TranslationException("Invalid externref constant expression");
        }

        return value;
    }

    private float nextFloat32() throws IOException {
        var b0 = toUnsignedInt(nextByte());
        var b1 = toUnsignedInt(nextByte());
        var b2 = toUnsignedInt(nextByte());
        var b3 = toUnsignedInt(nextByte());
        var bits = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
        return intBitsToFloat(bits);
    }

    private double nextFloat64() throws IOException {
        var b0 = toUnsignedLong(nextByte());
        var b1 = toUnsignedLong(nextByte());
        var b2 = toUnsignedLong(nextByte());
        var b3 = toUnsignedLong(nextByte());
        var b4 = toUnsignedLong(nextByte());
        var b5 = toUnsignedLong(nextByte());
        var b6 = toUnsignedLong(nextByte());
        var b7 = toUnsignedLong(nextByte());
        var bits = (b7 << 56) | (b6 << 48) | (b5 << 40) | (b4 << 32) | (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
        return longBitsToDouble(bits);
    }

    private @NotNull Constant nextFunctionRefConstantExpression() throws IOException, TranslationException {
        var value = switch (nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_REF_NULL -> NullConstant.INSTANCE;
            case OP_REF_FUNC -> new FunctionRefConstant(nextUnsigned32());
            default -> throw new TranslationException("Invalid funcref constant expression");
        };

        if (nextByte() != OP_END) {
            throw new TranslationException("Invalid funcref constant expression");
        }

        return value;
    }

    private int nextI32ConstantExpression() throws TranslationException, IOException {
        var value = switch (nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_I32_CONST -> nextSigned32();
            default -> throw new TranslationException("Invalid i32 constant expression");
        };

        if (nextByte() != OP_END) {
            throw new TranslationException("Invalid i32 constant expression");
        }

        return value;
    }

    private @NotNull Local nextIndexedLocal() throws IOException {
        return locals.get(nextUnsigned32());
    }

    private @NotNull FunctionType nextIndexedType() throws IOException {
        return types.get(nextUnsigned32());
    }

    private @NotNull Limits nextLimits() throws TranslationException, IOException {
        return switch (nextByte()) {
            case 0x00 -> new Limits(nextUnsigned32());
            case 0x01 -> new Limits(nextUnsigned32(), nextUnsigned32());
            default -> throw new TranslationException("Invalid limits encoding");
        };
    }

    private @NotNull MemoryType nextMemoryType() throws TranslationException, IOException {
        return new MemoryType(nextLimits());
    }

    private @NotNull String nextName() throws IOException {
        return nextUtf8(nextUnsigned32());
    }

    private @NotNull ValueType nextReferenceType() throws TranslationException, IOException {
        return switch (nextByte()) {
            case TYPE_EXTERNREF -> ValueType.EXTERNREF;
            case TYPE_FUNCREF -> ValueType.FUNCREF;
            default -> throw new TranslationException("Invalid reference type");
        };
    }

    private @NotNull List<ValueType> nextResultType() throws IOException, TranslationException {
        var unsignedSize = nextUnsigned32();
        switch (unsignedSize) {
            case 0:
                return List.of();
            case 1:
                return List.of(nextValueType());
            default: {
                var array = new ValueType[unsignedSize];

                for (var i = 0; i != array.length; i++) {
                    array[i] = nextValueType();
                }

                return Arrays.asList(array);
            }
        }
    }

    private int nextSigned32() throws IOException {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffff80;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffc000;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffe00000;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xf0000000;
            }
            return total;
        }

        total |= nextByte() << 28;
        return total;
    }

    private long nextSigned33() throws IOException {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffffff80L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffffc000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffe00000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffffffff0000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 28;
        if (b > 63) {
            total |= 0xfffffff800000000L;
        }
        return total;
    }

    private long nextSigned64() throws IOException {
        byte b;
        var total = 0L;

        total |= (b = nextByte()) & 0x7f;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffffff80L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffffc000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xffffffffffe00000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffffffff0000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 28;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffffff800000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 35;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffffc0000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 42;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xfffe000000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 49;
        if (b >= 0) {
            if (b > 63) {
                total |= 0xff00000000000000L;
            }
            return total;
        }

        total |= ((b = nextByte()) & 0x7fL) << 56;
        if (b >= 0) {
            if (b > 63) {
                total |= 0x8000000000000000L;
            }
            return total;
        }

        total |= (long) nextByte() << 63;
        return total;
    }

    private @NotNull TableType nextTableType() throws TranslationException, IOException {
        return new TableType(nextReferenceType(), nextLimits());
    }

    private int nextUnsigned32() throws IOException {
        byte b;
        var total = 0;

        total |= (b = nextByte()) & 0x7f;
        if (b >= 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 7;
        if (b >= 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 14;
        if (b >= 0) {
            return total;
        }

        total |= ((b = nextByte()) & 0x7f) << 21;
        if (b >= 0) {
            return total;
        }

        total |= nextByte() << 28;
        return total;
    }

    private @NotNull String nextUtf8(int length) throws IOException {
        if (length == 0) {
            return "";
        }

        var bytes = new byte[length];

        for (var i = 0; i < length; i++) {
            bytes[i] = nextByte();
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private @NotNull ValueType nextValueType() throws IOException, TranslationException {
        var code = nextByte();
        return switch (code) {
            case TYPE_EXTERNREF -> ValueType.EXTERNREF;
            case TYPE_FUNCREF -> ValueType.FUNCREF;
            case TYPE_F64 -> ValueType.F64;
            case TYPE_F32 -> ValueType.F32;
            case TYPE_I64 -> ValueType.I64;
            case TYPE_I32 -> ValueType.I32;
            default -> throw new TranslationException("Invalid value type: " + Integer.toHexString(Byte.toUnsignedInt(code)));
        };
    }

    //==================================================================================================================

    private void fetchInput() throws IOException {
        inputBuffer.clear();

        while (true) {
            var bytesRead = inputChannel.read(inputBuffer);

            if (bytesRead == -1) {
                throw new EOFException();
            }

            if (bytesRead != 0) {
                break;
            }

            onSpinWait();
        }

        inputBuffer.flip();
    }

    private byte peekByte() throws IOException {
        if (!inputBuffer.hasRemaining()) {
            fetchInput();
        }

        return inputBuffer.get(inputBuffer.position());
    }

    private void skipInput(long count) throws IOException {
        if (count <= inputBuffer.remaining()) {
            inputBuffer.position(inputBuffer.position() + (int) count);
        }
        else {
            count -= inputBuffer.remaining();
            inputBuffer.position(inputBuffer.limit());

            if (inputChannel instanceof SeekableByteChannel seekableChannel) {
                seekableChannel.position(seekableChannel.position() + count);
            }
            else {
                while (true) {
                    fetchInput();

                    if (count <= inputBuffer.remaining()) {
                        inputBuffer.position((int) count);
                        return;
                    }

                    count -= inputBuffer.position();
                }
            }
        }
    }

    //==================================================================================================================

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
