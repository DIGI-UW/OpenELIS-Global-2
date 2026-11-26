package org.openelisglobal.testcatalog.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1200 FR-H1: what each run of a test must carry. A control, a blank and a
 * duplicate may each be required per run; when one is and none is recorded, the
 * run warns or holds the test's rows. A failed control always holds the test
 * (FR-C4); a failed blank or duplicate holds it or only warns (FR-C12). A test
 * without a policy requires nothing.
 */
@Entity
@Table(name = "test_qc_policy", schema = "clinlims")
public class TestQcPolicy extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * What a run does when a required item is missing, or a blank or duplicate
     * fails.
     */
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

    @Column(name = "blank_required", nullable = false)
    private boolean blankRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "blank_when_missing", nullable = false, length = 10)
    private WhenMissing blankWhenMissing = WhenMissing.WARN;

    @Enumerated(EnumType.STRING)
    @Column(name = "blank_when_fails", nullable = false, length = 10)
    private WhenMissing blankWhenFails = WhenMissing.HOLD;

    @Column(name = "duplicate_required", nullable = false)
    private boolean duplicateRequired;

    @Enumerated(EnumType.STRING)
    @Column(name = "duplicate_when_missing", nullable = false, length = 10)
    private WhenMissing duplicateWhenMissing = WhenMissing.WARN;

    @Enumerated(EnumType.STRING)
    @Column(name = "duplicate_when_fails", nullable = false, length = 10)
    private WhenMissing duplicateWhenFails = WhenMissing.HOLD;

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

    public boolean holdsWhenBlankMissing() {
        return blankRequired && blankWhenMissing == WhenMissing.HOLD;
    }

    public boolean holdsWhenDuplicateMissing() {
        return duplicateRequired && duplicateWhenMissing == WhenMissing.HOLD;
    }

    /** A failed blank holds the test's rows, or only warns. */
    public boolean holdsWhenBlankFails() {
        return blankWhenFails != WhenMissing.WARN;
    }

    public boolean holdsWhenDuplicateFails() {
        return duplicateWhenFails != WhenMissing.WARN;
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

    public boolean isBlankRequired() {
        return blankRequired;
    }

    public void setBlankRequired(boolean blankRequired) {
        this.blankRequired = blankRequired;
    }

    public WhenMissing getBlankWhenMissing() {
        return blankWhenMissing;
    }

    public void setBlankWhenMissing(WhenMissing blankWhenMissing) {
        this.blankWhenMissing = blankWhenMissing;
    }

    public WhenMissing getBlankWhenFails() {
        return blankWhenFails;
    }

    public void setBlankWhenFails(WhenMissing blankWhenFails) {
        this.blankWhenFails = blankWhenFails;
    }

    public boolean isDuplicateRequired() {
        return duplicateRequired;
    }

    public void setDuplicateRequired(boolean duplicateRequired) {
        this.duplicateRequired = duplicateRequired;
    }

    public WhenMissing getDuplicateWhenMissing() {
        return duplicateWhenMissing;
    }

    public void setDuplicateWhenMissing(WhenMissing duplicateWhenMissing) {
        this.duplicateWhenMissing = duplicateWhenMissing;
    }

    public WhenMissing getDuplicateWhenFails() {
        return duplicateWhenFails;
    }

    public void setDuplicateWhenFails(WhenMissing duplicateWhenFails) {
        this.duplicateWhenFails = duplicateWhenFails;
    }
}
