package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;
import static org.objectweb.asm.Type.getInternalName;
import static org.objectweb.asm.Type.getMethodType;
import static org.wastastic.CodegenUtils.pushF32Constant;
import static org.wastastic.CodegenUtils.pushF64Constant;
import static org.wastastic.CodegenUtils.pushI32Constant;
import static org.wastastic.CodegenUtils.pushI64Constant;
import static org.wastastic.Importers.IMPORT_FUNCTION_DESCRIPTOR;
import static org.wastastic.Importers.IMPORT_FUNCTION_NAME;
import static org.wastastic.Importers.IMPORT_MEMORY_DESCRIPTOR;
import static org.wastastic.Importers.IMPORT_MEMORY_NAME;
import static org.wastastic.Importers.IMPORT_TABLE_DESCRIPTOR;
import static org.wastastic.Importers.IMPORT_TABLE_NAME;
import static org.wastastic.Names.FUNCTION_CLASS_ENTRY_NAME;
import static org.wastastic.Names.GENERATED_INSTANCE_CONSTRUCTOR_DESCRIPTOR;
import static org.wastastic.Names.GENERATED_INSTANCE_INTERNAL_NAME;
import static org.wastastic.Names.MEMORY_SEGMENT_DESCRIPTOR;
import static org.wastastic.Names.METHOD_HANDLE_DESCRIPTOR;
import static org.wastastic.Names.METHOD_HANDLE_INTERNAL_NAME;
import static org.wastastic.Names.MODULE_INSTANCE_INTERNAL_NAME;
import static org.wastastic.Names.OBJECT_ARRAY_DESCRIPTOR;
import static org.wastastic.Names.OBJECT_INTERNAL_NAME;
import static org.wastastic.Names.functionClassInternalName;
import static org.wastastic.Names.methodDescriptor;

final class ModuleClassLoader extends ClassLoader implements Module {
    private final @NotNull ParsedModule parsedModule;
    private final @NotNull Map<String, Integer> functionIndices = new HashMap<>();
    private final @NotNull Map<String, ExportedFunction> exportedFunctions = new HashMap<>();
    private final @NotNull Map<String, String> exportedTableNames = new HashMap<>();
    private final @NotNull Map<String, String> exportedMemoryNames = new HashMap<>();

    ModuleClassLoader(@NotNull ParsedModule parsedModule) {
        this.parsedModule = requireNonNull(parsedModule);

        for (var i = 0; i < parsedModule.functionNames().size(); i++) {
            functionIndices.put(functionClassInternalName(parsedModule.functionNames().get(i)).replace('/', '.'), i);
        }

        for (var export : parsedModule.exports()) {
            switch (export.kind()) {
                case FUNCTION -> exportedFunctions.put(export.name(), new ExportedFunction(parsedModule.functionNames().get(export.index()), parsedModule.functionType(export.index())));
                case TABLE -> exportedTableNames.put(export.name(), parsedModule.tableNames().get(export.index()));
                case MEMORY -> exportedMemoryNames.put(export.name(), parsedModule.memoryNames().get(export.index()));
            }
        }
    }

