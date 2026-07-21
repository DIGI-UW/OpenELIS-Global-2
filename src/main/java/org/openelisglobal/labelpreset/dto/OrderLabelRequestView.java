package org.openelisglobal.labelpreset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;

/**
 * Read-only projection of an {@code order_label_request} row for the GET
 * {@code /api/orders/{id}/labels} endpoint (OGC-285 M6).
 *
 * <p>
 * Built <em>inside</em> the read-only service transaction so the LAZY
 * {@code parentSample} / {@code sampleItem} / {@code preset} associations are
 * resolved to plain ids before the (non-transactional) controller serializes
 * the response — serializing the entity directly would raise
 * {@code LazyInitializationException}. The {@code presetSnapshot} is an eager
 * POJO and is exposed verbatim so callers see exactly the frozen JSON used for
 * reprint.
 */
public class OrderLabelRequestView {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("parent_sample_id")
    private String parentSampleId;

    @JsonProperty("sample_item_id")
    private String sampleItemId;

    @JsonProperty("preset_id")
    private Integer presetId;

    @JsonProperty("qty")
    private Integer qty;

    @JsonProperty("preset_snapshot")
    private PresetSnapshotDto presetSnapshot;

    public OrderLabelRequestView() {
    }

    /** Resolve LAZY ids while still inside the service transaction. */
    public static OrderLabelRequestView from(OrderLabelRequest row) {
        OrderLabelRequestView view = new OrderLabelRequestView();
        view.id = row.getId();
        view.parentSampleId = row.getParentSample() == null ? null : row.getParentSample().getId();
        view.sampleItemId = row.getSampleItem() == null ? null : row.getSampleItem().getId();
        view.presetId = row.getPreset() == null ? null : row.getPreset().getId();
        view.qty = row.getQty();
        view.presetSnapshot = row.getPresetSnapshot();
        return view;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getParentSampleId() {
        return parentSampleId;
    }

    public void setParentSampleId(String parentSampleId) {
        this.parentSampleId = parentSampleId;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
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

    public PresetSnapshotDto getPresetSnapshot() {
        return presetSnapshot;
    }

    public void setPresetSnapshot(PresetSnapshotDto presetSnapshot) {
        this.presetSnapshot = presetSnapshot;
    }
}
