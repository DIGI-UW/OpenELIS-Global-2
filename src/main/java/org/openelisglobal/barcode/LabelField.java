package org.openelisglobal.barcode;

/**
 * Stores information about a field that appears on a bar code label examples of
 * how it appears on labels: name: value (displayFieldName = true) value
 * (displayFieldName = false)
 *
 * @author Caleb
 */
public class LabelField {

    private String name;
    private String value;
    private int colspan = 5;
    private boolean startNewline = false; // should field start on newline
    private boolean displayFieldName = false; // should field name be displayed
    private boolean underline = false; // should there be an underline

    public LabelField(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public LabelField(String name, String value, int colspan) {
        this.name = name;
        this.value = value;
        this.colspan = colspan;
    }

    public LabelField(int colspan) {
        this.colspan = colspan;
    }

    public boolean isStartNewline() {
        return startNewline;
    }

    public void setStartNewline(boolean startNewline) {
        this.startNewline = startNewline;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getColspan() {
        return colspan;
    }

    public void setColspan(int colspan) {
        this.colspan = colspan;
    }

    public boolean isDisplayFieldName() {
        return displayFieldName;
    }

    public void setDisplayFieldName(boolean displayFieldName) {
        this.displayFieldName = displayFieldName;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }
}
