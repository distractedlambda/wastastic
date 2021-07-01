package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Map;

import static org.objectweb.asm.Type.getInternalName;
import static org.wastastic.Names.methodDescriptor;

final class Importers {
    private Importers() {}

    static final String INTERNAL_NAME = getInternalName(Importers.class);

    private static @NotNull Object fetchImport(
        @NotNull Map<QualifiedName, Object> imports,
        @NotNull String moduleName,
        @NotNull String name
    ) throws MissingImportException {
        var qualifiedName = new QualifiedName(moduleName, name);
        var value = imports.get(qualifiedName);

        if (value == null) {
            throw new MissingImportException(qualifiedName);
        }

        return value;
    }

    static final String IMPORT_FUNCTION_NAME = "importFunction";
    static final String IMPORT_FUNCTION_DESCRIPTOR = methodDescriptor(MethodHandle.class, Map.class, String.class, String.class, MethodType.class);

    @SuppressWarnings("unused")
    static @NotNull MethodHandle importFunction(
        @NotNull Map<QualifiedName, Object> imports,
        @NotNull String moduleName,
        @NotNull String name,
        @NotNull MethodType requiredType
    ) throws MissingImportException, InvalidImportException {
        if (!(fetchImport(imports, moduleName, name) instanceof MethodHandle handle)) {
            throw new InvalidImportException("Value provided for imported function is not a method handle", null);
        }

        try {
            return handle.asType(requiredType);
        }
        catch (WrongMethodTypeException exception) {
            throw new InvalidImportException("Method handle provided for imported function has an incompatible type", exception);
        }
    }

    static final String IMPORT_TABLE_NAME = "importTable";
    static final String IMPORT_TABLE_DESCRIPTOR = methodDescriptor(Table.class, Map.class, String.class, String.class);

    @SuppressWarnings("unused")
    static @NotNull Table importTable(
        @NotNull Map<QualifiedName, Object> imports,
        @NotNull String moduleName,
        @NotNull String name
    ) throws MissingImportException, InvalidImportException {
        // FIXME: check limits
        // FIXME: track element type?
        if (!(fetchImport(imports, moduleName, name) instanceof Table table)) {
            throw new InvalidImportException("Value provided for imported table is not a table", null);
        }

        return table;
    }

    static final String IMPORT_MEMORY_NAME = "importMemory";
    static final String IMPORT_MEMORY_DESCRIPTOR = methodDescriptor(Memory.class, Map.class, String.class, String.class);

    @SuppressWarnings("unused")
    static @NotNull Memory importMemory(
        @NotNull Map<QualifiedName, Object> imports,
        @NotNull String moduleName,
        @NotNull String name
    ) throws MissingImportException, InvalidImportException {
        // FIXME: check limits
        if (!(fetchImport(imports, moduleName, name) instanceof Memory memory)) {
            throw new InvalidImportException("Value provided for imported memory is not a memory", null);
        }

        return memory;
    }
}
