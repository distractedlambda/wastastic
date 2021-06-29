package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodHandles.exactInvoker;
import static java.lang.invoke.MethodHandles.filterArguments;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
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
import static org.wastastic.Names.MODULE_INSTANCE_INTERNAL_NAME;
import static org.wastastic.Names.OBJECT_ARRAY_DESCRIPTOR;
import static org.wastastic.Names.OBJECT_INTERNAL_NAME;
import static org.wastastic.Names.dataSegmentName;
import static org.wastastic.Names.elementSegmentName;
import static org.wastastic.Names.functionName;
import static org.wastastic.Names.globalName;
import static org.wastastic.Names.memoryName;
import static org.wastastic.Names.methodDescriptor;
import static org.wastastic.Names.tableName;

final class ModuleImpl implements Module {
    private final @NotNull ParsedModule parsedModule;

    private @Nullable MethodHandles.Lookup instanceLookup;
    private final @Nullable MethodHandle @NotNull[] functionHandles;

    private final @NotNull Map<String, ExportedFunction> exportedFunctions = new HashMap<>();
    private final @NotNull Map<String, String> exportedTableNames = new HashMap<>();
    private final @NotNull Map<String, String> exportedMemoryNames = new HashMap<>();

    ModuleImpl(@NotNull ParsedModule parsedModule) {
        this.parsedModule = requireNonNull(parsedModule);

        functionHandles = new MethodHandle[parsedModule.functionNames().size()];

        for (var export : parsedModule.exports()) {
            switch (export.kind()) {
                case FUNCTION -> exportedFunctions.put(export.name(), new ExportedFunction(parsedModule.functionNames().get(export.index()), parsedModule.functionType(export.index())));
                case TABLE -> exportedTableNames.put(export.name(), parsedModule.tableNames().get(export.index()));
                case MEMORY -> exportedMemoryNames.put(export.name(), parsedModule.memoryNames().get(export.index()));
            }
        }
    }

    @Override public @NotNull MethodHandle instantiationHandle() {
        // try {
        //     var instanceClass = loadClass(GENERATED_INSTANCE_INTERNAL_NAME.replace('/', '.'));
        //     return MethodHandles.lookup().findConstructor(instanceClass, methodType(void.class, Map.class));
        // } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
        //     throw new IllegalStateException(exception);
        // }
    }

    @Override public @NotNull MethodHandle exportedFunctionHandle(@NotNull String name) {
        // var function = exportedFunctions.get(name);

        // if (function == null) {
        //     throw new IllegalArgumentException();
        // }

        // try {
        //     var instanceClass = loadClass(GENERATED_INSTANCE_INTERNAL_NAME);
        //     var functionClass = loadClass(functionClassInternalName(function.name()));
        //     return MethodHandles.lookup().findStatic(functionClass, FUNCTION_CLASS_ENTRY_NAME, function.type().methodType(instanceClass));
        // } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException exception) {
        //     throw new IllegalStateException(exception);
        // }
    }

    @Override public @NotNull VarHandle exportedTableHandle(@NotNull String name) {
        // var fieldName = exportedTableNames.get(name);

        // if (fieldName == null) {
        //     throw new IllegalArgumentException();
        // }

        // try {
        //     var instanceClass = loadClass(GENERATED_INSTANCE_INTERNAL_NAME);
        //     return MethodHandles.lookup().findVarHandle(instanceClass, fieldName, Table.class);
        // } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
        //     throw new IllegalStateException(exception);
        // }
    }

    @Override public @NotNull VarHandle exportedMemoryHandle(@NotNull String name) {
        // var fieldName = exportedMemoryNames.get(name);

        // if (fieldName == null) {
        //     throw new IllegalArgumentException();
        // }

        // try {
        //     var instanceClass = loadClass(GENERATED_INSTANCE_INTERNAL_NAME);
        //     return MethodHandles.lookup().findVarHandle(instanceClass, fieldName, Memory.class);
        // } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException exception) {
        //     throw new IllegalStateException(exception);
        // }
    }

