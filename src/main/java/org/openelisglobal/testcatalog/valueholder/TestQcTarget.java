package org.openelisglobal.testcatalog.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1148 — the QC target a test's control level is judged against: an
 * expected value with a tolerance (quantitative) or an expected dictionary
 * outcome (qualitative), either as the level default ({@code qcControlLotId}
 * null) or as an override for one control lot. Entered once in the Test
 * Catalog; Results Entry control capture prefills from the effective target
 * (lot override, else level default). Deactivated, never deleted.
 */
@Entity
@Table(name = "test_qc_target", schema = "clinlims")
public class TestQcTarget extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public static final String LEVEL_LOW = "LOW";
    public static final String LEVEL_NORMAL = "NORMAL";
    public static final String LEVEL_HIGH = "HIGH";

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "component_id", length = 36)
    private String componentId;

    @Column(name = "control_level", nullable = false, length = 10)
    private String controlLevel;

    @Column(name = "qc_control_lot_id", length = 36)
    private String qcControlLotId;

    @Column(name = "expected_value", precision = 15, scale = 5)
    private BigDecimal expectedValue;

    @Column(name = "uncertainty", precision = 15, scale = 5)
    private BigDecimal uncertainty;

    @Column(name = "expected_dict_result_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String expectedDictResultId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // FR-D2: who last changed the target; BaseObject's sysUserId is not a column.
    @Column(name = "sys_user_id", precision = 10, scale = 0)
    private Integer systemUserId;

    public TestQcTarget() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getControlLevel() {
        return controlLevel;
    }

    public void setControlLevel(String controlLevel) {
        this.controlLevel = controlLevel;
    }

    public String getQcControlLotId() {
        return qcControlLotId;
    }

    public void setQcControlLotId(String qcControlLotId) {
        this.qcControlLotId = qcControlLotId;
    }

    public BigDecimal getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(BigDecimal expectedValue) {
        this.expectedValue = expectedValue;
    }

    public BigDecimal getUncertainty() {
        return uncertainty;
    }

    public void setUncertainty(BigDecimal uncertainty) {
        this.uncertainty = uncertainty;
    }

    public String getExpectedDictResultId() {
        return expectedDictResultId;
    }

    public void setExpectedDictResultId(String expectedDictResultId) {
        this.expectedDictResultId = expectedDictResultId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    /** Also records the user on the persisted audit column. */
    @Override
    public void setSysUserId(String sysUserId) {
        super.setSysUserId(sysUserId);
        try {
            this.systemUserId = sysUserId == null || sysUserId.isBlank() ? null : Integer.valueOf(sysUserId.trim());
        } catch (NumberFormatException e) {
            this.systemUserId = null;
        }
    }
}
