package org.openelisglobal.inventory.imports;

/**
 * Defining a catalogue from a CSV file, for a deployment that starts with an
 * empty one.
 *
 * <p>
 * Preview and apply take the same file and run the same evaluation; apply is
 * preview plus the writes. They cannot disagree about what a row means, which
 * is the property that makes a preview worth showing. The one thing apply can
 * report differently is a row the database changed underneath it between the
 * two calls, and that shows up as a different outcome rather than silently.
 */
public interface InventoryImportService {

    /** The header row and one worked example, for an operator to fill in. */
    String template();

    /** What the file would do. Writes nothing. */
    InventoryImportPlan preview(String csv);

    /** What the file did. */
    InventoryImportPlan apply(String csv, String sysUserId);
}
