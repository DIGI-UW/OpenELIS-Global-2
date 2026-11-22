package org.openelisglobal.inventory.imports;

import java.util.List;

public record InventoryImportPlan(int created, int updated, int unchanged, int skipped, List<RowPlan> rows) {

    public enum Outcome {
        CREATE, UPDATE, UNCHANGED, SKIP
    }

    /** {@code reason} says why a row was skipped, and is empty otherwise. */
    public record RowPlan(int lineNumber, String name, Outcome outcome, String reason) {
    }
}
