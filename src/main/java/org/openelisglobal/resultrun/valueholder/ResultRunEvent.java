package org.openelisglobal.resultrun.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Something that happened on a run (OGC-1200 FR-A6): a control recorded, a test
 * held or released. Written once, never changed.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "result_run_event", schema = "clinlims")
public class ResultRunEvent extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public static final String CONTROL_RECORDED = "controlRecorded";
    public static final String TEST_HELD = "testHeld";
    public static final String TEST_RELEASED = "testReleased";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "result_run_event_generator")
    @SequenceGenerator(name = "result_run_event_generator", sequenceName = "result_run_event_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "event", nullable = false, length = 40)
    private String event;

    @Column(name = "test_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "detail", length = 255)
    private String detail;

    @Column(name = "sys_user_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String actorId;

    @Column(name = "occurred_at", nullable = false)
    private Timestamp occurredAt;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public Timestamp getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Timestamp occurredAt) {
        this.occurredAt = occurredAt;
    }
}
