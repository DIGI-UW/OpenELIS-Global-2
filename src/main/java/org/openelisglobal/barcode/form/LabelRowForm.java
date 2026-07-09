package org.openelisglobal.barcode.form;

import java.util.List;
import java.util.Map;

public class LabelRowForm {

    private String rowType;
    private String rowId;
    private String sampleRef;
    private List<String> applicableLabelTypes;
    private Map<String, Integer> quantities;
    private int rowTotal;

    public String getRowType() {
        return rowType;
    }

    public void setRowType(String rowType) {
        this.rowType = rowType;
    }

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public String getSampleRef() {
        return sampleRef;
    }

    public void setSampleRef(String sampleRef) {
        this.sampleRef = sampleRef;
    }

    public List<String> getApplicableLabelTypes() {
        return applicableLabelTypes;
    }

    public void setApplicableLabelTypes(List<String> applicableLabelTypes) {
        this.applicableLabelTypes = applicableLabelTypes;
    }

    public Map<String, Integer> getQuantities() {
        return quantities;
    }

    public void setQuantities(Map<String, Integer> quantities) {
        this.quantities = quantities;
    }

    public int getRowTotal() {
        return rowTotal;
    }

    public void setRowTotal(int rowTotal) {
        this.rowTotal = rowTotal;
    }
}