    private synchronized @NotNull MethodHandles.Lookup getInstanceLookup() throws IllegalAccessException {
        if (instanceLookup != null) {
            return instanceLookup;
        }

        var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        writer.visit(V17, ACC_FINAL, GENERATED_INSTANCE_INTERNAL_NAME, null, OBJECT_INTERNAL_NAME, new String[]{MODULE_INSTANCE_INTERNAL_NAME});

        for (var i = 0; i < parsedModule.importedFunctions().size(); i++) {
            writer.visitField(ACC_PRIVATE | ACC_FINAL, functionName(i), METHOD_HANDLE_DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.importedMemories().size() + parsedModule.definedMemories().size(); i++) {
            writer.visitField(ACC_PRIVATE | ACC_FINAL, memoryName(i), Memory.DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.importedTables().size() + parsedModule.definedTables().size(); i++) {
            writer.visitField(ACC_PRIVATE | ACC_FINAL, tableName(i), Table.DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.dataSegments().size(); i++) {
            writer.visitField(ACC_PRIVATE, dataSegmentName(i), MEMORY_SEGMENT_DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.elementSegments().size(); i++) {
            writer.visitField(ACC_PRIVATE, dataSegmentName(i), OBJECT_ARRAY_DESCRIPTOR, null, null);
        }

        for (var i = 0; i < parsedModule.definedGlobals().size(); i++) {
            var access = ACC_PRIVATE;

            if (parsedModule.definedGlobals().get(i).type().mutability() == Mutability.CONST) {
                access |= ACC_FINAL;
            }

            writer.visitField(access, globalName(parsedModule.importedTables().size() + i), parsedModule.definedGlobals().get(i).type().valueType().descriptor(), null, null);
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
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, functionName(i), METHOD_HANDLE_DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.importedMemories().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitVarInsn(ALOAD, 1);
            constructor.visitLdcInsn(parsedModule.importedMemories().get(i).name().moduleName());
            constructor.visitLdcInsn(parsedModule.importedMemories().get(i).name().name());
            constructor.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_MEMORY_NAME, IMPORT_MEMORY_DESCRIPTOR, false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryName(i), Memory.DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.importedTables().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitVarInsn(ALOAD, 1);
            constructor.visitLdcInsn(parsedModule.importedTables().get(i).name().moduleName());
            constructor.visitLdcInsn(parsedModule.importedTables().get(i).name().name());
            constructor.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_TABLE_NAME, IMPORT_TABLE_DESCRIPTOR, false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableName(i), Table.DESCRIPTOR);
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
                constructor.visitLdcInsn(new ConstantDynamic("", METHOD_HANDLE_DESCRIPTOR, FUNCTION_REF_BOOTSTRAP, functionRefConstant.index()));
            }
            else {
                throw new ClassCastException();
            }

            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, globalName(parsedModule.importedGlobals().size() + i), parsedModule.definedGlobals().get(i).type().valueType().descriptor());
        }

        for (var i = 0; i < parsedModule.definedMemories().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitTypeInsn(NEW, Memory.INTERNAL_NAME);
            constructor.visitInsn(DUP);
            pushI32Constant(constructor, parsedModule.definedMemories().get(i).limits().unsignedMinimum());
            pushI32Constant(constructor, parsedModule.definedMemories().get(i).limits().unsignedMaximum());
            constructor.visitMethodInsn(INVOKESPECIAL, Memory.INTERNAL_NAME, "<init>", "(II)V", false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryName(parsedModule.importedMemories().size() + i), Memory.DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.definedTables().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitTypeInsn(NEW, Table.INTERNAL_NAME);
            constructor.visitInsn(DUP);
            pushI32Constant(constructor, parsedModule.definedTables().get(i).limits().unsignedMinimum());
            pushI32Constant(constructor, parsedModule.definedTables().get(i).limits().unsignedMaximum());
            constructor.visitMethodInsn(INVOKESPECIAL, Table.INTERNAL_NAME, "<init>", "(II)V", false);
            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableName(parsedModule.importedTables().size() + i), Table.DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.dataSegments().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitLdcInsn(new ConstantDynamic("", MEMORY_SEGMENT_DESCRIPTOR, DATA_BOOTSTRAP, i));

            if (parsedModule.dataSegments().get(i).mode() == DataSegment.Mode.ACTIVE) {
                constructor.visitInsn(DUP);
                pushI32Constant(constructor, parsedModule.dataSegments().get(i).memoryOffset());
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryName(parsedModule.dataSegments().get(i).memoryIndex()), Memory.DESCRIPTOR);
                constructor.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.INIT_FROM_ACTIVE_NAME, Memory.INIT_FROM_ACTIVE_DESCRIPTOR, false);
            }

            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, dataSegmentName(i), MEMORY_SEGMENT_DESCRIPTOR);
        }

        for (var i = 0; i < parsedModule.elementSegments().size(); i++) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitLdcInsn(new ConstantDynamic("", OBJECT_ARRAY_DESCRIPTOR, ELEMENT_BOOTSTRAP, i));

