package org.openelisglobal.labelpreset.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.test.valueholder.Test;

/**
 * Per-test master label toggle (FRS §7.2). Surrogate Integer PK + UNIQUE FK to
 * the legacy Test entity expresses the 1:1 association (matches the
 * SampleBarcodeInfo idiom). One row per test, lazily created on first save of
 * the test's Labels tab.
 */
@Entity
@Table(name = "test_label_config")
public class TestLabelConfig extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_label_config_generator")
    @SequenceGenerator(name = "test_label_config_generator", sequenceName = "test_label_config_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", referencedColumnName = "id", unique = true, nullable = false)
    private Test test;

    @Column(name = "allow_order_entry_override", nullable = false)
    private Boolean allowOrderEntryOverride = true;

    public TestLabelConfig() {
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

    public Boolean getAllowOrderEntryOverride() {
        return allowOrderEntryOverride;
    }

    public void setAllowOrderEntryOverride(Boolean allowOrderEntryOverride) {
        this.allowOrderEntryOverride = allowOrderEntryOverride;
    }
}
