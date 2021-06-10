package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.D2F;
import static org.objectweb.asm.Opcodes.D2I;
import static org.objectweb.asm.Opcodes.D2L;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.F2D;
import static org.objectweb.asm.Opcodes.F2I;
import static org.objectweb.asm.Opcodes.F2L;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
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
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IOR;
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
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LOR;
import static org.objectweb.asm.Opcodes.LSHL;
import static org.objectweb.asm.Opcodes.LSHR;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.LUSHR;
import static org.objectweb.asm.Opcodes.LXOR;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.wastastic.Names.GENERATED_CONSTRUCTOR_DESCRIPTOR;
import static org.wastastic.Names.GENERATED_INSTANCE_DESCRIPTOR;
import static org.wastastic.Names.GENERATED_INSTANCE_INTERNAL_NAME;

final class ModuleTranslator {

    private static final String MAP_INTERNAL_NAME = "java/util/Map";
    private static final String MAP_DESCRIPTOR = "Ljava/util/Map;";

    private static final String METHOD_HANDLE_INTERNAL_NAME = "java/lang/invoke/MethodHandle";
    private static final String METHOD_HANDLE_DESCRIPTOR = "Ljava/lang/invoke/MethodHandle;";

    private static final String OBJECT_DESCRIPTOR = "Ljava/lang/Object;";

    private static final String MAP_GET_DESCRIPTOR = '(' + OBJECT_DESCRIPTOR + ')' + OBJECT_DESCRIPTOR;

    private final ModuleReader reader;

    private final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
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
    private final ArrayList<MemorySegment> dataSegments = new ArrayList<>();

    private final Set<SpecializedLoadOp> loadOps = new LinkedHashSet<>();
    private final Set<SpecializedStoreOp> storeOps = new LinkedHashSet<>();

    private int moduleArgumentLocalIndex;
    private int firstScratchLocalIndex;
    private final ArrayList<Local> locals = new ArrayList<>();
    private final ArrayList<ValueType> operandStack = new ArrayList<>();
    private final ArrayList<LabelScope> labelStack = new ArrayList<>();

    private int startFunctionIndex = -1;

    private static @NotNull String functionFieldName(int index) {
        return "f-" + index + "-mh";
    }

    private static @NotNull String tableFieldName(int index) {
        return "t-" + index;
    }

    private static @NotNull String memoryFieldName(int index) {
        return "m-" + index;
    }

    ModuleTranslator(@NotNull ModuleReader reader) {
        this.reader = reader;
    }

