package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Per-test QC acceptance thresholds for environmental/vector QC evaluation.
 *
 * 1:1 companion to the Test entity (same pattern as SampleItemQcProfile for
 * SampleItem). Keeps QC-specific configuration out of the shared Test entity.
 */
@Entity
@Table(name = "test_qc_threshold")
public class TestQcThreshold extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotNull
    @Column(name = "test_id", nullable = false, unique = true)
    private Integer testId;

    @Column(name = "blank_threshold", precision = 15, scale = 5)
    private BigDecimal blankThreshold;

    @Column(name = "rpd_threshold", precision = 15, scale = 5)
    private BigDecimal rpdThreshold;

    @Column(name = "recovery_window_pct", precision = 15, scale = 5)
    private BigDecimal recoveryWindowPct;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    public TestQcThreshold() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public BigDecimal getBlankThreshold() {
        return blankThreshold;
    }

    public void setBlankThreshold(BigDecimal blankThreshold) {
        this.blankThreshold = blankThreshold;
    }

    public BigDecimal getRpdThreshold() {
        return rpdThreshold;
    }

    public void setRpdThreshold(BigDecimal rpdThreshold) {
        this.rpdThreshold = rpdThreshold;
    }

    public BigDecimal getRecoveryWindowPct() {
        return recoveryWindowPct;
    }

    public void setRecoveryWindowPct(BigDecimal recoveryWindowPct) {
        this.recoveryWindowPct = recoveryWindowPct;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
