package org.openelisglobal.barcode.form;

import java.util.ArrayList;
import java.util.List;

public class PostSavePrintDialogForm {

    private String accessionNumber;
    private List<PrintableLabelOptionForm> printableLabelTypes = new ArrayList<>();
    private boolean allowSkipPrintLater = true;
    private String reprintContextToken;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public List<PrintableLabelOptionForm> getPrintableLabelTypes() {
        return printableLabelTypes;
    }

    public void setPrintableLabelTypes(List<PrintableLabelOptionForm> printableLabelTypes) {
        this.printableLabelTypes = printableLabelTypes;
    }

    public boolean isAllowSkipPrintLater() {
        return allowSkipPrintLater;
    }

    public void setAllowSkipPrintLater(boolean allowSkipPrintLater) {
        this.allowSkipPrintLater = allowSkipPrintLater;
    }

    public String getReprintContextToken() {
        return reprintContextToken;
    }

    public void setReprintContextToken(String reprintContextToken) {
        this.reprintContextToken = reprintContextToken;
    }
}
