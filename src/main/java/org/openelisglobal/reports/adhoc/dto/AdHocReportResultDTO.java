package org.openelisglobal.reports.adhoc.dto;

import java.util.ArrayList;
import java.util.List;

public class AdHocReportResultDTO {

    private List<ColumnDefinition> columns = new ArrayList<>();
    private List<List<Object>> rows = new ArrayList<>();
    private long totalCount;
    private int returnedCount;
    private boolean hasMore;

    public AdHocReportResultDTO() {
    }

    public List<ColumnDefinition> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnDefinition> columns) {
        this.columns = columns != null ? columns : new ArrayList<>();
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public void setRows(List<List<Object>> rows) {
        this.rows = rows != null ? rows : new ArrayList<>();
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public int getReturnedCount() {
        return returnedCount;
    }

    public void setReturnedCount(int returnedCount) {
        this.returnedCount = returnedCount;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public void addColumn(String fieldId, String displayName, ReportFieldDTO.DataType dataType) {
        columns.add(new ColumnDefinition(fieldId, displayName, dataType));
    }

    public void addRow(List<Object> rowData) {
        rows.add(rowData);
    }

    public static class ColumnDefinition {
        private String fieldId;
        private String displayName;
        private ReportFieldDTO.DataType dataType;

        public ColumnDefinition() {
        }

        public ColumnDefinition(String fieldId, String displayName, ReportFieldDTO.DataType dataType) {
            this.fieldId = fieldId;
            this.displayName = displayName;
            this.dataType = dataType;
        }

        public String getFieldId() {
            return fieldId;
        }

        public void setFieldId(String fieldId) {
            this.fieldId = fieldId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public ReportFieldDTO.DataType getDataType() {
            return dataType;
        }

        public void setDataType(ReportFieldDTO.DataType dataType) {
            this.dataType = dataType;
        }
    }

    @Override
    public String toString() {
        return "AdHocReportResultDTO{columns=" + columns.size() + ", rows=" + rows.size() + ", totalCount=" + totalCount
                + ", returnedCount=" + returnedCount + ", hasMore=" + hasMore + "}";
    }
}