            if (parsedModule.elementSegments().get(i).mode() == ElementSegment.Mode.ACTIVE) {
                constructor.visitInsn(DUP);
                pushI32Constant(constructor, parsedModule.elementSegments().get(i).tableOffset());
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableName(parsedModule.elementSegments().get(i).tableIndex()), Table.DESCRIPTOR);
                constructor.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.INIT_FROM_ACTIVE_NAME, Table.INIT_FROM_ACTIVE_DESCRIPTOR, false);
            }

            constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, elementSegmentName(i), OBJECT_ARRAY_DESCRIPTOR);
        }

        if (parsedModule.startFunctionIndex().isPresent()) {
            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitInvokeDynamicInsn("", parsedModule.functionType(parsedModule.startFunctionIndex().getAsInt()).descriptor(), FUNCTION_BOOTSTRAP, parsedModule.startFunctionIndex());
        }

        constructor.visitInsn(RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        writer.visitEnd();
        return instanceLookup = MethodHandles.lookup().defineHiddenClassWithClassData(writer.toByteArray(), this, false);
    }

    private synchronized @NotNull MethodHandle getFunctionHandle(int index) throws TranslationException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        if (functionHandles[index] != null) {
            return functionHandles[index];
        }

        if (index < parsedModule.importedFunctions().size()) {
            var lookup = getInstanceLookup();
            var importGetter = lookup.findGetter(lookup.lookupClass(), functionName(index), MethodHandle.class);
            var handle = exactInvoker(parsedModule.functionType(index).methodType());
            handle = filterArguments(handle, 0, importGetter);

            var permutationOrder = new int[parsedModule.functionType(index).parameterTypes().size() + 2];

            permutationOrder[0] = parsedModule.functionType(index).parameterTypes().size();

            for (var i = 0; i < parsedModule.functionType(index).parameterTypes().size(); i++) {
                permutationOrder[i + 1] = i;
            }

            permutationOrder[parsedModule.functionType(index).parameterTypes().size() + 1] = parsedModule.functionType(index).parameterTypes().size();

            handle = permuteArguments(handle, parsedModule.functionType(index).methodType(), permutationOrder);
            return functionHandles[index] = handle;
        }

        var bytes = new FunctionTranslator().translate(parsedModule, index);
        var lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, this, false);
        return functionHandles[index] = lookup.findStatic(lookup.lookupClass(), FUNCTION_CLASS_ENTRY_NAME, parsedModule.functionType(index).methodType());
    }

    private static final String INTERNAL_NAME = getInternalName(ModuleImpl.class);

    static final Handle FUNCTION_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "functionBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite functionBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int index) throws IllegalAccessException, TranslationException, NoSuchFieldException, NoSuchMethodException {
        var module = MethodHandles.classData(lookup, "_", ModuleImpl.class);
        return new ConstantCallSite(module.getFunctionHandle(index));
    }

    static final Handle GLOBAL_GET_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "globalGetBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite globalGetBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int index) throws IllegalAccessException, NoSuchFieldException {
        var module = MethodHandles.classData(lookup, "_", ModuleImpl.class);
        var instanceLookup = module.getInstanceLookup();
        var handle = instanceLookup.findGetter(instanceLookup.lookupClass(), globalName(index), module.parsedModule.globalType(index).valueType().jvmType());
        return new ConstantCallSite(handle);
    }

    static final Handle GLOBAL_SET_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "globalSetBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite globalSetBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int index) throws IllegalAccessException, NoSuchFieldException {
        var module = MethodHandles.classData(lookup, "_", ModuleImpl.class);
        var instanceLookup = module.getInstanceLookup();
        var handle = instanceLookup.findSetter(instanceLookup.lookupClass(), globalName(index), module.parsedModule.globalType(index).valueType().jvmType());
        handle = permuteArguments(handle, methodType(void.class, module.parsedModule.globalType(index).valueType().jvmType(), ModuleInstance.class), 1, 0);
        return new ConstantCallSite(handle);
    }

    static final Handle FUNCTION_REF_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "functionBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, Class.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull MethodHandle functionRefBootstrap(@NotNull MethodHandles.Lookup lookup, String name, Class<?> clazz, int index) throws IllegalAccessException, TranslationException, NoSuchFieldException, NoSuchMethodException {
        var module = MethodHandles.classData(lookup, "_", ModuleImpl.class);
        return module.getFunctionHandle(index);
    }

    private static final Handle DATA_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "dataBootstrap", methodDescriptor(MemorySegment.class, MethodHandles.Lookup.class, String.class, Class.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull MemorySegment dataBootstrap(@NotNull MethodHandles.Lookup lookup, String name, Class<?> clazz, int index) throws IllegalAccessException {
        return ((ModuleImpl) clazz.getClassLoader()).parsedModule.dataSegments().get(index).contents();
    }

    private static final Handle ELEMENT_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "elementBootstrap", methodDescriptor(Object[].class, MethodHandles.Lookup.class, String.class, Class.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull Object[] elementBootstrap(@NotNull MethodHandles.Lookup lookup, String name, Class<?> clazz, int index) throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
        var loader = (ModuleImpl) clazz.getClassLoader();
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
