package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.wastastic.Names.importName;
import static org.wastastic.WasmOpcodes.SECTION_CODE;
import static org.wastastic.WasmOpcodes.SECTION_CUSTOM;
import static org.wastastic.WasmOpcodes.SECTION_DATA;
import static org.wastastic.WasmOpcodes.SECTION_DATA_COUNT;
import static org.wastastic.WasmOpcodes.SECTION_ELEMENT;
import static org.wastastic.WasmOpcodes.SECTION_EXPORT;
import static org.wastastic.WasmOpcodes.SECTION_FUNCTION;
import static org.wastastic.WasmOpcodes.SECTION_GLOBAL;
import static org.wastastic.WasmOpcodes.SECTION_IMPORT;
import static org.wastastic.WasmOpcodes.SECTION_MEMORY;
import static org.wastastic.WasmOpcodes.SECTION_START;
import static org.wastastic.WasmOpcodes.SECTION_TABLE;
import static org.wastastic.WasmOpcodes.SECTION_TYPE;
import static org.wastastic.WasmOpcodes.TYPE_FUNCTION;

final class ModuleParser {
    private @Nullable String moduleName;

    private final Map<String, Integer> usedNames = new HashMap<>();

    private String[] functionNames;
    private String[] tableNames;
    private String[] memoryNames;
    private String[] globalNames;

    private final List<MemorySegment> functionBodies = new ArrayList<>();
    private final List<FunctionType> types = new ArrayList<>();
    private final List<ImportedFunction> importedFunctions = new ArrayList<>();
    private final List<FunctionType> definedFunctions = new ArrayList<>();
    private final List<ImportedTable> importedTables = new ArrayList<>();
    private final List<TableType> definedTables = new ArrayList<>();
    private final List<ImportedMemory> importedMemories = new ArrayList<>();
    private final List<MemoryType> definedMemories = new ArrayList<>();
    private final List<ImportedGlobal> importedGlobals = new ArrayList<>();
    private final List<DefinedGlobal> definedGlobals = new ArrayList<>();
    private final List<Export> exports = new ArrayList<>();
    private final List<ElementSegment> elementSegments = new ArrayList<>();
    private final List<DataSegment> dataSegments = new ArrayList<>();

    private int startFunctionIndex = -1;

    private final ResourceScope dataSegmentsScope = ResourceScope.newImplicitScope();

    @NotNull ParsedModule parse(@NotNull MemorySegment input) throws TranslationException {
        var reader = new WasmReader(input);

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

        while (reader.hasRemaining()) {
            byte sectionId = reader.nextByte();
            var sectionSize = Integer.toUnsignedLong(reader.nextUnsigned32());
            var sectionBody = reader.nextSlice(sectionSize);
            var bodyReader = new WasmReader(sectionBody);

            switch (sectionId) {
                case SECTION_CUSTOM -> readCustomSection(bodyReader);
                case SECTION_TYPE -> readTypeSection(bodyReader);
                case SECTION_IMPORT -> readImportSection(bodyReader);
                case SECTION_FUNCTION -> readFunctionSection(bodyReader);
                case SECTION_TABLE -> readTableSection(bodyReader);
                case SECTION_MEMORY -> readMemorySection(bodyReader);
                case SECTION_GLOBAL -> readGlobalSection(bodyReader);
                case SECTION_EXPORT -> readExportSection(bodyReader);
                case SECTION_START -> readStartSection(bodyReader);
                case SECTION_ELEMENT -> readElementSection(bodyReader);
                case SECTION_CODE -> readCodeSection(bodyReader);
                case SECTION_DATA -> readDataSection(bodyReader);
                case SECTION_DATA_COUNT -> readDataCountSection(bodyReader);
                default -> throw new TranslationException("Invalid section ID: " + sectionId);
            }
        }

        ensureNameTables();

        for (var i = 0; i < importedFunctions.size(); i++) {
            if (functionNames[i] == null) {
                functionNames[i] = legalizeName(importName(importedFunctions.get(i).qualifiedName()));
            }
        }

        for (var i = 0; i < definedFunctions.size(); i++) {
            var functionIndex = importedFunctions.size() + i;

            if (functionNames[functionIndex] == null) {
                functionNames[functionIndex] = "$function$" + functionIndex;
            }
        }

        for (var i = 0; i < importedTables.size(); i++) {
            if (tableNames[i] == null) {
                tableNames[i] = legalizeName(importName(importedTables.get(i).name()));
            }
        }

        for (var i = 0; i < definedTables.size(); i++) {
            var tableIndex = importedTables.size() + i;

            if (tableNames[tableIndex] == null) {
                tableNames[tableIndex] = "$table$" + tableIndex;
            }
        }

        for (var i = 0; i < importedMemories.size(); i++) {
            if (memoryNames[i] == null) {
                memoryNames[i] = legalizeName(importName(importedMemories.get(i).name()));
            }
        }

        for (var i = 0; i < definedMemories.size(); i++) {
            var memoryIndex = importedMemories.size() + i;
            if (memoryNames[memoryIndex] == null) {
                memoryNames[memoryIndex] = "$memory$" + memoryIndex;
            }
        }

        for (var i = 0; i < definedGlobals.size(); i++) {
            var globalIndex = importedGlobals.size() + i;

            if (globalNames[globalIndex] == null) {
                globalNames[globalIndex] = "$global$" + globalIndex;
            }
        }

        var dataSegmentNames = new String[dataSegments.size()];

        for (var i = 0; i < dataSegments.size(); i++) {
            dataSegmentNames[i] = "$dataSegment$" + i;
        }

        var elementSegmentNames = new String[elementSegments.size()];

        for (var i = 0; i < elementSegments.size(); i++) {
            elementSegmentNames[i] = "$elementSegment$" + i;
        }

        return new ParsedModule(
            asList(functionNames),
            asList(memoryNames),
            asList(tableNames),
            asList(globalNames),
            types,
            importedFunctions,
            definedFunctions,
            importedGlobals,
            definedGlobals,
            importedTables,
            definedTables,
            importedMemories,
            definedMemories,
            exports,
            moduleName,
            startFunctionIndex,
            functionBodies,
            dataSegments,
            elementSegments,
            asList(dataSegmentNames),
            asList(elementSegmentNames)
        );
    }

