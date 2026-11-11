package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportSelection;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;

public class MicroWhonetDataset {

    private final MicroWhonetPreviewForm preview;
    private final List<WHONetRow> rows;
    private final MicroWhonetExportSelection populationSelection;

    public MicroWhonetDataset(MicroWhonetPreviewForm preview, List<WHONetRow> rows,
            MicroWhonetExportSelection populationSelection) {
        this.preview = preview;
        this.rows = List.copyOf(rows);
        this.populationSelection = populationSelection;
    }

    public MicroWhonetPreviewForm getPreview() {
        return preview;
    }

    public List<WHONetRow> getRows() {
        return rows;
    }

    public MicroWhonetExportSelection getPopulationSelection() {
        return populationSelection;
    }
}
