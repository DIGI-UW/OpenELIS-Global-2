package org.openelisglobal.labelpreset.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Save-time payload of the technician's chosen label quantities for an order
 * (OGC-285 M5, T141). This is the persist counterpart to the read-only
 * {@link OrderEntryLabelRequestResponse} — it carries the <em>chosen</em>
 * quantities (the response carries only computed defaults).
 *
 * <p>
 * Shaped to mirror the aggregation response so the M5b Order Entry frontend can
 * round-trip the user's edits straight back: a per-order cell list plus
 * per-sample cell lists keyed by the client-supplied {@code sample_id_local}.
 * The save service resolves each {@code sample_id_local} to the persisted
 * {@code SampleItem} via the caller-supplied id map.
 *
 * <p>
 * No save/persist schema exists in {@code contracts/openapi.yaml} yet (only the
 * aggregation request) — this DTO defines the minimal shape; the openapi
 * contract is extended when the M5b live hook lands.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderLabelPersistRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Chosen quantities for the per-order presets (one cell per per-order preset).
     */
    @JsonProperty("order_cells")
    private List<PersistCell> orderCells = new ArrayList<>();

    /** Chosen quantities per sample, keyed by client-supplied local id. */
    @JsonProperty("sample_rows")
    private List<PersistSampleRow> sampleRows = new ArrayList<>();

    public OrderLabelPersistRequest() {
    }

    public List<PersistCell> getOrderCells() {
        return orderCells;
    }

    public void setOrderCells(List<PersistCell> orderCells) {
        this.orderCells = orderCells;
    }

    public List<PersistSampleRow> getSampleRows() {
        return sampleRows;
    }

    public void setSampleRows(List<PersistSampleRow> sampleRows) {
        this.sampleRows = sampleRows;
    }

    /** A chosen (preset, qty) pair. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PersistCell implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("preset_id")
        private Integer presetId;

        @JsonProperty("qty")
        private Integer qty;

        public PersistCell() {
        }

        public PersistCell(Integer presetId, Integer qty) {
            this.presetId = presetId;
            this.qty = qty;
        }

        public Integer getPresetId() {
            return presetId;
        }

        public void setPresetId(Integer presetId) {
            this.presetId = presetId;
        }

        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
        }
    }

    /** Chosen per-sample cells for one client-supplied local sample id. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PersistSampleRow implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("sample_id_local")
        private String sampleIdLocal;

        @JsonProperty("cells")
        private List<PersistCell> cells = new ArrayList<>();

        public PersistSampleRow() {
        }

        public PersistSampleRow(String sampleIdLocal) {
            this.sampleIdLocal = sampleIdLocal;
        }

        public String getSampleIdLocal() {
            return sampleIdLocal;
        }

        public void setSampleIdLocal(String sampleIdLocal) {
            this.sampleIdLocal = sampleIdLocal;
        }

        public List<PersistCell> getCells() {
            return cells;
        }

        public void setCells(List<PersistCell> cells) {
            this.cells = cells;
        }
    }
}
