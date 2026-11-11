package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Links QC metadata to a SampleItem via a 1:1 relationship.
 *
 * Keeps the core SampleItem entity clean while storing QC-specific fields
 * (type, parent linkage for duplicates, expected value for controls, optional
 * bridge to Westgard QC control lots).
 */
@Entity
@Table(name = "sample_item_qc_profile")
public class SampleItemQcProfile extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotNull
    @Column(name = "sample_item_id", nullable = false, unique = true)
    private Integer sampleItemId;

    @NotNull
    @Column(name = "qc_type", nullable = false, length = 20)
    private String qcType;

    @Column(name = "parent_sample_item_id")
    private Integer parentSampleItemId;

    @Column(name = "expected_value", precision = 15, scale = 5)
    private BigDecimal expectedValue;

    @Column(name = "control_lot_id", length = 36)
    private String controlLotId;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    public SampleItemQcProfile() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getQcType() {
        return qcType;
    }

    public void setQcType(String qcType) {
        this.qcType = qcType;
    }

    public Integer getParentSampleItemId() {
        return parentSampleItemId;
    }

    public void setParentSampleItemId(Integer parentSampleItemId) {
        this.parentSampleItemId = parentSampleItemId;
    }

    public BigDecimal getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(BigDecimal expectedValue) {
        this.expectedValue = expectedValue;
    }

    public String getControlLotId() {
        return controlLotId;
    }

    public void setControlLotId(String controlLotId) {
        this.controlLotId = controlLotId;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