    void translate() throws TranslationException, IOException {
        if (reader.nextByte() != 0x00 ||
            reader.nextByte() != 0x61 ||
            reader.nextByte() != 0x73 ||
            reader.nextByte() != 0x6d
        ) {
            throw new TranslationException("Invalid magic number");
        }

        if (reader.nextByte() != 0x01 ||
            reader.nextByte() != 0x00 ||
            reader.nextByte() != 0x00 ||
            reader.nextByte() != 0x00
        ) {
            throw new TranslationException("Unsupported version");
        }

        while (true) {
            byte sectionId;

            try {
                sectionId = reader.nextByte();
            }
            catch (EOFException ignored) {
                break;
            }

            var sectionSize = reader.nextUnsigned32();

            switch (sectionId) {
                case SECTION_CUSTOM -> reader.skip(sectionSize);
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

        var constructorWriter = classWriter.visitMethod(ACC_PRIVATE, "<init>", GENERATED_CONSTRUCTOR_DESCRIPTOR, null, new String[]{ModuleInstantiationException.INTERNAL_NAME});
        constructorWriter.visitCode();

        for (var i = 0; i < importedFunctions.size(); i++) {
            var importedFunction = importedFunctions.get(i);
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitVarInsn(ALOAD, 1);
            constructorWriter.visitLdcInsn(importedFunction.qualifiedName().moduleName());
            constructorWriter.visitLdcInsn(importedFunction.qualifiedName().name());
            constructorWriter.visitLdcInsn(importedFunction.type().asmType());
            constructorWriter.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, Importers.IMPORT_FUNCTION_NAME, Importers.IMPORT_FUNCTION_DESCRIPTOR, false);
            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, functionFieldName(i), METHOD_HANDLE_DESCRIPTOR);
        }

        for (var i = 0; i < importedTables.size(); i++) {
            var importedTable = importedTables.get(i);
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitVarInsn(ALOAD, 1);
            constructorWriter.visitLdcInsn(importedTable.qualifiedName().moduleName());
            constructorWriter.visitLdcInsn(importedTable.qualifiedName().name());
            constructorWriter.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, Importers.IMPORT_TABLE_NAME, Importers.IMPORT_TABLE_DESCRIPTOR, false);
            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableFieldName(i), Table.DESCRIPTOR);
        }

        for (var i = 0; i < importedMemories.size(); i++) {
            var importedMemory = importedMemories.get(i);
            constructorWriter.visitVarInsn(ALOAD, 0);
            constructorWriter.visitVarInsn(ALOAD, 1);
            constructorWriter.visitLdcInsn(importedMemory.qualifiedName().moduleName());
            constructorWriter.visitLdcInsn(importedMemory.qualifiedName().name());
            constructorWriter.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, Importers.IMPORT_MEMORY_NAME, Importers.IMPORT_MEMORY_DESCRIPTOR, false);
            constructorWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryFieldName(i), Memory.DESCRIPTOR);
        }

        // TODO: implement global imports

        for (var i = 0; i < elementSegments.size(); i++) {
            new ConstantDynamic("element", "[Ljava/lang/Object;", null, i);
        }

        constructorWriter.visitInsn(RETURN);
        constructorWriter.visitMaxs(0, 0);
        constructorWriter.visitEnd();
    }

    private void translateTypeSection() throws IOException, TranslationException {
        var remaining = reader.nextUnsigned32();
        types.ensureCapacity(types.size() + remaining);
        for (; remaining != 0; remaining--) {
            if (reader.nextByte() != TYPE_FUNCTION) {
                throw new TranslationException("Invalid function type");
            }
            types.add(new FunctionType(nextResultType(), nextResultType()));
        }
    }

    private void translateImportSection() throws IOException, TranslationException {
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            var moduleName = nextName();
            var name = nextName();
            switch (reader.nextByte()) {
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
        var index = importedFunctions.size();

        var handleFieldName = "f-" + index + "-mh";
        classWriter.visitField(ACC_PRIVATE | ACC_FINAL, handleFieldName, METHOD_HANDLE_DESCRIPTOR, null, null);

        var wrapperMethod = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "f-" + index, type.descriptor(), null, null);
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
        classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "t-" + index, Table.DESCRIPTOR, null, null);
        importedTables.add(new ImportedTable(new QualifiedName(moduleName, name), nextTableType()));
    }

    private void translateMemoryImport(@NotNull String moduleName, @NotNull String name) throws TranslationException, IOException {
        importedMemories.add(new ImportedMemory(new QualifiedName(moduleName, name), nextMemoryType()));
    }

    private void translateGlobalImport(@NotNull String moduleName, @NotNull String name) throws TranslationException, IOException {
        var index = importedGlobals.size();
        var valueType = nextValueType();

        var getterFieldName = "g-" + index + "-get-mh";
        classWriter.visitField(ACC_PRIVATE | ACC_FINAL, getterFieldName, METHOD_HANDLE_DESCRIPTOR, null, null);

        var getterMethod = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "g-" + index + "-get", '(' + GENERATED_INSTANCE_DESCRIPTOR + ')' + valueType.descriptor(), null, null);
        getterMethod.visitCode();
        getterMethod.visitVarInsn(ALOAD, 0);
        getterMethod.visitInsn(DUP);
        getterMethod.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, getterFieldName, METHOD_HANDLE_DESCRIPTOR);
        getterMethod.visitInsn(SWAP);
        getterMethod.visitMethodInsn(INVOKEVIRTUAL, METHOD_HANDLE_INTERNAL_NAME, "invokeExact", '(' + GENERATED_INSTANCE_DESCRIPTOR + ')' + valueType.descriptor(), false);
        getterMethod.visitInsn(valueType.returnOpcode());
        getterMethod.visitMaxs(0, 0);
        getterMethod.visitEnd();

        var mutability = switch (reader.nextByte()) {
            case 0x00 -> {
                yield Mutability.CONST;
            }

            case 0x01 -> {
                var setterFieldName = "g-" + index + "-set-mh";
                classWriter.visitField(ACC_PRIVATE | ACC_FINAL, setterFieldName, METHOD_HANDLE_DESCRIPTOR, null, null);

                var setterMethod = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "g-" + index + "-set", '(' + valueType.descriptor() + GENERATED_INSTANCE_DESCRIPTOR + ")V", null, null);
                setterMethod.visitCode();
                setterMethod.visitVarInsn(ALOAD, valueType.width());
                setterMethod.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, setterFieldName, METHOD_HANDLE_DESCRIPTOR);
                setterMethod.visitVarInsn(valueType.localLoadOpcode(), 0);
                setterMethod.visitVarInsn(ALOAD, valueType.width());
                setterMethod.visitMethodInsn(INVOKEVIRTUAL, METHOD_HANDLE_INTERNAL_NAME, "invokeExact", '(' + valueType.descriptor() + GENERATED_INSTANCE_DESCRIPTOR + ")V", false);
                setterMethod.visitInsn(RETURN);
                setterMethod.visitMaxs(0, 0);
                setterMethod.visitEnd();

                yield Mutability.VAR;
            }

            default -> throw new TranslationException("Invalid mutability");
        };

        importedGlobals.add(new ImportedGlobal(new QualifiedName(moduleName, name), new GlobalType(valueType, mutability)));
    }

    private void translateFunctionSection() throws IOException {
        var remaining = reader.nextUnsigned32();
        definedFunctions.ensureCapacity(definedFunctions.size() + remaining);
        for (; remaining != 0; remaining--) {
            definedFunctions.add(nextIndexedType());
        }
    }

    private void translateTableSection() throws IOException, TranslationException {
        var remaining = reader.nextUnsigned32();
        definedTables.ensureCapacity(definedTables.size() + remaining);
        for (; remaining != 0; remaining--) {
            var index = definedTables.size() + importedTables.size();
            classWriter.visitField(ACC_PRIVATE | ACC_FINAL, "t-" + index, Table.DESCRIPTOR, null, null);
            definedTables.add(nextTableType());
        }
    }

    private void translateMemorySection() throws IOException, TranslationException {
        var remaining = reader.nextUnsigned32();
        definedMemories.ensureCapacity(definedMemories.size() + remaining);
        for (; remaining != 0; remaining--) {
            definedMemories.add(nextMemoryType());
        }
    }

    private void translateGlobalSection() throws IOException, TranslationException {
        var remaining = reader.nextUnsigned32();
        definedGlobals.ensureCapacity(definedGlobals.size() + remaining);
        for (; remaining != 0; remaining--) {
            var type = nextValueType();
            var index = definedGlobals.size() + importedGlobals.size();
            var access = ACC_PRIVATE;
            var fieldName = "g-" + index;

            var getterMethod = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "g-" + index + "-get", '(' + GENERATED_INSTANCE_DESCRIPTOR + ')' + type.descriptor(), null, null);
            getterMethod.visitCode();
            getterMethod.visitVarInsn(ALOAD, 0);
            getterMethod.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, fieldName, type.descriptor());
            getterMethod.visitInsn(type.returnOpcode());
            getterMethod.visitMaxs(0, 0);
            getterMethod.visitEnd();

            var mutability = switch (reader.nextByte()) {
                case 0x00 -> {
                    access |= ACC_FINAL;
                    yield Mutability.CONST;
                }

                case 0x01 -> {
                    var setterMethod = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "g-" + index + "-set", '(' + type.descriptor() + GENERATED_INSTANCE_DESCRIPTOR + ")V", null, null);
                    setterMethod.visitCode();
                    setterMethod.visitVarInsn(ALOAD, type.width());
                    setterMethod.visitVarInsn(type.localLoadOpcode(), 0);
                    setterMethod.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, fieldName, type.descriptor());
                    setterMethod.visitInsn(RETURN);
                    setterMethod.visitMaxs(0, 0);
                    setterMethod.visitEnd();

                    yield Mutability.VAR;
                }

                default -> throw new TranslationException("Invalid mutability");
            };

            classWriter.visitField(access, fieldName, type.descriptor(), null, null);

            var initialValue = nextConstantExpression();
            definedGlobals.add(new DefinedGlobal(new GlobalType(type, mutability), initialValue));
        }
    }

    private @NotNull Constant nextConstantExpression() throws IOException, TranslationException {
        var value = switch (reader.nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_I32_CONST -> new I32Constant(reader.nextSigned32());
            case OP_I64_CONST -> new I64Constant(reader.nextSigned64());
            case OP_REF_NULL -> NullConstant.INSTANCE;
            case OP_REF_FUNC -> new FunctionRefConstant(reader.nextUnsigned32());
            default -> throw new TranslationException("Invalid constant expression");
        };

        if (reader.nextByte() != OP_END) {
            throw new TranslationException("Invalid constant expression");
        }

        return value;
    }

    private void translateExportSection() throws IOException, TranslationException {
        var remaining = reader.nextUnsigned32();
        exports.ensureCapacity(exports.size() + remaining);
        for (; remaining != 0; remaining--) {
            var name = nextName();

            var kind = switch (reader.nextByte()) {
                case 0x00 -> ExportKind.FUNCTION;
                case 0x01 -> ExportKind.TABLE;
                case 0x02 -> ExportKind.MEMORY;
                case 0x03 -> ExportKind.GLOBAL;
                default -> throw new TranslationException("Invalid export description");
            };

            var index = reader.nextUnsigned32();

            exports.add(new Export(name, kind, index));
        }
    }

    private void translateStartSection() throws IOException {
        startFunctionIndex = reader.nextUnsigned32();
    }

    private int nextI32ConstantExpression() throws TranslationException, IOException {
        var value = switch (reader.nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_I32_CONST -> reader.nextSigned32();
            default -> throw new TranslationException("Invalid i32 constant expression");
        };

        if (reader.nextByte() != OP_END) {
            throw new TranslationException("Invalid i32 constant expression");
        }

        return value;
    }

    private void nextElementKind() throws IOException, TranslationException {
        if (reader.nextByte() != 0) {
            throw new TranslationException("Unsupported elemkind");
        }
    }

    private void translateElementSection() throws IOException, TranslationException {
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            translateElementSegment();
        }
    }

    private void translateElementSegment() throws IOException, TranslationException {
        Constant[] values;
        int tableIndex;
        int tableOffset;

        switch (reader.nextByte()) {
            case 0x00 -> {
                tableIndex = 0;
                tableOffset = nextI32ConstantExpression();
                var functionIndexCount = reader.nextUnsigned32();
                values = new Constant[functionIndexCount];
                for (var i = 0; i < functionIndexCount; i++) {
                    values[i] = new FunctionRefConstant(reader.nextUnsigned32());
                }
            }

            case 0x01 -> {
                tableIndex = -1;
                tableOffset = -1;
                nextElementKind();
                var functionIndexCount = reader.nextUnsigned32();
                values = new Constant[functionIndexCount];
                for (var i = 0; i < functionIndexCount; i++) {
                    values[i] = new FunctionRefConstant(reader.nextUnsigned32());
                }
            }

            case 0x02 -> {
                tableIndex = reader.nextUnsigned32();
                tableOffset = nextI32ConstantExpression();
                nextElementKind();
                var functionIndexCount = reader.nextUnsigned32();
                values = new Constant[functionIndexCount];
                for (var i = 0; i < functionIndexCount; i++) {
                    values[i] = new FunctionRefConstant(reader.nextUnsigned32());
                }
            }

            case 0x03 -> {
                nextElementKind();
                var functionIndexCount = reader.nextUnsigned32();
                for (var i = 0; i < functionIndexCount; i++) {
                    reader.nextUnsigned32();
                }
                return;
            }

            case 0x04 -> {
                tableIndex = 0;
                tableOffset = nextI32ConstantExpression();
                var exprCount = reader.nextUnsigned32();
                values = new Constant[exprCount];
                for (var i = 0; i < exprCount; i++) {
                    values[i] = nextFunctionRefConstant();
                }
            }

            case 0x05 -> {
                tableIndex = -1;
                tableOffset = -1;
                var type = nextReferenceType();
                var exprCount = reader.nextUnsigned32();
                values = new Constant[exprCount];
                switch (type) {
                    case FUNCREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            values[i] = nextFunctionRefConstant();
                        }
                    }

                    case EXTERNREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            values[i] = nextExternRefConstant();
                        }
                    }
                }
            }

            case 0x06 -> {
                tableIndex = reader.nextUnsigned32();
                tableOffset = nextI32ConstantExpression();
                var type = nextReferenceType();
                var exprCount = reader.nextUnsigned32();
                values = new Constant[exprCount];
                switch (type) {
                    case FUNCREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            values[i] = nextFunctionRefConstant();
                        }
                    }

                    case EXTERNREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            values[i] = nextExternRefConstant();
                        }
                    }
                }
            }

            case 0x07 -> {
                nextReferenceType();
                var type = nextReferenceType();
                var exprCount = reader.nextUnsigned32();
                switch (type) {
                    case FUNCREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            nextFunctionRefConstant();
                        }
                    }

                    case EXTERNREF -> {
                        for (var i = 0; i < exprCount; i++) {
                            nextExternRefConstant();
                        }
                    }
                }
                return;
            }

            default -> throw new TranslationException("Invalid element segment");
        }

        elementSegments.add(new ElementSegment(values, tableIndex, tableOffset));
    }

    private @NotNull Constant nextExternRefConstant() throws IOException, TranslationException {
        var value = switch (reader.nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_REF_NULL -> NullConstant.INSTANCE;
            default -> throw new TranslationException("Invalid externref constant expression");
        };

        if (reader.nextByte() != OP_END) {
            throw new TranslationException("Invalid externref constant expression");
        }

        return value;
    }

    private @NotNull Constant nextFunctionRefConstant() throws IOException, TranslationException {
        var value = switch (reader.nextByte()) {
            case OP_GLOBAL_GET -> throw new TranslationException("TODO implement global.get constants");
            case OP_REF_NULL -> NullConstant.INSTANCE;
            case OP_REF_FUNC -> new FunctionRefConstant(reader.nextUnsigned32());
            default -> throw new TranslationException("Invalid funcref constant expression");
        };

        if (reader.nextByte() != OP_END) {
            throw new TranslationException("Invalid funcref constant expression");
        }

        return value;
    }

    private void translateCodeSection() {
        // TODO
    }

    private void translateFunction(int index) throws IOException, TranslationException {
        locals.clear();
        operandStack.clear();
        labelStack.clear();

        var type = definedFunctions.get(index);

        functionWriter = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "f-" + index, type.descriptor(), null, null);
        functionWriter.visitCode();

        var nextLocalIndex = 0;

        for (var parameterType : type.parameterTypes()) {
            locals.add(new Local(parameterType, nextLocalIndex));
            nextLocalIndex += parameterType.width();
        }

        moduleArgumentLocalIndex = nextLocalIndex;
        nextLocalIndex += 1;

        for (var fieldVectorsRemaining = reader.nextUnsigned32(); fieldVectorsRemaining != 0; fieldVectorsRemaining--) {
            var fieldsRemaining = reader.nextUnsigned32();
            var fieldType = nextValueType();
            for (; fieldsRemaining != 0; fieldsRemaining--) {
                locals.add(new Local(fieldType, nextLocalIndex));
                nextLocalIndex += fieldType.width();
            }
        }

        firstScratchLocalIndex = nextLocalIndex;

        var returnLabel = new Label();
        labelStack.add(new LabelScope(returnLabel, type.returnTypes(), 0, returnLabel, null, null));

        while (!labelStack.isEmpty()) {
            translateInstruction();
        }

        functionWriter.visitInsn(type.returnOpcode());
        functionWriter.visitMaxs(0, 0);
        functionWriter.visitEnd();
    }

    private void translateDataSection() {
        // TODO
    }

    private void translateDataCountSection() {
        // TODO
    }

    private void translateInstruction() throws IOException, TranslationException {
        switch (reader.nextByte()) {
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
            default -> throw new TranslationException("Invalid opcode");
        }
    }

    private void translateUnreachable() {
        emitHelperCall("trap", "()Ljava/lang/RuntimeException;");
        functionWriter.visitInsn(ATHROW);
    }

    private void translateNop() {
        // Nothing to do
    }

    private void translateBlock() throws TranslationException, IOException {
        var type = nextBlockType();
        var endLabel = new Label();
        labelStack.add(new LabelScope(
            endLabel,
            type.returnTypes(),
            operandStack.size() - type.parameterTypes().size(),
            endLabel,
            null,
            null
        ));
    }

    private void translateLoop() throws TranslationException, IOException {
        var type = nextBlockType();
        var startLabel = new Label();
        functionWriter.visitLabel(startLabel);
        labelStack.add(new LabelScope(
            startLabel,
            type.parameterTypes(),
            operandStack.size() - type.parameterTypes().size(),
            null,
            null,
            null
        ));
    }

    private void translateIf() throws TranslationException, IOException {
        var type = nextBlockType();
        var endLabel = new Label();
        var elseLabel = new Label();

        labelStack.add(new LabelScope(
            endLabel,
            type.returnTypes(),
            operandStack.size() - type.parameterTypes().size() - 1,
            endLabel,
            elseLabel,
            type.parameterTypes()
        ));

        functionWriter.visitJumpInsn(IFEQ, elseLabel);
        popOperandType();
    }

    private void translateElse() {
        var scope = popLabelScope();
        functionWriter.visitJumpInsn(GOTO, scope.targetLabel());
        functionWriter.visitLabel(scope.elseLabel());

        operandStack.subList(scope.operandStackSize(), operandStack.size()).clear();
        operandStack.addAll(scope.elseParameterTypes());

        labelStack.add(new LabelScope(
            scope.targetLabel(),
            scope.parameterTypes(),
            scope.operandStackSize(),
            scope.endLabel(),
            null,
            null
        ));
    }

    private void translateEnd() {
        var scope = popLabelScope();

        if (scope.elseLabel() != null) {
            functionWriter.visitLabel(scope.elseLabel());
        }

        if (scope.endLabel() != null) {
            functionWriter.visitLabel(scope.endLabel());
        }
    }

    private void translateBr() throws IOException {
        emitBranch(nextBranchTarget());
    }

    private void translateBrIf() throws IOException {
        var pastBranchLabel = new Label();
        functionWriter.visitJumpInsn(IFEQ, pastBranchLabel);
        popOperandType();
        emitBranch(nextBranchTarget());
        functionWriter.visitLabel(pastBranchLabel);
    }

    private void translateBrTable() throws IOException {
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

        functionWriter.visitTableSwitchInsn(0, indexedTargetCount - 1, defaultAdapterLabel, indexedAdapterLabels);
        popOperandType();

        for (var i = 0; i < indexedTargetCount; i++) {
            functionWriter.visitLabel(indexedAdapterLabels[i]);
            emitBranch(indexedTargets[i]);
        }

        functionWriter.visitLabel(defaultAdapterLabel);
        emitBranch(defaultTarget);
    }

    private void translateReturn() {
        emitBranch(labelStack.get(0));
    }

    private void translateCall() throws IOException {
        var index = reader.nextUnsigned32();
        var name = "f-" + index;

        FunctionType type;
        if (index < importedFunctions.size()) {
            type = importedFunctions.get(index).type();
        }
        else {
            type = definedFunctions.get(importedFunctions.size() - index);
        }

        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, name, type.descriptor(), false);
        operandStack.subList(operandStack.size() - type.returnTypes().size(), operandStack.size()).clear();
        operandStack.addAll(type.returnTypes());
    }

    private void translateCallIndirect() throws IOException {
        var type = nextIndexedType();
        var tableIndex = reader.nextUnsigned32();
        emitLoadTable(tableIndex);
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Table", "get", "(ILorg/wastastic/Table;)Ljava/lang/Object", false);
        functionWriter.visitInsn(DUP);
        functionWriter.visitLdcInsn(Type.getMethodType(type.descriptor()));
        emitHelperCall("checkFunctionType", "(Ljava/lang/Object;Ljava/lang/invoke/MethodType;)V");

        var calleeLocalIndex = firstScratchLocalIndex;
        functionWriter.visitVarInsn(ASTORE, calleeLocalIndex);

        var nextLocalIndex = calleeLocalIndex + 1;
        var returnTypes = type.returnTypes();
        for (var i = returnTypes.size() - 1; i >= 0; i--) {
            var returnType = returnTypes.get(i);
            functionWriter.visitVarInsn(returnType.localStoreOpcode(), nextLocalIndex);
            nextLocalIndex += returnType.width();
        }

        functionWriter.visitVarInsn(ALOAD, calleeLocalIndex);

        for (var returnType : returnTypes) {
            nextLocalIndex -= returnType.width();
            functionWriter.visitVarInsn(returnType.localLoadOpcode(), nextLocalIndex);
        }

        emitLoadModule();

        functionWriter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", type.descriptor(), false);
        operandStack.subList(operandStack.size() - type.parameterTypes().size() - 1, operandStack.size()).clear();
        operandStack.addAll(returnTypes);
    }

    private void translateDrop() {
        functionWriter.visitInsn(popOperandType().isDoubleWidth() ? POP2 : POP);
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

    private void translateSelectVec() throws TranslationException, IOException {
        for (var i = reader.nextUnsigned32(); i != 0; i--) {
            nextValueType();
        }

        translateSelect();
    }

    private void translateLocalGet() throws IOException {
        var local = nextIndexedLocal();
        functionWriter.visitVarInsn(local.type().localLoadOpcode(), local.index());
        operandStack.add(local.type());
    }

    private void translateLocalSet() throws IOException {
        var local = nextIndexedLocal();
        functionWriter.visitVarInsn(local.type().localStoreOpcode(), local.index());
        popOperandType();
    }

    private void translateLocalTee() throws IOException {
        var local = nextIndexedLocal();
        functionWriter.visitInsn(local.type().isDoubleWidth() ? DUP2 : DUP);
        functionWriter.visitVarInsn(local.type().localStoreOpcode(), local.index());
    }

    private void translateGlobalGet() throws IOException {
        var globalIndex = reader.nextUnsigned32();
        ValueType type;

        if (globalIndex < importedGlobals.size()) {
            type = importedGlobals.get(globalIndex).getType().valueType();
        }
        else {
            type = definedGlobals.get(globalIndex - importedGlobals.size()).type().valueType();
        }

        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, "g-" + globalIndex + "-get", "(Lorg/wastastic/Module;)" + type.descriptor(), false);
        operandStack.add(type);
    }

    private void translateGlobalSet() throws IOException {
        var globalIndex = reader.nextUnsigned32();
        ValueType type;

        if (globalIndex < importedGlobals.size()) {
            type = importedGlobals.get(globalIndex).getType().valueType();
        }
        else {
            type = definedGlobals.get(globalIndex - importedGlobals.size()).type().valueType();
        }

        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, "g-" + globalIndex + "-set", "(" + type.descriptor() + "Lorg/wastastic/Module;" + ")V", false);
        popOperandType();
    }

    private void emitLoadTable(int index) {
        emitLoadModule();
        functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, "t-" + index, "Lorg/wastastic/Table;");
    }

    private @NotNull TableType indexedTableType(int index) {
        if (index < importedTables.size()) {
            return importedTables.get(index).getType();
        } else {
            return definedTables.get(index - importedTables.size());
        }
    }

    private void translateTableGet() throws IOException {
        var index = reader.nextUnsigned32();
        emitLoadTable(index);
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Table", "get", "(ILorg/wastastic/Table;)Ljava/lang/Object;", false);
        operandStack.add(indexedTableType(index).elementType().toValueType());
    }

    private void translateTableSet() throws IOException {
        emitLoadTable(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Table", "set", "(ILjava/lang/Object;Lorg/wastastic/Table;)V", false);
        popOperandType();
    }

    private void translateLoad(@NotNull SpecializedLoadOp.Op op) throws IOException {
        reader.nextUnsigned32(); // expected alignment (ignored)
        var offset = reader.nextUnsigned32();
        var specializedOp = new SpecializedLoadOp(op, 0, offset);
        loadOps.add(specializedOp);
        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, specializedOp.methodName(), specializedOp.methodDescriptor(), false);
        replaceTopOperandType(specializedOp.returnType());
    }

    private void translateI32Load() throws IOException {
        translateLoad(SpecializedLoadOp.Op.I32);
    }

    private void translateI64Load() throws IOException {
        translateLoad(SpecializedLoadOp.Op.I64);
    }

    private void translateF32Load() throws IOException {
        translateLoad(SpecializedLoadOp.Op.F32);
    }

    private void translateF64Load() throws IOException {
        translateLoad(SpecializedLoadOp.Op.F64);
    }

    private void translateI32Load8S() throws IOException {
        translateLoad(SpecializedLoadOp.Op.I8_AS_I32);
    }

    private void translateI32Load8U() throws IOException {
        translateLoad(SpecializedLoadOp.Op.U8_AS_I32);
    }

    private void translateI32Load16S() throws IOException {
        translateLoad(SpecializedLoadOp.Op.I16_AS_I32);
    }

    private void translateI32Load16U() throws IOException {
        translateLoad(SpecializedLoadOp.Op.U16_AS_I32);
    }

    private void translateI64Load8S() throws IOException {
        translateLoad(SpecializedLoadOp.Op.I8_AS_I64);
    }

    private void translateI64Load8U() throws IOException {
        translateLoad(SpecializedLoadOp.Op.U8_AS_I64);
    }

    private void translateI64Load16S() throws IOException {
        translateLoad(SpecializedLoadOp.Op.I16_AS_I64);
    }

    private void translateI64Load16U() throws IOException {
        translateLoad(SpecializedLoadOp.Op.U16_AS_I64);
    }

    private void translateI64Load32S() throws IOException {
        translateLoad(SpecializedLoadOp.Op.I32_AS_I64);
    }

    private void translateI64Load32U() throws IOException {
        translateLoad(SpecializedLoadOp.Op.U32_AS_I64);
    }

    private void translateStore(@NotNull SpecializedStoreOp.Op op) throws IOException {
        reader.nextUnsigned32(); // expected alignment (ignored)
        var offset = reader.nextUnsigned32();
        var specializedOp = new SpecializedStoreOp(op, 0, offset);
        storeOps.add(specializedOp);
        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, GENERATED_INSTANCE_INTERNAL_NAME, specializedOp.methodName(), specializedOp.methodDescriptor(), false);
        popOperandType(); // stored value arg
        popOperandType(); // address arg
    }

    private void translateI32Store() throws IOException {
        translateStore(SpecializedStoreOp.Op.I32);
    }

    private void translateI64Store() throws IOException {
        translateStore(SpecializedStoreOp.Op.I64);
    }

    private void translateF32Store() throws IOException {
        translateStore(SpecializedStoreOp.Op.F32);
    }

    private void translateF64Store() throws IOException {
        translateStore(SpecializedStoreOp.Op.F64);
    }

    private void translateI32Store8() throws IOException {
        translateStore(SpecializedStoreOp.Op.I32_AS_I8);
    }

    private void translateI32Store16() throws IOException {
        translateStore(SpecializedStoreOp.Op.I32_AS_I16);
    }

    private void translateI64Store8() throws IOException {
        translateStore(SpecializedStoreOp.Op.I64_AS_I8);
    }

    private void translateI64Store16() throws IOException {
        translateStore(SpecializedStoreOp.Op.I64_AS_I16);
    }

    private void translateI64Store32() throws IOException {
        translateStore(SpecializedStoreOp.Op.I64_AS_I32);
    }

    private void translateMemorySize() throws IOException {
        reader.nextByte();
        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Module", "memorySize", "(Lorg/wastastic/Module;)I", false);
        operandStack.add(ValueType.I32);
    }

    private void translateMemoryGrow() throws IOException {
        reader.nextByte();
        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Module", "memoryGrow", "(ILorg/wastastic/Module;)I", false);
        operandStack.add(ValueType.I32);
    }

    private void emitLoadModule() {
        functionWriter.visitVarInsn(ALOAD, moduleArgumentLocalIndex);
    }

    private void translateI32Const() throws IOException {
        var value = reader.nextSigned32();

        switch (value) {
            case -1 -> functionWriter.visitInsn(ICONST_M1);
            case 0 -> functionWriter.visitInsn(ICONST_0);
            case 1 -> functionWriter.visitInsn(ICONST_1);
            case 2 -> functionWriter.visitInsn(ICONST_2);
            case 3 -> functionWriter.visitInsn(ICONST_3);
            case 4 -> functionWriter.visitInsn(ICONST_4);
            case 5 -> functionWriter.visitInsn(ICONST_5);
            default -> {
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    functionWriter.visitIntInsn(BIPUSH, value);
                }
                else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    functionWriter.visitIntInsn(SIPUSH, value);
                }
                else {
                    functionWriter.visitLdcInsn(value);
                }
            }
        }
    }

    private void translateI64Const() throws IOException {
        var value = reader.nextSigned64();

        if (value == 0) {
            functionWriter.visitInsn(LCONST_0);
        }
        else if (value == 1) {
            functionWriter.visitInsn(LCONST_1);
        }
        else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            functionWriter.visitIntInsn(BIPUSH, (int) value);
            functionWriter.visitInsn(I2L);
        }
        else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            functionWriter.visitIntInsn(SIPUSH, (int) value);
            functionWriter.visitInsn(I2L);
        }
        else {
            functionWriter.visitLdcInsn(value);
        }
    }

    private void translateF32Const() throws IOException {
        var value = reader.nextFloat32();

        if (value == 0) {
            functionWriter.visitInsn(FCONST_0);
        }
        else if (value == 1) {
            functionWriter.visitInsn(FCONST_1);
        }
        else if (value == 2) {
            functionWriter.visitInsn(FCONST_2);
        }
        else {
            functionWriter.visitLdcInsn(value);
        }
    }

    private void translateF64Const() throws IOException {
        var value = reader.nextFloat64();

        if (value == 0) {
            functionWriter.visitInsn(DCONST_0);
        }
        else if (value == 1) {
            functionWriter.visitInsn(DCONST_1);
        }
        else {
            functionWriter.visitLdcInsn(value);
        }
    }

    private void translateI32Eqz() {
        emitHelperCall("eqz", "(I)Z");
    }

    private void translateI32Comparison(@NotNull String name) {
        emitHelperCall(name, "(II)Z");
        popOperandType();
    }

    private void translateI32Eq() {
        translateI32Comparison("eq");
    }

    private void translateI32Ne() {
        translateI32Comparison("ne");
    }

    private void translateI32LtS() {
        translateI32Comparison("lts");
    }

    private void translateI32LtU() {
        translateI32Comparison("ltu");
    }

    private void translateI32GtS() {
        translateI32Comparison("gts");
    }

    private void translateI32GtU() {
        translateI32Comparison("gtu");
    }

    private void translateI32LeS() {
        translateI32Comparison("les");
    }

    private void translateI32LeU() {
        translateI32Comparison("leu");
    }

    private void translateI32GeS() {
        translateI32Comparison("ges");
    }

    private void translateI32GeU() {
        translateI32Comparison("geu");
    }

    private void translateI64Eqz() {
        emitHelperCall("eqz", "(J)Z");
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI64Comparison(@NotNull String name) {
        emitHelperCall(name, "(JJ)Z");
        popOperandType();
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI64Eq() {
        translateI64Comparison("eq");
    }

    private void translateI64Ne() {
        translateI64Comparison("ne");
    }

    private void translateI64LtS() {
        translateI64Comparison("lts");
    }

    private void translateI64LtU() {
        translateI64Comparison("ltu");
    }

    private void translateI64GtS() {
        translateI64Comparison("gts");
    }

    private void translateI64GtU() {
        translateI64Comparison("gtu");
    }

    private void translateI64LeS() {
        translateI64Comparison("les");
    }

    private void translateI64LeU() {
        translateI64Comparison("leu");
    }

    private void translateI64GeS() {
        translateI64Comparison("ges");
    }

    private void translateI64GeU() {
        translateI64Comparison("geu");
    }

    private void translateF32Comparison(@NotNull String name) {
        emitHelperCall(name, "(FF)Z");
        popOperandType();
        replaceTopOperandType(ValueType.I32);
    }

    private void translateF32Eq() {
        translateF32Comparison("feq");
    }

    private void translateF32Ne() {
        translateF32Comparison("fne");
    }

    private void translateF32Lt() {
        translateF32Comparison("flt");
    }

    private void translateF32Gt() {
        translateF32Comparison("fgt");
    }

    private void translateF32Le() {
        translateF32Comparison("fle");
    }

    private void translateF32Ge() {
        translateF32Comparison("fge");
    }

    private void translateF64Comparison(@NotNull String name) {
        emitHelperCall(name, "(DD)Z");
        popOperandType();
        replaceTopOperandType(ValueType.I32);
    }

    private void translateF64Eq() {
        translateF64Comparison("feq");
    }

    private void translateF64Ne() {
        translateF64Comparison("fne");
    }

    private void translateF64Lt() {
        translateF64Comparison("flt");
    }

    private void translateF64Gt() {
        translateF64Comparison("fgt");
    }

    private void translateF64Le() {
        translateF64Comparison("fle");
    }

    private void translateF64Ge() {
        translateF64Comparison("fge");
    }

    private void translateI32Clz() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "numberOfLeadingZeros", "(I)I", false);
    }

    private void translateI32Ctz() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "numberOfTrailingZeros", "(I)I", false);
    }

    private void translateI32Popcnt() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "bitCount", "(I)I", false);
    }

    private void translateI32Add() {
        functionWriter.visitInsn(IADD);
        popOperandType();
    }

    private void translateI32Sub() {
        functionWriter.visitInsn(ISUB);
        popOperandType();
    }

    private void translateI32Mul() {
        functionWriter.visitInsn(IMUL);
        popOperandType();
    }

    private void translateI32DivS() {
        emitHelperCall("divS", "(II)I");
        popOperandType();
    }

    private void translateI32DivU() {
        emitHelperCall("divU", "(II)I");
        popOperandType();
    }

    private void translateI32RemS() {
        emitHelperCall("remS", "(II)I");
        popOperandType();
    }

    private void translateI32RemU() {
        emitHelperCall("remU", "(II)I");
        popOperandType();
    }

    private void translateI32And() {
        functionWriter.visitInsn(IAND);
        popOperandType();
    }

    private void translateI32Or() {
        functionWriter.visitInsn(IOR);
        popOperandType();
    }

    private void translateI32Xor() {
        functionWriter.visitInsn(IXOR);
        popOperandType();
    }

    private void translateI32Shl() {
        functionWriter.visitInsn(ISHL);
        popOperandType();
    }

    private void translateI32ShrS() {
        functionWriter.visitInsn(ISHR);
        popOperandType();
    }

    private void translateI32ShrU() {
        functionWriter.visitInsn(IUSHR);
        popOperandType();
    }

    private void translateI32Rotl() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "rotateLeft", "(II)I", false);
        popOperandType();
    }

    private void translateI32Rotr() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "rotateRight", "(II)I", false);
        popOperandType();
    }

    private void translateI64Clz() {
        emitHelperCall("clz", "(J)J");
    }

    private void translateI64Ctz() {
        emitHelperCall("ctz", "(J)J");
    }

    private void translateI64Popcnt() {
        emitHelperCall("popcnt", "(J)J");
    }

    private void translateI64Add() {
        functionWriter.visitInsn(LADD);
        popOperandType();
    }

    private void translateI64Sub() {
        functionWriter.visitInsn(LSUB);
        popOperandType();
    }

    private void translateI64Mul() {
        functionWriter.visitInsn(LMUL);
        popOperandType();
    }

    private void translateI64DivS() {
        emitHelperCall("divS", "(JJ)J");
        popOperandType();
    }

    private void translateI64DivU() {
        emitHelperCall("divU", "(JJ)J");
        popOperandType();
    }

    private void translateI64RemS() {
        emitHelperCall("remS", "(JJ)J");
        popOperandType();
    }

    private void translateI64RemU() {
        emitHelperCall("remU", "(JJ)J");
        popOperandType();
    }

    private void translateI64And() {
        functionWriter.visitInsn(LAND);
        popOperandType();
    }

    private void translateI64Or() {
        functionWriter.visitInsn(LOR);
        popOperandType();
    }

    private void translateI64Xor() {
        functionWriter.visitInsn(LXOR);
        popOperandType();
    }

    private void translateI64Shl() {
        functionWriter.visitInsn(LSHL);
        popOperandType();
    }

    private void translateI64ShrS() {
        functionWriter.visitInsn(LSHR);
        popOperandType();
    }

    private void translateI64ShrU() {
        functionWriter.visitInsn(LUSHR);
        popOperandType();
    }

    private void translateI64Rotl() {
        emitHelperCall("rotl", "(JJ)J");
        popOperandType();
    }

    private void translateI64Rotr() {
        emitHelperCall("rotr", "(JJ)J");
        popOperandType();
    }

    private void translateF32Abs() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(F)F", false);
    }

    private void translateF32Neg() {
        functionWriter.visitInsn(FNEG);
    }

    private void translateF32Ceil() {
        emitHelperCall("fceil", "(F)F");
    }

    private void translateF32Floor() {
        emitHelperCall("ffloor", "(F)F");
    }

    private void translateF32Trunc() {
        emitHelperCall("ftrunc", "(F)F");
    }

    private void translateF32Nearest() {
        emitHelperCall("fnearest", "(F)F");
    }

    private void translateF32Sqrt() {
        emitHelperCall("fsqrt", "(F)F");
    }

    private void translateF32Add() {
        functionWriter.visitInsn(FADD);
        popOperandType();
    }

    private void translateF32Sub() {
        functionWriter.visitInsn(FSUB);
        popOperandType();
    }

    private void translateF32Mul() {
        functionWriter.visitInsn(FMUL);
        popOperandType();
    }

    private void translateF32Div() {
        functionWriter.visitInsn(FDIV);
        popOperandType();
    }

    private void translateF32Min() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "min", "(FF)F", false);
        popOperandType();
    }

    private void translateF32Max() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "max", "(FF)F", false);
        popOperandType();
    }

    private void translateF32Copysign() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "copySign", "(FF)F", false);
        popOperandType();
    }

    private void translateF64Abs() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
    }

    private void translateF64Neg() {
        functionWriter.visitInsn(DNEG);
    }

    private void translateF64Ceil() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "ceil", "(D)D", false);
    }

    private void translateF64Floor() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floor", "(D)D", false);
    }

    private void translateF64Trunc() {
        emitHelperCall("ftrunc", "(D)D");
    }

    private void translateF64Nearest() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "rint", "(D)D", false);
    }

    private void translateF64Sqrt() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sqrt", "(D)D", false);
    }

    private void translateF64Add() {
        functionWriter.visitInsn(DADD);
        popOperandType();
    }

    private void translateF64Sub() {
        functionWriter.visitInsn(DSUB);
        popOperandType();
    }

    private void translateF64Mul() {
        functionWriter.visitInsn(DMUL);
        popOperandType();
    }

    private void translateF64Div() {
        functionWriter.visitInsn(DDIV);
        popOperandType();
    }

    private void translateF64Min() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "min", "(DD)D", false);
        popOperandType();
    }

    private void translateF64Max() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "max", "(DD)D", false);
        popOperandType();
    }

    private void translateF64Copysign() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "copySign", "(DD)D", false);
        popOperandType();
    }

    private void translateI32WrapI64() {
        functionWriter.visitInsn(L2I);
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI32TruncF32S() {
        emitHelperCall("i32TruncS", "(F)I");
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI32TruncF32U() {
        emitHelperCall("i32TruncU", "(F)I");
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI32TruncF64S() {
        emitHelperCall("i32TruncS", "(D)I");
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI32TruncF64U() {
        emitHelperCall("i32TruncU", "(D)I");
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI64ExtendI32S() {
        functionWriter.visitInsn(I2L);
        replaceTopOperandType(ValueType.I64);
    }

    private void translateI64ExtendI32U() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toUnsignedLong", "(I)J", false);
        replaceTopOperandType(ValueType.I64);
    }

    private void translateI64TruncF32S() {
        emitHelperCall("i64TruncS", "(F)I");
        replaceTopOperandType(ValueType.I64);
    }

    private void translateI64TruncF32U() {
        emitHelperCall("i64TruncU", "(F)I");
        replaceTopOperandType(ValueType.I64);
    }

    private void translateI64TruncF64S() {
        emitHelperCall("i64TruncS", "(D)I");
        replaceTopOperandType(ValueType.I64);
    }

    private void translateI64TruncF64U() {
        emitHelperCall("i64TruncU", "(D)I");
        replaceTopOperandType(ValueType.I64);
    }

    private void translateF32ConvertI32S() {
        functionWriter.visitInsn(I2F);
        replaceTopOperandType(ValueType.F32);
    }

    private void translateF32ConvertI32U() {
        emitHelperCall("f32ConvertU", "(I)F");
        replaceTopOperandType(ValueType.F32);
    }

    private void translateF32ConvertI64S() {
        functionWriter.visitInsn(L2F);
        replaceTopOperandType(ValueType.F32);
    }

    private void translateF32ConvertI64U() {
        emitHelperCall("f32ConvertU", "(J)F");
        replaceTopOperandType(ValueType.F32);
    }

    private void translateF32DemoteF64() {
        functionWriter.visitInsn(D2F);
        replaceTopOperandType(ValueType.F32);
    }

    private void translateF64ConvertI32S() {
        functionWriter.visitInsn(I2D);
        replaceTopOperandType(ValueType.F64);
    }

    private void translateF64ConvertI32U() {
        emitHelperCall("f64ConvertU", "(I)D");
        replaceTopOperandType(ValueType.F64);
    }

    private void translateF64ConvertI64S() {
        functionWriter.visitInsn(L2D);
        replaceTopOperandType(ValueType.F64);
    }

    private void translateF64ConvertI64U() {
        emitHelperCall("f64ConvertU", "(J)D");
        replaceTopOperandType(ValueType.F64);
    }

    private void translateF64PromoteF32() {
        functionWriter.visitInsn(F2D);
        replaceTopOperandType(ValueType.F64);
    }

    private void translateI32ReinterpretF32() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "floatToRawIntBits", "(F)I", false);
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI64ReinterpretF64() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "doubleToRawLongBits", "(D)J", false);
        replaceTopOperandType(ValueType.I64);
    }

    private void translateF32ReinterpretI32() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false);
        replaceTopOperandType(ValueType.F32);
    }

    private void translateF64ReinterpretI64() {
        functionWriter.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false);
        replaceTopOperandType(ValueType.F64);
    }

    private void translateI32Extend8S() {
        functionWriter.visitInsn(I2B);
    }

    private void translateI32Extend16S() {
        functionWriter.visitInsn(I2S);
    }

    private void translateI64Extend8S() {
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2B);
        functionWriter.visitInsn(I2L);
    }

    private void translateI64Extend16S() {
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2S);
        functionWriter.visitInsn(L2I);
    }

    private void translateI64Extend32S() {
        functionWriter.visitInsn(L2I);
        functionWriter.visitInsn(I2L);
    }

    private void translateRefNull() throws TranslationException, IOException {
        functionWriter.visitInsn(ACONST_NULL);
        operandStack.add(nextReferenceType().toValueType());
    }

    private void translateRefIsNull() {
        emitHelperCall("refIsNull", "(Ljava/lang/Object;)Z");
        popOperandType();
    }

    private void translateRefFunc() throws IOException {
        var index = reader.nextUnsigned32();
        var name = "f-" + index;

        if (index < importedFunctions.size()) {
            emitLoadModule();
            functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, name + "-mh", METHOD_HANDLE_DESCRIPTOR);
        }
        else {
            var type = definedFunctions.get(index - importedFunctions.size());
            var handle = new Handle(H_INVOKESPECIAL, GENERATED_INSTANCE_INTERNAL_NAME, name, type.descriptor(), false);
            functionWriter.visitLdcInsn(handle);
        }

        operandStack.add(ValueType.FUNCREF);
    }

    private void translateCont() throws IOException, TranslationException {
        switch (reader.nextByte()) {
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

    private void translateI32TruncSatF32S() {
        functionWriter.visitInsn(F2I);
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI32TruncSatF32U() {
        emitHelperCall("i32TruncSatU", "(F)I");
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI32TruncSatF64S() {
        functionWriter.visitInsn(D2I);
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI32TruncSatF64U() {
        emitHelperCall("i32TruncSatU", "(D)I");
        replaceTopOperandType(ValueType.I32);
    }

    private void translateI64TruncSatF32S() {
        functionWriter.visitInsn(F2L);
        replaceTopOperandType(ValueType.I64);
    }

    private void translateI64TruncSatF32U() {
        emitHelperCall("i64TruncSatU", "(F)J");
        replaceTopOperandType(ValueType.I64);
    }

    private void translateI64TruncSatF64S() {
        functionWriter.visitInsn(D2L);
        replaceTopOperandType(ValueType.I64);
    }

    private void translateI64TruncSatF64U() {
        emitHelperCall("i64TruncSatU", "(D)J");
        replaceTopOperandType(ValueType.I64);
    }

    private void translateMemoryInit() throws IOException {
        var dataIndex = reader.nextUnsigned32();
        reader.nextByte();
        emitLoadModule();
        functionWriter.visitInsn(DUP);
        functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, "d-" + dataIndex, "Ljdk/incubator/foreign/MemorySegment;");
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Module", "memoryInit", "(IIILorg/wastastic/Module;Ljdk/incubator/foreign/MemorySegment;)V", false);
        popOperandType();
        popOperandType();
        popOperandType();
    }

    private void translateDataDrop() throws IOException {
        var index = reader.nextUnsigned32();
        emitLoadModule();
        functionWriter.visitFieldInsn(GETSTATIC, "org/wastastic/Module", "EMPTY_DATA", "Ljdk/incubator/foreign/MemorySegment;");
        functionWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, "d-" + index, "Ljdk/incubator/foreign/MemorySegment;");
    }

    private void translateMemoryCopy() throws IOException {
        reader.nextByte();
        reader.nextByte();
        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Module", "memoryCopy", "(IIILorg/wastastic/Module;)V", false);
        popOperandType();
        popOperandType();
        popOperandType();
    }

    private void translateMemoryFill() throws IOException {
        reader.nextByte();
        emitLoadModule();
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Module", "memoryFill", "(IBILorg/wastastic/Module;)V", false);
        popOperandType();
        popOperandType();
        popOperandType();
    }

    private void translateTableInit() throws IOException {
        var elemIndex = reader.nextUnsigned32();
        var tableIndex = reader.nextUnsigned32();
        emitLoadModule();
        functionWriter.visitInsn(DUP);
        functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, "e-" + elemIndex, "[Ljava/lang/Object;");
        functionWriter.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, "t-" + tableIndex, "Lorg/wastastic/Table;");
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Table", "init", "(III[Ljava/lang/Object;Lorg/wastastic/Table;)V", false);
        popOperandType();
        popOperandType();
        popOperandType();
    }

    private void translateElemDrop() throws IOException {
        var index = reader.nextUnsigned32();
        emitLoadModule();
        functionWriter.visitFieldInsn(GETSTATIC, "org/wastastic/Module", "EMPTY_ELEMENT", "[Ljava/lang/Object;");
        functionWriter.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, "e-" + index, "[Ljava/lang/Object;");
    }

    private void translateTableCopy() throws IOException {
        emitLoadTable(reader.nextUnsigned32());
        emitLoadTable(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Table", "copy", "(IIILorg/wastastic/Table;Lorg/wastastic/Table;)V", false);
        popOperandType();
        popOperandType();
        popOperandType();
    }

    private void translateTableGrow() throws IOException {
        emitLoadTable(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Table", "grow", "(Ljava/lang/Object;ILorg/wastastic/Table;)I", false);
        popOperandType();
        replaceTopOperandType(ValueType.I32);
    }

    private void translateTableSize() throws IOException {
        emitLoadTable(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Table", "size", "(Lorg/wastastic/Table;)I", false);
        operandStack.add(ValueType.I32);
    }

    private void translateTableFill() throws IOException {
        emitLoadTable(reader.nextUnsigned32());
        functionWriter.visitMethodInsn(INVOKESTATIC, "org/wastastic/Table", "fill", "(ILjava/lang/Object;ILorg/wastastic/Table;)V", false);
        popOperandType();
        popOperandType();
        popOperandType();
    }

    private @NotNull Local nextIndexedLocal() throws IOException {
        return locals.get(reader.nextUnsigned32());
    }

    private @NotNull LabelScope nextBranchTarget() throws IOException {
        return labelStack.get(labelStack.size() - 1 - reader.nextUnsigned32());
    }

    private void emitBranch(LabelScope target) {
        var nextLocalIndex = firstScratchLocalIndex;

        for (var i = target.parameterTypes().size() - 1; i >= 0; i--) {
            var parameterType = target.parameterTypes().get(i);
            functionWriter.visitVarInsn(parameterType.localStoreOpcode(), nextLocalIndex);
            nextLocalIndex += parameterType.width();
        }

        for (var i = operandStack.size() - target.parameterTypes().size() - 1; i >= target.operandStackSize(); i--) {
            if (operandStack.get(i).isDoubleWidth()) {
                functionWriter.visitInsn(POP2);
            }
            else {
                functionWriter.visitInsn(POP);
            }
        }

        for (var parameterType : target.parameterTypes()) {
            nextLocalIndex -= parameterType.width();
            functionWriter.visitVarInsn(parameterType.localLoadOpcode(), nextLocalIndex);
        }

        functionWriter.visitJumpInsn(GOTO, target.targetLabel());
    }

    private @NotNull FunctionType nextBlockType() throws IOException, TranslationException {
        var code = reader.nextSigned33();
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

    private void emitHelperCall(String name, String descriptor) {
        functionWriter.visitMethodInsn(
            INVOKESTATIC,
            "org/wastastic/runtime/InstructionHelpers",
            name,
            descriptor,
            false
        );
    }

    private @NotNull FunctionType nextIndexedType() throws IOException {
        return types.get(reader.nextUnsigned32());
    }

    private @NotNull MemoryType nextMemoryType() throws TranslationException, IOException {
        return new MemoryType(nextLimits());
    }

    private @NotNull TableType nextTableType() throws TranslationException, IOException {
        return new TableType(nextReferenceType(), nextLimits());
    }

    private @NotNull ReferenceType nextReferenceType() throws TranslationException, IOException {
        return switch (reader.nextByte()) {
            case TYPE_EXTERNREF -> ReferenceType.EXTERNREF;
            case TYPE_FUNCREF -> ReferenceType.FUNCREF;
            default -> throw new TranslationException("Invalid reference type");
        };
    }

    private @NotNull Limits nextLimits() throws TranslationException, IOException {
        return switch (reader.nextByte()) {
            case 0x00 -> new Limits(reader.nextUnsigned32());
            case 0x01 -> new Limits(reader.nextUnsigned32(), reader.nextUnsigned32());
            default -> throw new TranslationException("Invalid limits encoding");
        };
    }

    private @NotNull String nextName() throws IOException {
        return reader.nextUtf8(reader.nextUnsigned32());
    }

    private @NotNull List<ValueType> nextResultType() throws TranslationException, IOException {
        var unsignedSize = reader.nextUnsigned32();
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

    private @NotNull ValueType nextValueType() throws TranslationException, IOException {
        return switch (reader.nextByte()) {
            case TYPE_EXTERNREF -> ValueType.EXTERNREF;
            case TYPE_FUNCREF -> ValueType.FUNCREF;
            case TYPE_F64 -> ValueType.F64;
            case TYPE_F32 -> ValueType.F32;
            case TYPE_I64 -> ValueType.I64;
            case TYPE_I32 -> ValueType.I32;
            default -> throw new TranslationException("Invalid value type");
        };
    }

    private @NotNull ValueType popOperandType() {
        return operandStack.remove(operandStack.size() - 1);
    }

    private void replaceTopOperandType(@NotNull ValueType replacement) {
        operandStack.set(operandStack.size() - 1, replacement);
    }

    private @NotNull LabelScope popLabelScope() {
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
