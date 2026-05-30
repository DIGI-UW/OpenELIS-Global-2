package org.openelisglobal.labelpreset.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.openelisglobal.common.valueholder.BaseObject;

/** Ordered content field within a label preset (FRS §7.1). */
@Entity
@Table(name = "label_preset_field", uniqueConstraints = {
        @UniqueConstraint(name = "label_preset_field_order_uniq", columnNames = { "preset_id", "display_order" }),
        @UniqueConstraint(name = "label_preset_field_key_uniq", columnNames = { "preset_id", "field_key" }) })
public class LabelPresetField extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "label_preset_field_generator")
    @SequenceGenerator(name = "label_preset_field_generator", sequenceName = "label_preset_field_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", referencedColumnName = "id", nullable = false)
    private LabelPreset preset;

    @Column(name = "field_key", nullable = false, length = 60)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private FieldSourceType sourceType = FieldSourceType.SYSTEM;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public LabelPresetField() {
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

    public LabelPreset getPreset() {
        return preset;
    }

    public void setPreset(LabelPreset preset) {
        this.preset = preset;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public FieldSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(FieldSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
