package org.openelisglobal.labelpreset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Response for {@code POST /api/orderEntry/labelRequest} (OGC-285 M5). The
 * dynamic two-table column set ({@code order_columns} / {@code sample_columns})
 * plus the resolved per-cell defaults ({@code order_row} /
 * {@code sample_rows}). Mirrors {@code OrderEntryLabelRequestResponse} in
 * {@code contracts/openapi.yaml} §8.1.
 */
public class OrderEntryLabelRequestResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("order_columns")
    private List<LabelColumn> orderColumns = new ArrayList<>();

    @JsonProperty("sample_columns")
    private List<LabelColumn> sampleColumns = new ArrayList<>();

    @JsonProperty("order_row")
    private OrderRow orderRow = new OrderRow();

    @JsonProperty("sample_rows")
    private List<SampleRow> sampleRows = new ArrayList<>();

    public OrderEntryLabelRequestResponse() {
    }

    public List<LabelColumn> getOrderColumns() {
        return orderColumns;
    }

    public void setOrderColumns(List<LabelColumn> orderColumns) {
        this.orderColumns = orderColumns;
    }

    public List<LabelColumn> getSampleColumns() {
        return sampleColumns;
    }

    public void setSampleColumns(List<LabelColumn> sampleColumns) {
        this.sampleColumns = sampleColumns;
    }

    public OrderRow getOrderRow() {
        return orderRow;
    }

    public void setOrderRow(OrderRow orderRow) {
        this.orderRow = orderRow;
    }

    public List<SampleRow> getSampleRows() {
        return sampleRows;
    }

    public void setSampleRows(List<SampleRow> sampleRows) {
        this.sampleRows = sampleRows;
    }

    /** The single per-order row of cells (one cell per per-order preset). */
    public static class OrderRow implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("cells")
        private List<LabelCell> cells = new ArrayList<>();

        public OrderRow() {
        }

        public List<LabelCell> getCells() {
            return cells;
        }

        public void setCells(List<LabelCell> cells) {
            this.cells = cells;
        }
    }

    /** One per-sample row of cells, keyed by the client-supplied local id. */
    public static class SampleRow implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("sample_id_local")
        private String sampleIdLocal;

        @JsonProperty("cells")
        private List<LabelCell> cells = new ArrayList<>();

        public SampleRow() {
        }

        public SampleRow(String sampleIdLocal) {
            this.sampleIdLocal = sampleIdLocal;
        }

        public String getSampleIdLocal() {
            return sampleIdLocal;
        }

        public void setSampleIdLocal(String sampleIdLocal) {
            this.sampleIdLocal = sampleIdLocal;
        }

        public List<LabelCell> getCells() {
            return cells;
        }

        public void setCells(List<LabelCell> cells) {
            this.cells = cells;
        }
    }
}
