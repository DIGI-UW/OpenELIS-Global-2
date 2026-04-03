package org.openelisglobal.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing the complete result set of a report.
 * <p>
 * This class is designed to be serialized to JSON for consumption by the UI or
 * external API clients. It contains metadata about the columns and the list of
 * data rows.
 */
public class ReportingData implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * List of column definitions (headers, types).
     */
    private List<ReportColumn> columns = new ArrayList<>();

    /**
     * List of data rows.
     */
    private List<ReportRow> rows = new ArrayList<>();

    /**
     * Optional message or status about the report execution.
     */
    private String message;

    public ReportingData() {
    }

    public List<ReportColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<ReportColumn> columns) {
        this.columns = columns;
    }

    public void addColumn(ReportColumn column) {
        this.columns.add(column);
    }

    public List<ReportRow> getRows() {
        return rows;
    }

    public void setRows(List<ReportRow> rows) {
        this.rows = rows;
    }

    public void addRow(ReportRow row) {
        this.rows.add(row);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
