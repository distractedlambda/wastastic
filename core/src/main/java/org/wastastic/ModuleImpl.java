package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
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
import java.util.Map;

import static java.lang.invoke.MethodHandles.classData;
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
    private final @NotNull ModuleIndex index;

    private @Nullable MethodHandles.Lookup instanceLookup;
    private final @Nullable MethodHandle @NotNull[] functionHandles;

    ModuleImpl(@NotNull ModuleIndex index) {
        this.index = requireNonNull(index);
        functionHandles = new MethodHandle[index.importedFunctions().size() + index.definedFunctions().size()];
    }

    @Override public @NotNull ResourceScope scope() {
        return index.scope();
    }

    @Override public void precompileFunctions() throws TranslationException {
        getOrCreateInstance();
        for (var i = 0; i < functionHandles.length; i++) {
            getOrCreateFunction(i);
        }
    }

    @Override public @NotNull MethodHandle instantiationHandle() throws TranslationException {
        try {
            var lookup = getOrCreateInstance();
            return lookup.findConstructor(lookup.lookupClass(), methodType(void.class, Map.class));
        }
        catch (TranslationException | VirtualMachineError exception) {
            throw exception;
        }
        catch (Throwable exception) {
            throw new TranslationException(exception);
        }
    }

    @Override public @NotNull MethodHandle exportedFunctionHandle(@NotNull String name) throws TranslationException {
        var id = index.exportedFunctions().get(name);

        if (id == null) {
            throw new IllegalArgumentException();
        }

        return getOrCreateFunction(id);
    }

    @Override public @NotNull VarHandle exportedTableHandle(@NotNull String name) throws TranslationException {
        var id = index.exportedTables().get(name);

        if (id == null) {
            throw new IllegalArgumentException();
        }

        try {
            var lookup = getOrCreateInstance();
            return lookup.findVarHandle(lookup.lookupClass(), tableName(id), Table.class);
        }
        catch (TranslationException | VirtualMachineError exception) {
            throw exception;
        }
        catch (Throwable exception) {
            throw new TranslationException(exception);
        }
    }

    @Override public @NotNull VarHandle exportedMemoryHandle(@NotNull String name) throws TranslationException {
        var id = index.exportedMemories().get(name);

        if (id == null) {
            throw new IllegalArgumentException();
        }

        try {
            var lookup = getOrCreateInstance();
            return lookup.findVarHandle(lookup.lookupClass(), memoryName(id), Memory.class);
        }
        catch (TranslationException | VirtualMachineError exception) {
            throw exception;
        }
        catch (Throwable exception) {
            throw new TranslationException(exception);
        }
    }

    private synchronized @NotNull MethodHandles.Lookup getOrCreateInstance() throws TranslationException {
        if (instanceLookup != null) {
            return instanceLookup;
        }

        try {
            var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            writer.visit(V17, ACC_FINAL, GENERATED_INSTANCE_INTERNAL_NAME, null, OBJECT_INTERNAL_NAME, new String[]{MODULE_INSTANCE_INTERNAL_NAME});

            for (var i = 0; i < index.importedFunctions().size(); i++) {
                writer.visitField(ACC_PRIVATE | ACC_FINAL, functionName(i), METHOD_HANDLE_DESCRIPTOR, null, null);
            }

            for (var i = 0; i < index.importedMemories().size() + index.definedMemories().size(); i++) {
                writer.visitField(ACC_PRIVATE | ACC_FINAL, memoryName(i), Memory.DESCRIPTOR, null, null);
            }

            for (var i = 0; i < index.importedTables().size() + index.definedTables().size(); i++) {
                writer.visitField(ACC_PRIVATE | ACC_FINAL, tableName(i), Table.DESCRIPTOR, null, null);
            }

            for (var i = 0; i < index.dataSegments().size(); i++) {
                writer.visitField(ACC_PRIVATE, dataSegmentName(i), MEMORY_SEGMENT_DESCRIPTOR, null, null);
            }

            for (var i = 0; i < index.elementSegments().size(); i++) {
                writer.visitField(ACC_PRIVATE, elementSegmentName(i), OBJECT_ARRAY_DESCRIPTOR, null, null);
            }

            for (var i = 0; i < index.definedGlobals().size(); i++) {
                var access = ACC_PRIVATE;

                if (index.definedGlobals().get(i).type().mutability() == Mutability.CONST) {
                    access |= ACC_FINAL;
                }

                writer.visitField(access, globalName(index.importedTables().size() + i), index.definedGlobals().get(i).type().valueType().descriptor(), null, null);
            }

            var constructor = writer.visitMethod(ACC_PRIVATE, "<init>", GENERATED_INSTANCE_CONSTRUCTOR_DESCRIPTOR, null, null);
            constructor.visitParameter("imports", ACC_FINAL);
            constructor.visitCode();

            constructor.visitVarInsn(ALOAD, 0);
            constructor.visitMethodInsn(INVOKESPECIAL, OBJECT_INTERNAL_NAME, "<init>", "()V", false);

            for (var i = 0; i < index.importedFunctions().size(); i++) {
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitVarInsn(ALOAD, 1);
                constructor.visitLdcInsn(index.importedFunctions().get(i).qualifiedName().moduleName());
                constructor.visitLdcInsn(index.importedFunctions().get(i).qualifiedName().name());
                constructor.visitLdcInsn(getMethodType(index.importedFunctions().get(i).type().descriptor()));
                constructor.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_FUNCTION_NAME, IMPORT_FUNCTION_DESCRIPTOR, false);
                constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, functionName(i), METHOD_HANDLE_DESCRIPTOR);
            }

            for (var i = 0; i < index.importedMemories().size(); i++) {
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitVarInsn(ALOAD, 1);
                constructor.visitLdcInsn(index.importedMemories().get(i).name().moduleName());
                constructor.visitLdcInsn(index.importedMemories().get(i).name().name());
                constructor.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_MEMORY_NAME, IMPORT_MEMORY_DESCRIPTOR, false);
                constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryName(i), Memory.DESCRIPTOR);
            }

            for (var i = 0; i < index.importedTables().size(); i++) {
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitVarInsn(ALOAD, 1);
                constructor.visitLdcInsn(index.importedTables().get(i).name().moduleName());
                constructor.visitLdcInsn(index.importedTables().get(i).name().name());
                constructor.visitMethodInsn(INVOKESTATIC, Importers.INTERNAL_NAME, IMPORT_TABLE_NAME, IMPORT_TABLE_DESCRIPTOR, false);
                constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableName(i), Table.DESCRIPTOR);
            }

            for (var i = 0; i < index.definedGlobals().size(); i++) {
                constructor.visitVarInsn(ALOAD, 0);

                var initialValue = index.definedGlobals().get(i).initialValue();

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
                    constructor.visitLdcInsn(new ConstantDynamic("_", METHOD_HANDLE_DESCRIPTOR, FUNCTION_REF_BOOTSTRAP, functionRefConstant.index()));
                }
                else {
                    throw new ClassCastException();
                }

                constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, globalName(index.importedGlobals().size() + i), index.definedGlobals().get(i).type().valueType().descriptor());
            }

            for (var i = 0; i < index.definedMemories().size(); i++) {
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitTypeInsn(NEW, Memory.INTERNAL_NAME);
                constructor.visitInsn(DUP);
                pushI32Constant(constructor, index.definedMemories().get(i).limits().unsignedMinimum());
                pushI32Constant(constructor, index.definedMemories().get(i).limits().unsignedMaximum());
                constructor.visitMethodInsn(INVOKESPECIAL, Memory.INTERNAL_NAME, "<init>", "(II)V", false);
                constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryName(index.importedMemories().size() + i), Memory.DESCRIPTOR);
            }

            for (var i = 0; i < index.definedTables().size(); i++) {
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitTypeInsn(NEW, Table.INTERNAL_NAME);
                constructor.visitInsn(DUP);
                pushI32Constant(constructor, index.definedTables().get(i).limits().unsignedMinimum());
                pushI32Constant(constructor, index.definedTables().get(i).limits().unsignedMaximum());
                constructor.visitMethodInsn(INVOKESPECIAL, Table.INTERNAL_NAME, "<init>", "(II)V", false);
                constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableName(index.importedTables().size() + i), Table.DESCRIPTOR);
            }

            for (var i = 0; i < index.dataSegments().size(); i++) {
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitLdcInsn(new ConstantDynamic("_", MEMORY_SEGMENT_DESCRIPTOR, DATA_BOOTSTRAP, i));

                if (index.dataSegments().get(i).mode() == DataSegment.Mode.ACTIVE) {
                    constructor.visitInsn(DUP);
                    pushI32Constant(constructor, index.dataSegments().get(i).memoryOffset());
                    constructor.visitVarInsn(ALOAD, 0);
                    constructor.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, memoryName(index.dataSegments().get(i).memoryIndex()), Memory.DESCRIPTOR);
                    constructor.visitMethodInsn(INVOKESTATIC, Memory.INTERNAL_NAME, Memory.INIT_FROM_ACTIVE_NAME, Memory.INIT_FROM_ACTIVE_DESCRIPTOR, false);
                }

                constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, dataSegmentName(i), MEMORY_SEGMENT_DESCRIPTOR);
            }

            for (var i = 0; i < index.elementSegments().size(); i++) {
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitLdcInsn(new ConstantDynamic("_", OBJECT_ARRAY_DESCRIPTOR, ELEMENT_BOOTSTRAP, i));

                if (index.elementSegments().get(i).mode() == ElementSegment.Mode.ACTIVE) {
                    constructor.visitInsn(DUP);
                    pushI32Constant(constructor, index.elementSegments().get(i).tableOffset());
                    constructor.visitVarInsn(ALOAD, 0);
                    constructor.visitFieldInsn(GETFIELD, GENERATED_INSTANCE_INTERNAL_NAME, tableName(index.elementSegments().get(i).tableIndex()), Table.DESCRIPTOR);
                    constructor.visitMethodInsn(INVOKESTATIC, Table.INTERNAL_NAME, Table.INIT_FROM_ACTIVE_NAME, Table.INIT_FROM_ACTIVE_DESCRIPTOR, false);
                }

                constructor.visitFieldInsn(PUTFIELD, GENERATED_INSTANCE_INTERNAL_NAME, elementSegmentName(i), OBJECT_ARRAY_DESCRIPTOR);
            }

            if (index.startFunctionId() != null) {
                constructor.visitVarInsn(ALOAD, 0);
                constructor.visitInvokeDynamicInsn("_", index.functionType(index.startFunctionId()).descriptor(), FUNCTION_BOOTSTRAP, index.startFunctionId());
            }

            constructor.visitInsn(RETURN);
            constructor.visitMaxs(0, 0);
            constructor.visitEnd();

            writer.visitEnd();
            var bytes = writer.toByteArray();
            return instanceLookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, this, false);
        }
        catch (VirtualMachineError exception) {
            throw exception;
        }
        catch (Throwable exception) {
            throw new TranslationException(exception);
        }
    }

    private synchronized @NotNull MethodHandle getOrCreateFunction(int id) throws TranslationException {
        if (functionHandles[id] != null) {
            return functionHandles[id];
        }

        try {
            var type = index.functionType(id);
            var methodType = type.methodType();

            if (id < index.importedFunctions().size()) {
                var lookup = getOrCreateInstance();

                var importInvoker = exactInvoker(type.methodType());
                var importGetter = lookup.findGetter(lookup.lookupClass(), functionName(id), MethodHandle.class);
                var erasedImportGetter = importGetter.asType(methodType(MethodHandle.class, ModuleInstance.class));
                var handle = filterArguments(importInvoker, 0, erasedImportGetter);

                var parameterCount = type.parameterTypes().size();
                var permutationOrder = new int[parameterCount + 2];
                permutationOrder[0] = parameterCount;

                for (var i = 0; i < parameterCount; i++) {
                    permutationOrder[i + 1] = i;
                }

                permutationOrder[parameterCount + 1] = parameterCount;
                return functionHandles[id] = permuteArguments(handle, methodType, permutationOrder);
            }

            var bytes = new FunctionTranslator().translate(index, id);
            var lookup = MethodHandles.lookup().defineHiddenClassWithClassData(bytes, this, false);
            return functionHandles[id] = lookup.findStatic(lookup.lookupClass(), FUNCTION_CLASS_ENTRY_NAME, methodType);
        }
        catch (TranslationException | VirtualMachineError exception) {
            throw exception;
        }
        catch (Throwable exception) {
            throw new TranslationException(exception);
        }
    }

    private static final String INTERNAL_NAME = getInternalName(ModuleImpl.class);

    static final Handle FUNCTION_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "functionBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite functionBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int id) throws IllegalAccessException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        return new ConstantCallSite(module.getOrCreateFunction(id));
    }

    static final Handle GLOBAL_GET_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "globalGetBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite globalGetBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int id) throws IllegalAccessException, NoSuchFieldException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        var instanceLookup = module.getOrCreateInstance();
        var type = module.index.globalType(id).valueType().jvmType();
        var handle = instanceLookup.findGetter(instanceLookup.lookupClass(), globalName(id), type);
        handle = handle.asType(methodType(type, ModuleInstance.class));
        return new ConstantCallSite(handle);
    }

    static final Handle TABLE_FIELD_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "tableFieldBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite tableFieldBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int id) throws IllegalAccessException, NoSuchFieldException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        var instanceLookup = module.getOrCreateInstance();
        var handle = instanceLookup.findGetter(instanceLookup.lookupClass(), tableName(id), Table.class);
        handle = handle.asType(methodType(Table.class, ModuleInstance.class));
        return new ConstantCallSite(handle);
    }

    static final Handle MEMORY_FIELD_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "memoryFieldBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite memoryFieldBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int id) throws IllegalAccessException, NoSuchFieldException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        var instanceLookup = module.getOrCreateInstance();
        var handle = instanceLookup.findGetter(instanceLookup.lookupClass(), memoryName(id), Memory.class);
        handle = handle.asType(methodType(Memory.class, ModuleInstance.class));
        return new ConstantCallSite(handle);
    }

    static final Handle ELEMENT_FIELD_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "elementFieldBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite elementFieldBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int id) throws IllegalAccessException, NoSuchFieldException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        var instanceLookup = module.getOrCreateInstance();
        var handle = instanceLookup.findGetter(instanceLookup.lookupClass(), elementSegmentName(id), Object[].class);
        handle = handle.asType(methodType(Object[].class, ModuleInstance.class));
        return new ConstantCallSite(handle);
    }

    static final Handle DATA_FIELD_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "dataFieldBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite dataFieldBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int id) throws IllegalAccessException, NoSuchFieldException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        var instanceLookup = module.getOrCreateInstance();
        var handle = instanceLookup.findGetter(instanceLookup.lookupClass(), dataSegmentName(id), MemorySegment.class);
        handle = handle.asType(methodType(MemorySegment.class, ModuleInstance.class));
        return new ConstantCallSite(handle);
    }

    static final Handle GLOBAL_SET_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "globalSetBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull CallSite globalSetBootstrap(@NotNull MethodHandles.Lookup lookup, String name, MethodType methodType, int id) throws IllegalAccessException, NoSuchFieldException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        var instanceLookup = module.getOrCreateInstance();
        var type = module.index.globalType(id).valueType().jvmType();
        var handle = instanceLookup.findSetter(instanceLookup.lookupClass(), globalName(id), type);
        handle = handle.asType(methodType(void.class, ModuleInstance.class, type));
        handle = permuteArguments(handle, methodType(void.class, type, ModuleInstance.class), 1, 0);
        return new ConstantCallSite(handle);
    }

    static final Handle FUNCTION_REF_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "functionRefBootstrap", methodDescriptor(CallSite.class, MethodHandles.Lookup.class, String.class, Class.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull MethodHandle functionRefBootstrap(@NotNull MethodHandles.Lookup lookup, String name, Class<?> clazz, int id) throws IllegalAccessException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        return module.getOrCreateFunction(id);
    }

    private static final Handle DATA_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "dataBootstrap", methodDescriptor(MemorySegment.class, MethodHandles.Lookup.class, String.class, Class.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull MemorySegment dataBootstrap(@NotNull MethodHandles.Lookup lookup, String name, Class<?> clazz, int id) throws IllegalAccessException {
        var module = classData(lookup, "_", ModuleImpl.class);
        return module.index.dataSegments().get(id).contents();
    }

    private static final Handle ELEMENT_BOOTSTRAP = new Handle(H_INVOKESTATIC, INTERNAL_NAME, "elementBootstrap", methodDescriptor(Object[].class, MethodHandles.Lookup.class, String.class, Class.class, int.class), false);
    @SuppressWarnings("unused") static @NotNull Object[] elementBootstrap(@NotNull MethodHandles.Lookup lookup, String name, Class<?> clazz, int id) throws IllegalAccessException, TranslationException {
        var module = classData(lookup, "_", ModuleImpl.class);
        var constantValues = module.index.elementSegments().get(id).values();
        var resolvedValues = new Object[constantValues.length];
        for (var i = 0; i < resolvedValues.length; i++) {
            if (constantValues[i] instanceof FunctionRefConstant functionRefConstant) {
                resolvedValues[i] = module.getOrCreateFunction(functionRefConstant.index());
            }
            else if (constantValues[i] != NullConstant.INSTANCE) {
                throw new ClassCastException();
            }
        }
        return resolvedValues;
    }
}
