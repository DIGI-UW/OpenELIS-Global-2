package org.openelisglobal.resultrun.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1200: the unit results are produced and reviewed in. An analyzer run is
 * one accepted message; its staged values and the results accepted from them
 * name it. Not {@code QcRun}, which OGC-1054 keeps out of the target
 * architecture.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "result_run", schema = "clinlims")
public class ResultRun extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "result_run_generator")
    @SequenceGenerator(name = "result_run_generator", sequenceName = "result_run_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private ResultRunSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 12)
    private ResultRunState state = ResultRunState.OPEN;

    @Column(name = "analyzer_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analyzerId;

    @Column(name = "test_section_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testSectionId;

    @Column(name = "delivery_receipt_id", length = 36)
    private String deliveryReceiptId;

    @Column(name = "source_connection_id", length = 255)
    private String sourceConnectionId;

    @Column(name = "source_message_id", length = 255)
    private String sourceMessageId;

    @Column(name = "operator_name", length = 255)
    private String operatorName;

    /** Every row the run ever held, so progress survives rows leaving staging. */
    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "opened_at", nullable = false)
    private Timestamp openedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Timestamp lastActivityAt;

    @Column(name = "completed_at")
    private Timestamp completedAt;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    /** The name the lab reads: RUN-42. */
    public String getLabel() {
        return id == null ? null : "RUN-" + id;
    }

    public ResultRunSource getSource() {
        return source;
    }

    public void setSource(ResultRunSource source) {
        this.source = source;
    }

    public ResultRunState getState() {
        return state;
    }

    public void setState(ResultRunState state) {
        this.state = state;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getDeliveryReceiptId() {
        return deliveryReceiptId;
    }

    public void setDeliveryReceiptId(String deliveryReceiptId) {
        this.deliveryReceiptId = deliveryReceiptId;
    }

    public String getSourceConnectionId() {
        return sourceConnectionId;
    }

    public void setSourceConnectionId(String sourceConnectionId) {
        this.sourceConnectionId = sourceConnectionId;
    }

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public void setSourceMessageId(String sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public Timestamp getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Timestamp openedAt) {
        this.openedAt = openedAt;
    }

    public Timestamp getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Timestamp lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }
}
