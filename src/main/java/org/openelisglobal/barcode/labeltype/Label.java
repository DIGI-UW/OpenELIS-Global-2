package org.openelisglobal.barcode.labeltype;

import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import java.util.ArrayList;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.barcode.service.BarcodeLabelInfoService;
import org.openelisglobal.barcode.valueholder.BarcodeLabelInfo;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Stores all the values, layout, and acts as a link to the persisted meta data
 * that is stored in the database. Used in printing bar code labels through the
 * BarcodeLabelMaker class
 *
 * @author Caleb
 */
public abstract class Label {

    // for sizing the bar code area
    static int SMALL_BARCODE = 6; // just over half of the width
    static int MED_BARCODE = 8; // just over 3/4 the width
    static int LARGE_BARCODE = 9; // most of the width

    // default fonts
    private Font valueFont = new Font(FontFamily.HELVETICA, 9, Font.NORMAL);
    private Font nameFont = new Font(FontFamily.HELVETICA, 9, Font.BOLD);

    // default dimension (height width are used only as a ratio)
    // pdfWidth and pdfHeight used for actual pdfScaling
    protected float height = 1;
    protected float width = 3;
    private int margin = 5;
    public float pdfHeight = 350;
    public float pdfWidth;

    // default bar code size
    private int barcodeSpace = LARGE_BARCODE;

    // default number of copies to print
    private int numLabels = 1;

    // holders for values in bar code
    protected ArrayList<LabelField> aboveFields;
    protected ArrayList<LabelField> belowFields;
    private String code;
    private String codeLabel;

    // information stored in/for database
    private BarcodeLabelInfo labelInfo;
    private String sysUserId; // for log tracking
    boolean newInfo; // for deciding if insert or update

    private BarcodeLabelInfoService barcodeLabelService;

    /**
     * Lazy initialization of barcodeLabelService. Initializes on first use to
     * ensure SpringContext is ready.
     * 
     * @return BarcodeLabelInfoService instance
     */
    private BarcodeLabelInfoService getBarcodeLabelService() {
        if (barcodeLabelService == null) {
            try {
                barcodeLabelService = SpringContext.getBean(BarcodeLabelInfoService.class);
            } catch (Exception e) {
                LogEvent.logError("Label", "getBarcodeLabelService",
                        "Failed to get BarcodeLabelInfoService from SpringContext: " + e.getMessage());
                throw new RuntimeException("BarcodeLabelInfoService not available", e);
            }
        }
        return barcodeLabelService;
    }

    /**
     * Gets how many rows will be above the bar code
     *
     * @return The number of rows
     */
    public abstract int getNumTextRowsBefore();

    /**
     * Gets how many rows will be after the bar code
     *
     * @return The number of rows
     */
    public abstract int getNumTextRowsAfter();

    /**
     * Gets the max number of labels that can be printed for the label type
     *
     * @return The max number of labels
     */
    public abstract int getMaxNumLabels();

    /**
     * Get the number of columns bar code takes up
     *
     * @return The number of columns bar code will fill
     */
    public int getScaledBarcodeSpace() {
        return barcodeSpace * 2;
    }

    /**
     * Get font for label field values
     *
     * @return Font for label field values
     */
    public Font getValueFont() {
        return valueFont;
    }

    /**
     * Set font for label field values
     *
     * @param valueFont Font to use for field values
     */
    public void setValueFont(Font valueFont) {
        this.valueFont = valueFont;
    }

    /**
     * Get font for label field names
     *
     * @return font Font to use for field values
     */
    public Font getNameFont() {
        return nameFont;
    }

