package org.openelisglobal.labelpreset.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Configurable barcode label preset (FRS §7.1). Surrogate Integer PK per the
 * OGC-284 SampleBarcodeInfo idiom. Per-scope flags + per-scope quantities drive
 * Order Entry placement.
 */
@Entity
@Table(name = "label_preset")
public class LabelPreset extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "label_preset_generator")
    @SequenceGenerator(name = "label_preset_generator", sequenceName = "label_preset_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 120, unique = true)
    private String name;

    @Column(name = "height_mm", nullable = false)
    private Integer heightMm;

    @Column(name = "width_mm", nullable = false)
    private Integer widthMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "barcode_type", nullable = false, length = 20)
    private BarcodeType barcodeType;

    @Column(name = "prints_per_order", nullable = false)
    private Boolean printsPerOrder = false;

    @Column(name = "prints_per_sample", nullable = false)
    private Boolean printsPerSample = true;

    @Column(name = "default_per_order", nullable = false)
    private Integer defaultPerOrder = 0;

    @Column(name = "max_per_order", nullable = false)
    private Integer maxPerOrder = 10;

    @Column(name = "default_per_sample", nullable = false)
    private Integer defaultPerSample = 0;

    @Column(name = "max_per_sample", nullable = false)
    private Integer maxPerSample = 10;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<LabelPresetField> fields = new ArrayList<>();

    public LabelPreset() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(Integer heightMm) {
        this.heightMm = heightMm;
    }

    public Integer getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(Integer widthMm) {
        this.widthMm = widthMm;
    }

    public BarcodeType getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(BarcodeType barcodeType) {
        this.barcodeType = barcodeType;
    }

    public Boolean getPrintsPerOrder() {
        return printsPerOrder;
    }

    public void setPrintsPerOrder(Boolean printsPerOrder) {
        this.printsPerOrder = printsPerOrder;
    }

    public Boolean getPrintsPerSample() {
        return printsPerSample;
    }

    public void setPrintsPerSample(Boolean printsPerSample) {
        this.printsPerSample = printsPerSample;
    }

    public Integer getDefaultPerOrder() {
        return defaultPerOrder;
    }

    public void setDefaultPerOrder(Integer defaultPerOrder) {
        this.defaultPerOrder = defaultPerOrder;
    }

    public Integer getMaxPerOrder() {
        return maxPerOrder;
    }

    public void setMaxPerOrder(Integer maxPerOrder) {
        this.maxPerOrder = maxPerOrder;
    }

    public Integer getDefaultPerSample() {
        return defaultPerSample;
    }

    public void setDefaultPerSample(Integer defaultPerSample) {
        this.defaultPerSample = defaultPerSample;
    }

    public Integer getMaxPerSample() {
        return maxPerSample;
    }

    public void setMaxPerSample(Integer maxPerSample) {
        this.maxPerSample = maxPerSample;
    }

    public Boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(Boolean isSystem) {
        this.isSystem = isSystem;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<LabelPresetField> getFields() {
        return fields;
    }

    public void setFields(List<LabelPresetField> fields) {
        this.fields = fields;
    }
}
