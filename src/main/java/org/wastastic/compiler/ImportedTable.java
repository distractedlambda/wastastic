package org.wastastic.compiler;

import static java.util.Objects.requireNonNull;

final class ImportedTable extends Import {
    private final TableType type;

    ImportedTable(String moduleName, String name, TableType type) {
        super(moduleName, name);
        this.type = requireNonNull(type);
    }

    TableType getType() {
        return type;
    }
}