    /**
     * Set font for label field names
     *
     * @param nameFont Font to use for name values
     */
    public void setNameFont(Font nameFont) {
        this.nameFont = nameFont;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public int getMargin() {
        return margin;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }

    /**
     * Get the unscaled bar code space (6-10)
     *
     * @return The space the bar code uses (half the number of columns)
     */
    public int getBarcodeSpace() {
        return barcodeSpace;
    }

    /**
     * Set unscaled bar code space (6-10)
     *
     * @param barcodeSpace The space the bar code uses (half the number of columns)
     */
    public void setBarcodeSpace(int barcodeSpace) {
        if (SMALL_BARCODE <= barcodeSpace && barcodeSpace <= LARGE_BARCODE) {
            this.barcodeSpace = barcodeSpace;
        }
    }

    /**
     * Get fields that are above the bar code
     *
     * @return List of the fields above the bar code
     */
    public Iterable<LabelField> getAboveFields() {
        return aboveFields;
    }

    /**
     * Set fields above the bar code
     *
     * @param aboveFields List of the fields above the bar code
     */
    public void setAboveFields(ArrayList<LabelField> aboveFields) {
        this.aboveFields = aboveFields;
    }

    /**
     * Get fields that are blow the bar code
     *
     * @return List of the fields below the bar code
     */
    public Iterable<LabelField> getBelowFields() {
        return belowFields;
    }

    /**
     * Set fields below the bar code
     *
     * @param belowFields List of the fields below the bar code
     */
    public void setBelowFields(ArrayList<LabelField> belowFields) {
        this.belowFields = belowFields;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeLabel() {
        return codeLabel == null ? code : codeLabel;
    }

    public void setCodeLabel(String codeLabel) {
        this.codeLabel = codeLabel;
    }

    /**
     * Get number of labels to attempt to print
     *
     * @return number of labels to attempt to print
     */
    public int getNumLabels() {
        return numLabels;
    }

    /**
     * Set number of labels to attempt to print
     *
     * @param numLabels to attempt to print
     */
    public void setNumLabels(int numLabels) {
        if (numLabels > 0) {
            this.numLabels = numLabels;
        }
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        if (labelInfo != null) {
            labelInfo.setSysUserId(sysUserId);
        }
        this.sysUserId = sysUserId;
    }

    /**
     * Get the number of rows a list of fields will take up This is used to shrink
     * the bar code vertical space inversely to the number of rows
     *
     * @param fields List of LabelFields to check how many rows they take up
     * @return the number of rows to the fields take up
     */
    protected int getNumRows(Iterable<LabelField> fields) {
        int numRows = 0;
        int curColumns = 0;
        boolean completeRow = true;
        for (LabelField field : fields) {
            // add to num row if start on newline
            if (field.isStartNewline() && !completeRow) {
                ++numRows;
                curColumns = 0;
            }
            curColumns += field.getColspan();
            if (curColumns > 20) {
                // throw error
                // row is completed, add to num row
            } else if (curColumns == 20) {
                completeRow = true;
                curColumns = 0;
                ++numRows;
            } else {
                completeRow = false;
            }
        }
        // add to num row if last row was incomplete
        if (!completeRow) {
            ++numRows;
        }

        return numRows;
    }

    /**
     * Check if the bar code is printable at least once (has not reached the maximum
     * printed)
     *
     * @return If the bar code can still be printed
     */
    public boolean checkIfPrintable() {
        boolean printable = true;
        if (labelInfo != null && labelInfo.getNumPrinted() >= getMaxNumLabels()) {
            printable = false;
        }

        return printable;
    }

    /**
     * Link the meta data stored in the database to the label if it exists
     * Otherwise, create new meta data for the label
     */
    public void linkBarcodeLabelInfo() {
        try {
            if (code == null || code.trim().isEmpty()) {
                // If code is not set, create a new label info without database lookup
                labelInfo = new BarcodeLabelInfo("");
                return;
            }
            BarcodeLabelInfoService service = getBarcodeLabelService();
            if (service != null) {
                labelInfo = service.getDataByCode(code);
            }
            if (labelInfo == null) {
                labelInfo = new BarcodeLabelInfo(code);
            }
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            // Create new label info if database lookup fails
            if (labelInfo == null) {
                labelInfo = new BarcodeLabelInfo(code != null ? code : "");
            }
        } catch (Exception e) {
            LogEvent.logError("Label", "linkBarcodeLabelInfo", "Failed to link barcode label info: " + e.getMessage());
            // Create new label info if service is not available
            if (labelInfo == null) {
                labelInfo = new BarcodeLabelInfo(code != null ? code : "");
            }
        }
    }

    public BarcodeLabelInfo getLabelInfo() {
        return labelInfo;
    }

    public void incrementNumPrinted() {
        if (labelInfo != null) {
            labelInfo.incrementNumPrinted();
        }
    }
}
