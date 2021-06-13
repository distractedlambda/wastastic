package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;

import static java.lang.invoke.MethodType.methodType;
import static org.wastastic.Names.functionMethodName;
import static org.wastastic.Names.memoryFieldName;
import static org.wastastic.Names.tableFieldName;

final class ModuleImpl implements Module {
    private final @NotNull MethodHandles.Lookup instanceLookup;
    private final @NotNull Map<String, ExportedFunction> exportedFunctions;
    private final @NotNull Map<String, Integer> exportedTableIndices;
    private final @NotNull Map<String, Integer> exportedMemoryIndices;

    ModuleImpl(
        @NotNull MethodHandles.Lookup instanceLookup,
        @NotNull Map<String, ExportedFunction> exportedFunctions,
        @NotNull Map<String, Integer> exportedTableIndices,
        @NotNull Map<String, Integer> exportedMemoryIndices
    ) {
        this.instanceLookup = instanceLookup;
        this.exportedFunctions = exportedFunctions;
        this.exportedTableIndices = exportedTableIndices;
        this.exportedMemoryIndices = exportedMemoryIndices;
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

        var type = function.type().jvmType();

        try {
            return instanceLookup.findStatic(instanceClass(), functionMethodName(function.index()), type);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull VarHandle exportedTableHandle(@NotNull String name) {
        var index = exportedTableIndices.get(name);

        if (index == null) {
            throw new IllegalArgumentException();
        }

        try {
            return instanceLookup.findVarHandle(instanceClass(), tableFieldName(index), Table.class);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull VarHandle exportedMemoryHandle(@NotNull String name) {
        var index = exportedMemoryIndices.get(name);

        if (index == null) {
            throw new IllegalArgumentException();
        }

        try {
            return instanceLookup.findVarHandle(instanceClass(), memoryFieldName(index), Memory.class);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
