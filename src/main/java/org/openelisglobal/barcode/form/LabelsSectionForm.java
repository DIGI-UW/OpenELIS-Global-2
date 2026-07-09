package org.openelisglobal.barcode.form;

import java.util.ArrayList;
import java.util.List;

public class LabelsSectionForm {

    private LabelRowForm orderRow;
    private List<LabelRowForm> sampleRows = new ArrayList<>();
    private int runningTotal;

    public LabelRowForm getOrderRow() {
        return orderRow;
    }

    public void setOrderRow(LabelRowForm orderRow) {
        this.orderRow = orderRow;
    }

    public List<LabelRowForm> getSampleRows() {
        return sampleRows;
    }

    public void setSampleRows(List<LabelRowForm> sampleRows) {
        this.sampleRows = sampleRows;
    }

    public int getRunningTotal() {
        return runningTotal;
    }

    public void setRunningTotal(int runningTotal) {
        this.runningTotal = runningTotal;
    }
}