    private void readCustomSection(@NotNull WasmReader reader) {
        switch (reader.nextName()) {
            case "name" -> {
                readNameSection(reader);
            }
        }
    }

    private void readNameSection(@NotNull WasmReader reader) {
        while (reader.hasRemaining()) {
            var subsectionId = reader.nextByte();
            var subsectionReader = new WasmReader(reader.nextSlice(Integer.toUnsignedLong(reader.nextUnsigned32())));
            switch (subsectionId) {
                case 0x00 -> {
                    moduleName = subsectionReader.nextName();
                }

                case 0x01 -> {
                    ensureNameTables();
                    for (var i = subsectionReader.nextUnsigned32(); i != 0; i--) {
                        functionNames[subsectionReader.nextUnsigned32()] = legalizeName(subsectionReader.nextName());
                    }
                }
            }
        }
    }

    private void readTypeSection(@NotNull WasmReader reader) throws TranslationException {
        var remaining = reader.nextUnsigned32();
        for (; remaining != 0; remaining--) {
            if (reader.nextByte() != TYPE_FUNCTION) {
                throw new TranslationException("Invalid function type");
            }
            types.add(new FunctionType(reader.nextResultType(), reader.nextResultType()));
        }
    }

    private void readImportSection(@NotNull WasmReader reader) throws TranslationException {
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            var moduleName = reader.nextName();
            var name = reader.nextName();
            var qualifiedName = new QualifiedName(moduleName, name);
            switch (reader.nextByte()) {
                case 0x00 -> importedFunctions.add(new ImportedFunction(qualifiedName, types.get(reader.nextUnsigned32())));
                case 0x01 -> importedTables.add(new ImportedTable(qualifiedName, reader.nextTableType()));
                case 0x02 -> importedMemories.add(new ImportedMemory(qualifiedName, reader.nextMemoryType()));
                case 0x03 -> throw new TranslationException("TODO implement global imports");
                default -> throw new TranslationException("Invalid import description");
            }
        }
    }

    private void readFunctionSection(@NotNull WasmReader reader) {
        var remaining = reader.nextUnsigned32();
        for (; remaining != 0; remaining--) {
            definedFunctions.add(types.get(reader.nextUnsigned32()));
        }
    }

    private void readTableSection(@NotNull WasmReader reader) throws TranslationException {
        var remaining = reader.nextUnsigned32();
        for (; remaining != 0; remaining--) {
            definedTables.add(reader.nextTableType());
        }
    }

    private void readMemorySection(@NotNull WasmReader reader) throws TranslationException {
        var remaining = reader.nextUnsigned32();
        for (; remaining != 0; remaining--) {
            definedMemories.add(reader.nextMemoryType());
        }
    }

    private void readGlobalSection(@NotNull WasmReader reader) throws TranslationException {
        var remaining = reader.nextUnsigned32();
        for (; remaining != 0; remaining--) {
            var type = reader.nextValueType();

            var mutability = switch (reader.nextByte()) {
                case 0x00 -> Mutability.CONST;
                case 0x01 -> Mutability.VAR;
                default -> throw new TranslationException("Invalid mutability");
            };

            definedGlobals.add(new DefinedGlobal(new GlobalType(type, mutability), reader.nextConstantExpression()));
        }
    }

    private void readExportSection(@NotNull WasmReader reader) throws TranslationException {
        ensureNameTables();
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            var name = reader.nextName();
            var legalizedName = legalizeName(name);
            var kindCode = reader.nextByte();
            var index = reader.nextUnsigned32();

            var kind = switch (kindCode) {
                case 0x00 -> {
                    functionNames[index] = legalizedName;
                    yield ExportKind.FUNCTION;
                }

                case 0x01 -> {
                    tableNames[index] = legalizedName;
                    yield ExportKind.TABLE;
                }

                case 0x02 -> {
                    memoryNames[index] = legalizedName;
                    yield ExportKind.MEMORY;
                }

                case 0x03 -> {
                    globalNames[index] = legalizedName;
                    yield ExportKind.GLOBAL;
                }

                default -> throw new TranslationException("Invalid export description");
            };

            exports.add(new Export(name, kind, index));
        }
    }

    private void readStartSection(@NotNull WasmReader reader) {
        startFunctionIndex = reader.nextUnsigned32();
    }

    private void readElementSection(@NotNull WasmReader reader) throws TranslationException {
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            readElementSegment(reader);
        }
    }

    private void readElementSegment(@NotNull WasmReader reader) throws TranslationException {
        var kind = reader.nextByte();

        if (kind < 0 || kind > 0x07) {
            throw new TranslationException("Invalid element segment");
        }

        var mode = switch (kind & 0x3) {
            case 0, 2 -> ElementSegment.Mode.ACTIVE;
            case 1 -> ElementSegment.Mode.PASSIVE;
            case 3 -> ElementSegment.Mode.DECLARATIVE;
            default -> throw new AssertionError();
        };

        var tableIndex = switch (kind) {
            case 0, 1, 3, 4, 5, 7 -> 0;
            case 2, 6 -> reader.nextUnsigned32();
            default -> throw new AssertionError();
        };

        var tableOffset = ((kind & 1) == 0) ? reader.nextI32ConstantExpression() : 0;

        var values = switch (kind) {
            case 0, 1, 2, 3 -> {
                var v = new Constant[reader.nextUnsigned32()];

                for (var i = 0; i < v.length; i++) {
                    v[i] = new FunctionRefConstant(reader.nextUnsigned32());
                }

                yield v;
            }

            case 4 -> {
                var v = new Constant[reader.nextUnsigned32()];

                for (var i = 0; i < v.length; i++) {
                    v[i] = reader.nextFunctionRefConstantExpression();
                }

                yield v;
            }

            case 5, 6, 7 -> {
                var type = reader.nextReferenceType();
                var v = new Constant[reader.nextUnsigned32()];

                switch (type) {
                    case FUNCREF -> {
                        for (var i = 0; i < v.length; i++) {
                            v[i] = reader.nextFunctionRefConstantExpression();
                        }
                    }

                    case EXTERNREF -> {
                        for (var i = 0; i < v.length; i++) {
                            v[i] = reader.nextExternRefConstantExpression();
                        }
                    }

                    default -> throw new AssertionError();
                }

                yield v;
            }

            default -> throw new AssertionError();
        };

        elementSegments.add(new ElementSegment(values, mode, tableIndex, tableOffset));
    }

    private void readCodeSection(@NotNull WasmReader reader) {
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            functionBodies.add(reader.nextSlice(Integer.toUnsignedLong(reader.nextUnsigned32())));
        }
    }

    private void readDataSection(@NotNull WasmReader reader) throws TranslationException {
        for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
            var kind = reader.nextByte();

            if (kind < 0 || kind > 0x02) {
                throw new TranslationException("Invalid data segment kind: " + kind);
            }

            var mode = (kind == 0 || kind == 2) ? DataSegment.Mode.ACTIVE : DataSegment.Mode.PASSIVE;
            var memoryIndex = (kind == 2) ? reader.nextUnsigned32() : 0;
            var memoryOffset = (kind == 0 || kind == 2) ? reader.nextI32ConstantExpression() : 0;

            var contentsSize = reader.nextUnsigned32();
            var contents = reader.nextData(Integer.toUnsignedLong(contentsSize), dataSegmentsScope);
            dataSegments.add(new DataSegment(contents, mode, memoryIndex, memoryOffset));
        }
    }

    private void readDataCountSection(@NotNull WasmReader reader) {
    }

    private void ensureNameTables() {
        if (functionNames == null) {
            functionNames = new String[importedFunctions.size() + definedFunctions.size()];
            tableNames = new String[importedTables.size() + definedTables.size()];
            memoryNames = new String[importedMemories.size() + definedMemories.size()];
            globalNames = new String[importedGlobals.size() + definedGlobals.size()];
        }
    }

    private @NotNull String legalizeName(@NotNull String name) {
        var builder = new StringBuilder();

        for (var i = 0; i < name.length(); i++) {
            switch (name.charAt(i)) {
                case '.' -> builder.append("%2e");
                case ';' -> builder.append("%3b");
                case '[' -> builder.append("%5b");
                case '/' -> builder.append("%2f");
                case '<' -> builder.append("%3c");
                case '>' -> builder.append("%3e");
                case '%' -> builder.append("%25");
                case '$' -> builder.append("%24");
                default -> builder.append(name.charAt(i));
            }
        }

        var escapedName = builder.toString();
        var existingCount = usedNames.putIfAbsent(escapedName, 0);

        if (existingCount == null) {
            return escapedName;
        }
        else {
            usedNames.put(escapedName, existingCount + 1);
            return escapedName + "$" + existingCount;
        }
    }
}
