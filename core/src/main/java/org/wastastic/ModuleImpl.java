package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;

import static java.lang.invoke.MethodType.methodType;

final class ModuleImpl implements Module {
    private final @NotNull MethodHandles.Lookup instanceLookup;
    private final @NotNull Map<String, ExportedFunction> exportedFunctions;
    private final @NotNull Map<String, String> exportedTableNames;
    private final @NotNull Map<String, String> exportedMemoryNames;

    ModuleImpl(
        @NotNull MethodHandles.Lookup instanceLookup,
        @NotNull Map<String, ExportedFunction> exportedFunctions,
        @NotNull Map<String, String> exportedTableNames,
        @NotNull Map<String, String> exportedMemoryNames
    ) {
        this.instanceLookup = instanceLookup;
        this.exportedFunctions = exportedFunctions;
        this.exportedTableNames = exportedTableNames;
        this.exportedMemoryNames = exportedMemoryNames;
    }

    private @NotNull Class<?> instanceClass() {
        return instanceLookup.lookupClass();
    }

    @Override public @NotNull MethodHandle instantiationHandle() {
        try {
            return instanceLookup.findConstructor(instanceClass(), methodType(void.class, Map.class));
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull MethodHandle exportedFunctionHandle(@NotNull String name) {
        var function = exportedFunctions.get(name);

        if (function == null) {
            throw new IllegalArgumentException();
        }

        try {
            return instanceLookup.findStatic(instanceClass(), function.name(), function.type().jvmType());
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull VarHandle exportedTableHandle(@NotNull String name) {
        var fieldName = exportedTableNames.get(name);

        if (fieldName == null) {
            throw new IllegalArgumentException();
        }

        try {
            return instanceLookup.findVarHandle(instanceClass(), fieldName, Table.class);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull VarHandle exportedMemoryHandle(@NotNull String name) {
        var fieldName = exportedMemoryNames.get(name);

        if (fieldName == null) {
            throw new IllegalArgumentException();
        }

        try {
            return instanceLookup.findVarHandle(instanceClass(), fieldName, Memory.class);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
