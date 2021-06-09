package org.wastastic;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;

import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;

final class ModuleImpl implements Module {
    private final @NotNull MethodHandles.Lookup lookup;
    private final @NotNull Map<QualifiedName, ExportedFunction> exportedFunctions;
    private final @NotNull Map<QualifiedName, Integer> exportedTableIndices;
    private final @NotNull Map<QualifiedName, Integer> exportedMemoryIndices;

    ModuleImpl(
        @NotNull MethodHandles.Lookup lookup,
        @NotNull Map<QualifiedName, ExportedFunction> exportedFunctions,
        @NotNull Map<QualifiedName, Integer> exportedTableIndices,
        @NotNull Map<QualifiedName, Integer> exportedMemoryIndices
    ) {
        this.lookup = lookup;
        this.exportedFunctions = exportedFunctions;
        this.exportedTableIndices = exportedTableIndices;
        this.exportedMemoryIndices = exportedMemoryIndices;
    }

    @Override public @NotNull MethodHandle instantiationHandle() {
        try {
            return lookup.findConstructor(lookup.lookupClass(), methodType(void.class, Map.class));
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull MethodHandle exportedFunctionHandle(@NotNull QualifiedName qualifiedName) {
        var function = exportedFunctions.get(qualifiedName);

        if (function == null) {
            throw new IllegalArgumentException();
        }

        var directType = function.type().jvmType(lookup.lookupClass());

        MethodHandle directHandle;
        try {
            directHandle = lookup.findStatic(lookup.lookupClass(), "f-" + function.index(), directType);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }

        var permutedParameterTypes = new Class<?>[function.type().parameterTypes().size() + 1];

        permutedParameterTypes[0] = lookup.lookupClass();

        for (var i = 0; i < function.type().parameterTypes().size(); i++) {
            permutedParameterTypes[i + 1] = function.type().parameterTypes().get(i).jvmType();
        }

        var permutationOrder = new int[permutedParameterTypes.length];

        permutationOrder[0] = function.type().parameterTypes().size();

        for (var i = 0; i < function.type().parameterTypes().size(); i++) {
            permutationOrder[i + 1] = i;
        }

        var permutedType = methodType(lookup.lookupClass(), permutedParameterTypes);
        return permuteArguments(directHandle, permutedType, permutationOrder);
    }

    @Override public @NotNull VarHandle exportedTableHandle(@NotNull QualifiedName qualifiedName) {
        var index = exportedTableIndices.get(qualifiedName);

        if (index == null) {
            throw new IllegalArgumentException();
        }

        try {
            return lookup.findVarHandle(lookup.lookupClass(), "t-" + index, Table.class);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override public @NotNull VarHandle exportedMemoryHandle(@NotNull QualifiedName qualifiedName) {
        var index = exportedMemoryIndices.get(qualifiedName);

        if (index == null) {
            throw new IllegalArgumentException();
        }

        try {
            return lookup.findVarHandle(lookup.lookupClass(), "m-" + index, Memory.class);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
