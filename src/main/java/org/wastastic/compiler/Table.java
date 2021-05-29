package org.wastastic.compiler;

sealed interface Table permits TableType, ImportedTable {
    TableType getType();
}
