package org.openelisglobal.labelpreset.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Persisted per-order / per-sample label request with a frozen JSONB snapshot
 * of the preset at save time (FRS §7.3). The snapshot is the authoritative
 * source for reprint (AC-20) — reprint MUST NOT re-fetch label_preset or
 * test_label_preset_link.
 *
 * <p>
 * JSONB binding uses the repo's Hibernate-5.6 idiom (class-level
 * {@link TypeDef} + field-level {@link Type}), matching Alert /
 * PatientMergeAudit. Do NOT use Hibernate-6 {@code @JdbcTypeCode}.
 */
@Entity
@Table(name = "order_label_request")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class OrderLabelRequest extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_label_request_generator")
    @SequenceGenerator(name = "order_label_request_generator", sequenceName = "order_label_request_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_sample_id", referencedColumnName = "id", nullable = false)
    private Sample parentSample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", referencedColumnName = "id", nullable = true)
    private SampleItem sampleItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", referencedColumnName = "id", nullable = false)
    private LabelPreset preset;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Type(type = "jsonb")
    @Column(name = "preset_snapshot", nullable = false, columnDefinition = "jsonb")
    private PresetSnapshotDto presetSnapshot;

    public OrderLabelRequest() {
        super();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Sample getParentSample() {
        return parentSample;
    }

    public void setParentSample(Sample parentSample) {
        this.parentSample = parentSample;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public LabelPreset getPreset() {
        return preset;
    }

    public void setPreset(LabelPreset preset) {
        this.preset = preset;
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
