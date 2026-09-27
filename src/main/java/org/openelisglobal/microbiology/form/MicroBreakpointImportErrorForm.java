package org.openelisglobal.microbiology.form;

public class MicroBreakpointImportErrorForm {
    public int rowNumber;
    public String message;
    public String sourceRow;

    public MicroBreakpointImportErrorForm() {
    }

    public MicroBreakpointImportErrorForm(int rowNumber, String message, String sourceRow) {
        this.rowNumber = rowNumber;
        this.message = message;
        this.sourceRow = sourceRow;
    }
}
