package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.OptionalInt;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class ParsedModule {
    private final @NotNull List<String> functionNames;
    private final @NotNull List<String> memoryNames;
    private final @NotNull List<String> tableNames;
    private final @NotNull List<String> globalNames;
    private final @NotNull List<FunctionType> types;
    private final @NotNull List<ImportedFunction> importedFunctions;
    private final @NotNull List<FunctionType> definedFunctions;
    private final @NotNull List<ImportedGlobal> importedGlobals;
    private final @NotNull List<DefinedGlobal> definedGlobals;
    private final @NotNull List<ImportedTable> importedTables;
    private final @NotNull List<TableType> definedTables;
    private final @NotNull List<ImportedMemory> importedMemories;
    private final @NotNull List<MemoryType> definedMemories;
    private final @NotNull List<Export> exports;
    private final @Nullable String moduleName;
    private final int startFunctionIndex;
    private final @NotNull List<MemorySegment> functionBodies;
    private final @NotNull List<DataSegment> dataSegments;
    private final @NotNull List<ElementSegment> elementSegments;
    private final @NotNull List<String> dataSegmentNames;
    private final @NotNull List<String> elementSegmentNames;

    ParsedModule(
        @NotNull List<String> functionNames,
        @NotNull List<String> memoryNames,
        @NotNull List<String> tableNames,
        @NotNull List<String> globalNames,
        @NotNull List<FunctionType> types,
        @NotNull List<ImportedFunction> importedFunctions,
        @NotNull List<FunctionType> definedFunctions,
        @NotNull List<ImportedGlobal> importedGlobals,
        @NotNull List<DefinedGlobal> definedGlobals,
        @NotNull List<ImportedTable> importedTables,
        @NotNull List<TableType> definedTables,
        @NotNull List<ImportedMemory> importedMemories,
        @NotNull List<MemoryType> definedMemories,
        @NotNull List<Export> exports,
        @Nullable String moduleName,
        int startFunctionIndex,
        @NotNull List<MemorySegment> functionBodies,
        @NotNull List<DataSegment> dataSegments,
        @NotNull List<ElementSegment> elementSegments,
        @NotNull List<String> dataSegmentNames,
        @NotNull List<String> elementSegmentNames
    ) {
        this.functionNames = requireNonNull(functionNames);
        this.memoryNames = requireNonNull(memoryNames);
        this.tableNames = requireNonNull(tableNames);
        this.globalNames = requireNonNull(globalNames);
        this.types = requireNonNull(types);
        this.importedFunctions = requireNonNull(importedFunctions);
        this.definedFunctions = requireNonNull(definedFunctions);
        this.importedGlobals = requireNonNull(importedGlobals);
        this.definedGlobals = requireNonNull(definedGlobals);
        this.importedTables = requireNonNull(importedTables);
        this.definedTables = requireNonNull(definedTables);
        this.importedMemories = requireNonNull(importedMemories);
        this.definedMemories = requireNonNull(definedMemories);
        this.exports = requireNonNull(exports);
        this.moduleName = moduleName;
        this.startFunctionIndex = startFunctionIndex;
        this.functionBodies = requireNonNull(functionBodies);
        this.dataSegments = requireNonNull(dataSegments);
        this.elementSegments = requireNonNull(elementSegments);
        this.dataSegmentNames = requireNonNull(dataSegmentNames);
        this.elementSegmentNames = requireNonNull(elementSegmentNames);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<String> functionNames() {
        return unmodifiableList(functionNames);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<String> memoryNames() {
        return unmodifiableList(memoryNames);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<String> tableNames() {
        return unmodifiableList(tableNames);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<String> globalNames() {
        return unmodifiableList(globalNames);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<FunctionType> types() {
        return unmodifiableList(types);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<ImportedFunction> importedFunctions() {
        return unmodifiableList(importedFunctions);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<FunctionType> definedFunctions() {
        return unmodifiableList(definedFunctions);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<ImportedGlobal> importedGlobals() {
        return unmodifiableList(importedGlobals);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<DefinedGlobal> definedGlobals() {
        return unmodifiableList(definedGlobals);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<ImportedTable> importedTables() {
        return unmodifiableList(importedTables);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<TableType> definedTables() {
        return unmodifiableList(definedTables);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<ImportedMemory> importedMemories() {
        return unmodifiableList(importedMemories);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<MemoryType> definedMemories() {
        return unmodifiableList(definedMemories);
    }

    @Contract(pure = true) @NotNull GlobalType globalType(int index) {
        if (index < importedGlobals.size()) {
            return importedGlobals.get(index).type();
        }
        else {
            return definedGlobals.get(index - importedGlobals.size()).type();
        }
    }

    @Contract(pure = true) @NotNull TableType tableType(int index) {
        if (index < importedTables.size()) {
            return importedTables.get(index).type();
        }
        else {
            return definedTables.get(index - importedTables.size());
        }
    }

    @Contract(pure = true) @NotNull FunctionType functionType(int index) {
        if (index < importedFunctions.size()) {
            return importedFunctions.get(index).type();
        }
        else {
            return definedFunctions.get(index - definedFunctions.size());
        }
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<Export> exports() {
        return unmodifiableList(exports);
    }

    @Contract(pure = true) @Nullable String moduleName() {
        return moduleName;
    }

    @Contract(pure = true) @NotNull OptionalInt startFunctionIndex() {
        if (startFunctionIndex < 0) {
            return OptionalInt.empty();
        }
        else {
            return OptionalInt.of(startFunctionIndex);
        }
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<MemorySegment> functionBodies() {
        return unmodifiableList(functionBodies);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<DataSegment> dataSegments() {
        return unmodifiableList(dataSegments);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<ElementSegment> elementSegments() {
        return unmodifiableList(elementSegments);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<String> dataSegmentNames() {
        return unmodifiableList(dataSegmentNames);
    }

    @Contract(pure = true) @NotNull @Unmodifiable List<String> elementSegmentNames() {
        return unmodifiableList(elementSegmentNames);
    }
}
