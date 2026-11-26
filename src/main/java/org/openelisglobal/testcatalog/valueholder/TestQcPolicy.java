package org.openelisglobal.testcatalog.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1200 FR-H1: what each analyzer run of a test must carry. A control may be
 * required per run; when it is and none is recorded, the run warns or holds the
 * test's rows. A failed control always holds the test (FR-C4). A test without a
 * policy requires nothing.
 */
@Entity
@Table(name = "test_qc_policy", schema = "clinlims")
public class TestQcPolicy extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    /** What a run does when a required control is missing. */
    public enum WhenMissing {
        WARN, HOLD
    }

    @Id
    @Column(name = "test_id")
    private Long id;

    @Column(name = "control_required", nullable = false)
    private boolean controlRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_when_missing", nullable = false, length = 10)
    private WhenMissing controlWhenMissing = WhenMissing.WARN;

    /** Who last changed the policy. */
    @Column(name = "sys_user_id", precision = 10, scale = 0)
    private Integer systemUserId;

    /** The policy of a test that has none stored: nothing required. */
    public static TestQcPolicy none(String testId) {
        TestQcPolicy policy = new TestQcPolicy();
        policy.setId(Long.valueOf(testId));
        return policy;
    }

    /** A required control that is missing holds the test's rows. */
    public boolean holdsWhenControlMissing() {
        return controlRequired && controlWhenMissing == WhenMissing.HOLD;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public boolean isControlRequired() {
        return controlRequired;
    }

    public void setControlRequired(boolean controlRequired) {
        this.controlRequired = controlRequired;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    public WhenMissing getControlWhenMissing() {
        return controlWhenMissing;
    }

    public void setControlWhenMissing(WhenMissing controlWhenMissing) {
        this.controlWhenMissing = controlWhenMissing;
    }
}
