package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_override_event", schema = "clinlims")
public class MicroAstOverrideEvent extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "reading_id", nullable = false, length = 36)
    private String readingId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "from_interpretation", nullable = false, length = 40)
    private String fromInterpretation;

    @Column(name = "to_interpretation", nullable = false, length = 40)
    private String toInterpretation;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "performed_at", nullable = false)
    private Timestamp performedAt;

    @Column(name = "performed_by", nullable = false, length = 20)
    private String performedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getReadingId() {
        return readingId;
    }

    public void setReadingId(String readingId) {
        this.readingId = readingId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFromInterpretation() {
        return fromInterpretation;
    }

    public void setFromInterpretation(String fromInterpretation) {
        this.fromInterpretation = fromInterpretation;
    }

    public String getToInterpretation() {
        return toInterpretation;
    }

    public void setToInterpretation(String toInterpretation) {
        this.toInterpretation = toInterpretation;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Timestamp getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Timestamp performedAt) {
        this.performedAt = performedAt;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }
}
