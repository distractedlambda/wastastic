package org.wastastic;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

record ModuleIndex(
    @NotNull ResourceScope scope,

    @NotNull List<@NotNull FunctionType> types,

    @NotNull List<@NotNull ImportedFunction> importedFunctions,
    @NotNull List<@NotNull ImportedGlobal> importedGlobals,
    @NotNull List<@NotNull ImportedTable> importedTables,
    @NotNull List<@NotNull ImportedMemory> importedMemories,

    @NotNull List<@NotNull FunctionType> definedFunctions,
    @NotNull List<@NotNull DefinedGlobal> definedGlobals,
    @NotNull List<@NotNull TableType> definedTables,
    @NotNull List<@NotNull MemoryType> definedMemories,

    @NotNull Map<@NotNull String, @NotNull Integer> exportedFunctions,
    @NotNull Map<@NotNull String, @NotNull Integer> exportedGlobals,
    @NotNull Map<@NotNull String, @NotNull Integer> exportedTables,
    @NotNull Map<@NotNull String, @NotNull Integer> exportedMemories,

    @NotNull List<@NotNull MemorySegment> functionBodies,
    @NotNull List<@NotNull DataSegment> dataSegments,
    @NotNull List<@NotNull ElementSegment> elementSegments,

    @Nullable Integer startFunctionId,

    @Nullable String moduleName,
    @NotNull List<@Nullable String> functionNames
) {
    static @NotNull ModuleIndex of(@NotNull MemorySegment input) throws TranslationException {
        return new Indexer().buildIndex(input);
    }

    @NotNull FunctionType functionType(int index) {
        if (index < importedFunctions.size()) {
            return importedFunctions.get(index).type();
        }
        else {
            return definedFunctions.get(index - importedFunctions.size());
        }
    }

    @NotNull GlobalType globalType(int index) {
        if (index < importedGlobals.size()) {
            return importedGlobals.get(index).type();
        }
        else {
            return definedGlobals.get(index - importedGlobals.size()).type();
        }
    }

    @NotNull TableType tableType(int index) {
        if (index < importedTables.size()) {
            return importedTables.get(index).type();
        }
        else {
            return definedTables.get(index - importedTables.size());
        }
    }

    private static final class Indexer {
        private ResourceScope scope;

        private final @NotNull List<@NotNull FunctionType> types = new ArrayList<>();

        private final @NotNull List<@NotNull ImportedFunction> importedFunctions = new ArrayList<>();
        private final @NotNull List<@NotNull ImportedGlobal> importedGlobals = new ArrayList<>();
        private final @NotNull List<@NotNull ImportedTable> importedTables = new ArrayList<>();
        private final @NotNull List<@NotNull ImportedMemory> importedMemories = new ArrayList<>();

        private final @NotNull List<@NotNull FunctionType> definedFunctions = new ArrayList<>();
        private final @NotNull List<@NotNull DefinedGlobal> definedGlobals = new ArrayList<>();
        private final @NotNull List<@NotNull TableType> definedTables = new ArrayList<>();
        private final @NotNull List<@NotNull MemoryType> definedMemories = new ArrayList<>();

        private final @NotNull Map<@NotNull String, @NotNull Integer> exportedFunctions = new HashMap<>();
        private final @NotNull Map<@NotNull String, @NotNull Integer> exportedGlobals = new HashMap<>();
        private final @NotNull Map<@NotNull String, @NotNull Integer> exportedTables = new HashMap<>();
        private final @NotNull Map<@NotNull String, @NotNull Integer> exportedMemories = new HashMap<>();

        private final @NotNull List<@NotNull MemorySegment> functionBodies = new ArrayList<>();
        private final @NotNull List<@NotNull DataSegment> dataSegments = new ArrayList<>();
        private final @NotNull List<@NotNull ElementSegment> elementSegments = new ArrayList<>();

        private @Nullable Integer startFunctionIndex;

        private @Nullable String moduleName;
        private String[] functionNames;

        private @NotNull ModuleIndex buildIndex(@NotNull MemorySegment input) throws TranslationException {
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

            return new ModuleIndex(
                input.scope(),
                List.copyOf(types),
                List.copyOf(importedFunctions),
                List.copyOf(importedGlobals),
                List.copyOf(importedTables),
                List.copyOf(importedMemories),
                List.copyOf(definedFunctions),
                List.copyOf(definedGlobals),
                List.copyOf(definedTables),
                List.copyOf(definedMemories),
                Map.copyOf(exportedFunctions),
                Map.copyOf(exportedGlobals),
                Map.copyOf(exportedTables),
                Map.copyOf(exportedMemories),
                List.copyOf(functionBodies),
                List.copyOf(dataSegments),
                List.copyOf(elementSegments),
                startFunctionIndex,
                moduleName,
                List.of(functionNames)
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
                        functionNames = new String[importedFunctions.size() + definedFunctions.size()];
                        for (var i = subsectionReader.nextUnsigned32(); i != 0; i--) {
                            functionNames[subsectionReader.nextUnsigned32()] = subsectionReader.nextName();
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
            for (var remaining = reader.nextUnsigned32(); remaining != 0; remaining--) {
                var name = reader.nextName();
                var kindCode = reader.nextByte();
                var index = reader.nextUnsigned32();

                var map = switch (kindCode) {
                    case 0x00 -> exportedFunctions;
                    case 0x01 -> exportedTables;
                    case 0x02 -> exportedMemories;
                    case 0x03 -> exportedGlobals;
                    default -> throw new TranslationException("Invalid export description");
                };

                map.put(name, index);
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

            elementSegments.add(new ElementSegment(List.of(values), mode, tableIndex, tableOffset));
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
                var contents = reader.nextSlice(Integer.toUnsignedLong(contentsSize));
                dataSegments.add(new DataSegment(contents, mode, memoryIndex, memoryOffset));
            }
        }

        private void readDataCountSection(@NotNull WasmReader reader) {
            // FIXME: do something with this
        }
    }
}
