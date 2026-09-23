package org.openelisglobal.inventory.imports;

import java.util.List;

/**
 * What an uploaded catalog file would do, or did.
 *
 * <p>
 * The same shape answers both the preview and the commit, so the screen that
 * shows "what will happen" and the one that shows "what happened" are the same
 * table with a different heading — and any disagreement between the two is
 * visible rather than inferred.
 */
public record InventoryImportPlan(int created, int updated, int unchanged, int skipped, List<RowPlan> rows) {

    /** What one line of the file resolves to. */
    public enum Outcome {
        /** No item matches, so one would be defined. */
        CREATE,
        /** An item matches and at least one field differs. */
        UPDATE,
        /** An item matches and nothing in the row differs from it. */
        UNCHANGED,
        /** The row cannot be used, and {@code reason} says why. */
        SKIP
    }

    /**
     * @param lineNumber the line in the uploaded file, so an operator can find it
     * @param name       the item the row names, for a table that reads like the
     *                   file
     * @param outcome    what this row resolves to
     * @param reason     why a row was skipped; empty otherwise
     */
    public record RowPlan(int lineNumber, String name, Outcome outcome, String reason) {
    }
}
