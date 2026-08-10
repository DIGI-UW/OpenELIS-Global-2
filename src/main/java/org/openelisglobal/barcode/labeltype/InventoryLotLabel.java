package org.openelisglobal.barcode.labeltype;

import java.util.ArrayList;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.internationalization.MessageUtil;

/**
 * Label for an inventory lot. Carries the lot's internal barcode, with the item
 * name and lot number above it and the expiry below, so a technician can read
 * the container without scanning it.
 *
 * Dimensions are fixed rather than configuration-driven: storage location
 * labels expose width/height as admin properties, but lots have no such
 * property yet and the 3x1 inch default is the same stock those use.
 */
public class InventoryLotLabel extends Label {

    private static final int MAX_COLUMNS = 20;

    /**
     * @param itemName    Catalog item name, e.g. "Test Reagent A"
     * @param lotNumber   The lot number as printed by the manufacturer, or
     *                    generated on receipt
     * @param expiryDate  Preformatted expiry date, blank when the lot has none
     * @param barcodeCode The lot's internal barcode — the value that is encoded
     */
    public InventoryLotLabel(String itemName, String lotNumber, String expiryDate, String barcodeCode) {
        width = 3.0f;
        height = 1.0f;

        aboveFields = new ArrayList<>();
        belowFields = new ArrayList<>();

        // Single-arg getMessage: the (key, String) overload treats its second
        // argument as a substitution parameter, not a default, so a missing key
        // silently prints the key itself on the label.
        LabelField nameField = new LabelField(MessageUtil.getMessage("barcode.label.info.itemName"),
                itemName != null ? itemName : "", 12);
        nameField.setDisplayFieldName(true);
        nameField.setUnderline(true);
        aboveFields.add(nameField);

        LabelField lotField = new LabelField(MessageUtil.getMessage("barcode.label.info.lotNumber"),
                lotNumber != null ? lotNumber : "", 8);
        lotField.setDisplayFieldName(true);
        aboveFields.add(lotField);

        LabelField expiryField = new LabelField(MessageUtil.getMessage("barcode.label.info.expiryDate"),
                expiryDate != null ? expiryDate : "", 8);
        expiryField.setDisplayFieldName(true);
        belowFields.add(expiryField);

        String barcodeValue = barcodeCode != null ? barcodeCode.trim() : "";
        setCode(barcodeValue);
        setCodeLabel(barcodeValue);
    }

    @Override
    public int getNumTextRowsBefore() {
        return countRows(aboveFields);
    }

    @Override
    public int getNumTextRowsAfter() {
        return countRows(belowFields);
    }

    private int countRows(java.util.List<LabelField> fields) {
        int numRows = 0;
        int curColumns = 0;
        boolean completeRow = true;

        for (LabelField field : fields) {
            if (field.isStartNewline() && !completeRow) {
                ++numRows;
                curColumns = 0;
            }
            curColumns += field.getColspan();
            if (curColumns == MAX_COLUMNS) {
                completeRow = true;
                curColumns = 0;
                ++numRows;
            } else if (curColumns < MAX_COLUMNS) {
                completeRow = false;
            }
        }

        if (!completeRow) {
            ++numRows;
        }

        return numRows;
    }

    @Override
    public int getMaxNumLabels() {
        // A lot can be split across many containers, so there is no sensible cap
        // on how many labels a lab may print for one lot.
        return Integer.MAX_VALUE;
    }
}
