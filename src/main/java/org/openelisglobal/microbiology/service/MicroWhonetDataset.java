package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;

public class MicroWhonetDataset {

    private final MicroWhonetPreviewForm preview;
    private final List<WHONetRow> rows;

    public MicroWhonetDataset(MicroWhonetPreviewForm preview, List<WHONetRow> rows) {
        this.preview = preview;
        this.rows = List.copyOf(rows);
    }

    public MicroWhonetPreviewForm getPreview() {
        return preview;
    }

    public List<WHONetRow> getRows() {
        return rows;
    }
}
