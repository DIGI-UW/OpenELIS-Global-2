package org.openelisglobal.inventory.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A generic tabular result shared by every inventory report type, so
 * {@link InventoryReportWriter} renders once per export format, not per report.
 */
public class ReportTable {

    private final String title;
    private final List<String> headers;
    private final Set<Integer> numericColumns;
    private final List<List<String>> rows = new ArrayList<>();

    public ReportTable(String title, List<String> headers) {
        this(title, headers, Set.of());
    }

    /**
     * {@code numericColumns} are the indices whose cells hold quantities, so the
     * XLSX writer can type them as numbers while identifiers stay text.
     */
    public ReportTable(String title, List<String> headers, Set<Integer> numericColumns) {
        this.title = title;
        this.headers = headers;
        this.numericColumns = numericColumns;
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

    public boolean isNumericColumn(int column) {
        return numericColumns.contains(column);
    }

    public List<List<String>> getRows() {
        return rows;
    }
}
