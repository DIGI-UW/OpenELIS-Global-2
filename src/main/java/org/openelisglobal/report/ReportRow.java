package org.openelisglobal.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object representing a single row of data in a report.
 */
public class ReportRow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Ordered list of cell values corresponding to the columns.
     */
    private List<Object> cells = new ArrayList<>();

    /**
     * Optional map representation for easier lookup by column key.
     */
    private Map<String, Object> dataMap = new HashMap<>();

    public ReportRow() {
    }

    public List<Object> getCells() {
        return cells;
    }

    public void setCells(List<Object> cells) {
        this.cells = cells;
    }

    public void addCell(Object cellValue) {
        this.cells.add(cellValue);
    }

    public Map<String, Object> getDataMap() {
        return dataMap;
    }

    public void setDataMap(Map<String, Object> dataMap) {
        this.dataMap = dataMap;
    }

    public void addData(String key, Object value) {
        this.dataMap.put(key, value);
    }
}