    @Override public @NotNull MethodHandle instantiationHandle() {
        try {
            var instanceClass = loadClass(GENERATED_INSTANCE_INTERNAL_NAME.replace('/', '.'));
            return MethodHandles.lookup().findConstructor(instanceClass, methodType(void.class, Map.class));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull MethodHandle exportedFunctionHandle(@NotNull String name) {
        var function = exportedFunctions.get(name);

        if (function == null) {
            throw new IllegalArgumentException();
        }

        try {
            var instanceClass = loadClass(GENERATED_INSTANCE_INTERNAL_NAME);
            var functionClass = loadClass(functionClassInternalName(function.name()));
            return MethodHandles.lookup().findStatic(functionClass, FUNCTION_CLASS_ENTRY_NAME, function.type().methodType(instanceClass));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull VarHandle exportedTableHandle(@NotNull String name) {
        var fieldName = exportedTableNames.get(name);

        if (fieldName == null) {
            throw new IllegalArgumentException();
        }

        try {
            var instanceClass = loadClass(GENERATED_INSTANCE_INTERNAL_NAME);
            return MethodHandles.lookup().findVarHandle(instanceClass, fieldName, Table.class);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull VarHandle exportedMemoryHandle(@NotNull String name) {
        var fieldName = exportedMemoryNames.get(name);

        if (fieldName == null) {
            throw new IllegalArgumentException();
        }

        try {
            var instanceClass = loadClass(GENERATED_INSTANCE_INTERNAL_NAME);
            return MethodHandles.lookup().findVarHandle(instanceClass, fieldName, Memory.class);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        requireNonNull(name);

        if (name.equals(GENERATED_INSTANCE_INTERNAL_NAME.replace('/', '.'))) {
            return defineModuleInstance(name);
        }

        var functionIndex = functionIndices.get(name);

        if (functionIndex != null) {
            try {
                return defineFunction(functionIndex, name);
            }
            catch (TranslationException exception) {
                throw new ClassNotFoundException("Translation failure", exception);
            }
        }

        throw new ClassNotFoundException();
    }

    private @NotNull Class<?> defineModuleInstance(@NotNull String className) {
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(V17, ACC_FINAL | ACC_PUBLIC, GENERATED_INSTANCE_INTERNAL_NAME, null, OBJECT_INTERNAL_NAME, new String[]{MODULE_INSTANCE_INTERNAL_NAME});

        for (var i = 0; i < parsedModule.importedFunctions().size(); i++) {
            writer.visitField(ACC_FINAL, parsedModule.functionNames().get(i), METHOD_HANDLE_DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.importedMemories().size(); i++) {
            writer.visitField(ACC_FINAL, parsedModule.memoryNames().get(i), Memory.DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.importedTables().size(); i++) {
            writer.visitField(ACC_FINAL, parsedModule.tableNames().get(i), Table.DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.definedMemories().size(); i++) {
            writer.visitField(ACC_FINAL, parsedModule.memoryNames().get(parsedModule.importedMemories().size() + i), Memory.DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.definedTables().size(); i++) {
            writer.visitField(ACC_FINAL, parsedModule.tableNames().get(parsedModule.importedTables().size() + i), Table.DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.dataSegments().size(); i++) {
            writer.visitField(0, parsedModule.dataSegmentNames().get(i), MEMORY_SEGMENT_DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.elementSegments().size(); i++) {
            writer.visitField(0, parsedModule.elementSegmentNames().get(i), OBJECT_ARRAY_DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.definedGlobals().size(); i++) {
            var access = 0;

            if (parsedModule.definedGlobals().get(i).type().mutability() == Mutability.CONST) {
                access |= ACC_FINAL;
            }

            writer.visitField(access, parsedModule.globalNames().get(parsedModule.importedGlobals().size() + i), parsedModule.definedGlobals().get(i).type().valueType().descriptor(), null, null);
        }

        var constructor = writer.visitMethod(ACC_PRIVATE, "<init>", GENERATED_INSTANCE_CONSTRUCTOR_DESCRIPTOR, null, null);
        constructor.visitParameter("imports", ACC_FINAL);
        constructor.visitCode();

        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, OBJECT_INTERNAL_NAME, "<init>", "()V", false);

        for (var i = 0; i < parsedModule.importedFunctions().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitVarInsn(ALOAD, 1);
            constructor.visitLdcInsn(parsedModule.importedFunctions().get(i).qualifiedName().moduleName());
            constructor.visitLdcInsn(parsedModule.importedFunctions().get(i).qualifiedName().name());
            constructor.visitLdcInsn(getMethodType(parsedModule.importedFunctions().get(i).type().descriptor()));
            constructor.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_FUNCTION_NAME, IMPORT_FUNCTION_DESCRIPTOR, false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.functionNames().get(i), METHOD_HANDLE_DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.importedMemories().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitVarInsn(ALOAD, 1);
            constructor.visitLdcInsn(parsedModule.importedMemories().get(i).name().moduleName());
            constructor.visitLdcInsn(parsedModule.importedMemories().get(i).name().name());
            constructor.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_MEMORY_NAME, IMPORT_MEMORY_DESCRIPTOR, false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.memoryNames().get(i), Memory.DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.importedTables().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitVarInsn(ALOAD, 1);
            constructor.visitLdcInsn(parsedModule.importedTables().get(i).name().moduleName());
            constructor.visitLdcInsn(parsedModule.importedTables().get(i).name().name());
            constructor.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_TABLE_NAME, IMPORT_TABLE_DESCRIPTOR, false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.tableNames().get(i), Table.DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.definedGlobals().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);

            var initialValue = parsedModule.definedGlobals().get(i).initialValue();

            if (initialValue instanceof NullConstant) {
                constructor.visitInsn(ACONST_NULL);
            }
            else if (initialValue instanceof I32Constant i32Constant) {
                pushI32Constant(constructor, i32Constant.value());
            }
            else if (initialValue instanceof I64Constant i64Constant) {
                pushI64Constant(constructor, i64Constant.value());
            }
            else if (initialValue instanceof F32Constant f32Constant) {
                pushF32Constant(constructor, f32Constant.value());
            }
            else if (initialValue instanceof F64Constant f64Constant) {
                pushF64Constant(constructor, f64Constant.value());
            }
            else if (initialValue instanceof FunctionRefConstant functionRefConstant) {
                constructor.visitLdcInsn(new Handle(H_INVOKESTATIC, functionClassInternalName(parsedModule.functionNames().get(functionRefConstant.index())), FUNCTION_CLASS_ENTRY_NAME, parsedModule.functionType(functionRefConstant.index()).descriptor(), false));
            }
            else {
                throw new ClassCastException();
            }

            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.globalNames().get(parsedModule.importedGlobals().size() + i), parsedModule.definedGlobals().get(i).type().valueType().descriptor());
        }

        for (var i = 0; i < parsedModule.definedMemories().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitTypeInsn(NEW, Memory.INTERNAL_NAME);
            constructor.visitInsn(DUP);
            pushI32Constant(constructor, parsedModule.definedMemories().get(i).limits().unsignedMinimum());
            pushI32Constant(constructor, parsedModule.definedMemories().get(i).limits().unsignedMaximum());
            constructor.visitMethodInsn(INVOKESPECIAL, Memory.INTERNAL_NAME, "<init>", "(II)V", false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.memoryNames().get(parsedModule.importedMemories().size() + i), Memory.DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.definedTables().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitTypeInsn(NEW, Table.INTERNAL_NAME);
            constructor.visitInsn(DUP);
            pushI32Constant(constructor, parsedModule.definedTables().get(i).limits().unsignedMinimum());
            pushI32Constant(constructor, parsedModule.definedTables().get(i).limits().unsignedMaximum());
            constructor.visitMethodInsn(INVOKESPECIAL, Table.INTERNAL_NAME, "<init>", "(II)V", false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.tableNames().get(parsedModule.importedTables().size() + i), Table.DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.dataSegments().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitLdcInsn(new ConstantDynamic("data", MEMORY_SEGMENT_DESCRIPTOR, DATA_BOOTSTRAP, i));

            if (parsedModule.dataSegments().get(i).mode() == DataSegment.Mode.ACTIVE) {
                constructor.visitInsn(DUP);
                pushI32Constant(constructor, parsedModule.dataSegments().get(i).memoryOffset());
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.memoryNames().get(parsedModule.dataSegments().get(i).memoryIndex()), Memory.DESCRIPTOR);
                constructor.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.INIT_FROM_ACTIVE_NAME, Memory.INIT_FROM_ACTIVE_DESCRIPTOR, false);
            }

            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.dataSegmentNames().get(i), MEMORY_SEGMENT_DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.elementSegments().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitLdcInsn(new ConstantDynamic("element", OBJECT_ARRAY_DESCRIPTOR, ELEMENT_BOOTSTRAP, i));

            if (parsedModule.elementSegments().get(i).mode() == ElementSegment.Mode.ACTIVE) {
                constructor.visitInsn(DUP);
                pushI32Constant(constructor, parsedModule.elementSegments().get(i).tableOffset());
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.tableNames().get(parsedModule.elementSegments().get(i).tableIndex()), Table.DESCRIPTOR);
                constructor.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.INIT_FROM_ACTIVE_NAME, Table.INIT_FROM_ACTIVE_DESCRIPTOR, false);
            }

            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.elementSegmentNames().get(i), OBJECT_ARRAY_DESCRIPTOR);
        }

        if (parsedModule.startFunctionIndex().isPresent()) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitMethodInsn(INVOKESTATIC, functionClassInternalName(parsedModule.functionNames().get(parsedModule.startFunctionIndex().getAsInt())), FUNCTION_CLASS_ENTRY_NAME, parsedModule.functionType(parsedModule.startFunctionIndex().getAsInt()).descriptor(), false);
        }

        constructor.visitInsn(RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        writer.visitEnd();
        var bytes = writer.toByteArray();
        return defineClass(className, bytes, 0, bytes.length);
    }

    private @NotNull Class<?> defineFunction(int index, @NotNull String className) throws TranslationException {
        if (index < parsedModule.importedFunctions().size()) {
            return defineImportedFunctionWrapper(index, className);
        }

        var bytes = new FunctionTranslator().translate(parsedModule, className, index);
        return defineClass(null, bytes, 0, bytes.length);
    }

    private @NotNull Class<?> defineImportedFunctionWrapper(int index, @NotNull String className) {
        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(V17, ACC_FINAL, className, null, OBJECT_INTERNAL_NAME, null);

        var entry = writer.visitMethod(ACC_STATIC, FUNCTION_CLASS_ENTRY_NAME, parsedModule.importedFunctions().get(index).type().descriptor(), null, null);

        var instanceArgumentLocalIndex = 0;
        for (var type : parsedModule.importedFunctions().get(index).type().parameterTypes()) {
            instanceArgumentLocalIndex += type.width();
        }

        entry.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        entry.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, parsedModule.functionNames().get(index), METHOD_HANDLE_DESCRIPTOR);

        var argumentLocalIndex = 0;
        for (var type : parsedModule.importedFunctions().get(index).type().parameterTypes()) {
            entry.visitVarInsn(type.localLoadOpcode(), argumentLocalIndex);
            argumentLocalIndex += type.width();
        }

        entry.visitVarInsn(ALOAD, instanceArgumentLocalIndex);
        entry.visitMethodInsn(INVOKEVIRTUAL, METHOD_HANDLE_INTERNAL_NAME, "invokeExact", parsedModule.importedFunctions().get(index).type().descriptor(), false);
        entry.visitInsn(parsedModule.importedFunctions().get(index).type().returnOpcode());

        entry.visitMaxs(0, 0);
        entry.visitEnd();

        writer.visitEnd();
        var bytes = writer.toByteArray();
        return defineClass(null, bytes, 0, bytes.length);
    }

    private static final String INTERNAL_NAME = getInternalName(ModuleClassLoader.class);

    private static final Handle DATA_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "dataBootstrap", methodDescriptor(MemorySegment.class, MethodHandles.Lookup.class, String.class, Class.class, int.class), false);

    static @NotNull MemorySegment dataBootstrap(@NotNull MethodHandles.Lookup lookup, String name, Class<?> clazz, int index) throws IllegalAccessException {
        return ((ModuleClassLoader) clazz.getClassLoader()).parsedModule.dataSegments().get(index).contents();
    }

    private static final Handle ELEMENT_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "elementBootstrap", methodDescriptor(Object[].class, MethodHandles.Lookup.class, String.class, Class.class, int.class), false);

    static @NotNull Object[] elementBootstrap(@NotNull MethodHandles.Lookup lookup, String name, Class<?> clazz, int index) throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
        var loader = (ModuleClassLoader) clazz.getClassLoader();
        var constantValues = loader.parsedModule.elementSegments().get(index).values();
        var resolvedValues = new Object[constantValues.length];
        for (var i = 0; i < resolvedValues.length; i++) {
            if (constantValues[i] instanceof FunctionRefConstant functionRefConstant) {
                var functionClazz = loader.loadClass(loader.parsedModule.functionNames().get(functionRefConstant.index()));
                resolvedValues[i] = lookup.findStatic(functionClazz, FUNCTION_CLASS_ENTRY_NAME, loader.parsedModule.functionType(functionRefConstant.index()).methodType(clazz));
            }
            else if (constantValues[i] != NullConstant.INSTANCE) {
                throw new ClassCastException();
            }
        }
        return resolvedValues;
    }
}
