package org.openelisglobal.barcode.form;

public class PrintableLabelOptionForm {

    private String labelType;
    private int quantity;
    private String dimensionsMm;
    private String printUrl;

    public String getLabelType() {
        return labelType;
    }

    public void setLabelType(String labelType) {
        this.labelType = labelType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDimensionsMm() {
        return dimensionsMm;
    }

    public void setDimensionsMm(String dimensionsMm) {
        this.dimensionsMm = dimensionsMm;
    }

    public String getPrintUrl() {
        return printUrl;
    }

    public void setPrintUrl(String printUrl) {
        this.printUrl = printUrl;
    }
}
