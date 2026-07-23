package org.openelisglobal.qaevent.criticalcallback.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-714 [QA-C.4] — one manual critical-result callback attempt (TJC
 * NPSG.02.03.01 / CLSI GP47 read-back documentation).
 *
 * <p>
 * The callback act is manual; a row only records that it happened: which
 * persisted result was communicated ({@code resultId} + {@code resultValue}
 * snapshot, immune to later edits), who called ({@code loggedBy}, stamped
 * server-side), when ({@code loggedAt}), who received it, and the outcome.
 * Result-grain per the C.4 outline §5 — a callback can only be logged against a
 * saved result. Rows are write-once and are themselves the record — the entity
 * is deliberately NOT audited (UUID id, no reference_tables registration;
 * audited tables need numeric ids). Repeat callbacks for the same result are
 * additional rows: multiplicity is the attempt log. Numeric FKs are mapped to
 * String via {@code LIMSStringNumberUserType} (the OpenELIS idiom). The audit
 * {@code @Version} column ({@code last_updated}) comes from {@link BaseObject};
 * the DB-filled {@code lastupdated} (DEFAULT now()) is not mapped here.
 *
 * <p>
 * Convergence (grep anchor: critical_notification): OGC-811 §F's GP47 read-back
 * form and M-11's polymorphic critical_notification (OGC-785/886) describe this
 * same record — extend this entity rather than duplicating it.
 */
@Entity
@Table(name = "critical_callback", schema = "clinlims")
public class CriticalCallback extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    /** Callback outcomes; DB CHECK constraint mirrors this set. */
    public enum Status {
        CONFIRMED, REACHED_NO_READBACK, UNABLE_TO_REACH
    }

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "result_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String resultId;

    @Column(name = "analysis_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analysisId;

    @Column(name = "result_value", nullable = false, length = 200)
    private String resultValue;

    @Column(name = "logged_by", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String loggedBy;

    @Column(name = "logged_at", nullable = false)
    private Timestamp loggedAt;

    @Column(name = "recipient_name", nullable = false, length = 255)
    private String recipientName;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    public CriticalCallback() {
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

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(String loggedBy) {
        this.loggedBy = loggedBy;
    }

    public Timestamp getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Timestamp loggedAt) {
        this.loggedAt = loggedAt;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
