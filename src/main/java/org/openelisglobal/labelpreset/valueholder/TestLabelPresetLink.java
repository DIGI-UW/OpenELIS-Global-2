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
import jakarta.persistence.UniqueConstraint;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.Test;

/**
 * Links a test to a label preset with per-sample quantity overrides (FRS §3.5).
 * Per-sample-only: service layer enforces that the linked preset has
 * prints_per_sample = true (no DDL CHECK because PostgreSQL CHECK cannot
 * reference other tables).
 */
@Entity
@Table(name = "test_label_preset_link", uniqueConstraints = {
        @UniqueConstraint(name = "test_label_preset_link_uniq", columnNames = { "test_id", "preset_id" }) })
public class TestLabelPresetLink extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_label_preset_link_generator")
    @SequenceGenerator(name = "test_label_preset_link_generator", sequenceName = "test_label_preset_link_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", referencedColumnName = "id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", referencedColumnName = "id", nullable = false)
    private LabelPreset preset;

    @Column(name = "default_qty", nullable = false)
    private Integer defaultQty;

    @Column(name = "max_qty", nullable = false)
    private Integer maxQty;

    @Column(name = "allow_override", nullable = false)
    private Boolean allowOverride = true;

    public TestLabelPresetLink() {
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

    public Test getTest() {
        return test;
    }

    public void setTest(Test test) {
        this.test = test;
    }

    public LabelPreset getPreset() {
        return preset;
    }

    public void setPreset(LabelPreset preset) {
        this.preset = preset;
    }

    public Integer getDefaultQty() {
        return defaultQty;
    }

    public void setDefaultQty(Integer defaultQty) {
        this.defaultQty = defaultQty;
    }

    public Integer getMaxQty() {
        return maxQty;
    }

    public void setMaxQty(Integer maxQty) {
        this.maxQty = maxQty;
    }

    public Boolean getAllowOverride() {
        return allowOverride;
    }

    public void setAllowOverride(Boolean allowOverride) {
        this.allowOverride = allowOverride;
    }
}
