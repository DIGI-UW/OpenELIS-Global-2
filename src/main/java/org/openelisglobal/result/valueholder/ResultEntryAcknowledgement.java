package org.openelisglobal.result.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1417: the record that the person entering a result acknowledged a
 * critical value, or confirmed a value outside the valid range, before saving
 * it. Write-once; the row is itself the record.
 */
@Entity
@Table(name = "result_entry_acknowledgement", schema = "clinlims")
public class ResultEntryAcknowledgement extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public static final String KIND_CRITICAL = "CRITICAL_ACKNOWLEDGED";
    public static final String KIND_INVALID = "INVALID_CONFIRMED";

    public static final String SOURCE_RESULTS_ENTRY = "RESULTS_ENTRY";
    public static final String SOURCE_VALIDATION = "VALIDATION";
    public static final String SOURCE_ANALYZER_REVIEW = "ANALYZER_REVIEW";

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "analysis_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analysisId;

    @Column(name = "result_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String resultId;

    @Column(name = "kind", nullable = false, length = 30)
    private String kind;

    @Column(name = "result_value", nullable = false, length = 200)
    private String resultValue;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "acknowledged_by", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String acknowledgedBy;

    @Column(name = "acknowledged_at", nullable = false)
    private Timestamp acknowledgedAt;

    public ResultEntryAcknowledgement() {
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

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Timestamp getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Timestamp acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }
}
