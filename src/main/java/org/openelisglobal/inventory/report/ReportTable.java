package org.openelisglobal.inventory.report;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic tabular result shared by every inventory report type, so
 * {@link InventoryReportWriter} renders once per export format, not per report.
 */
public class ReportTable {

    private final String title;
    private final List<String> headers;
    private final List<List<String>> rows = new ArrayList<>();

    public ReportTable(String title, List<String> headers) {
        this.title = title;
        this.headers = headers;
    }

    public void addRow(List<String> row) {
        rows.add(row);
    }

    public String getTitle() {
        return title;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<List<String>> getRows() {
        return rows;
    }
}
