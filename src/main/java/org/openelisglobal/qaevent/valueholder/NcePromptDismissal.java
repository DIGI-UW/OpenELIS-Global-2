package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Entity for tracking when a user dismisses a critical/extreme value alert
 * prompt without creating an NCE. Provides an audit trail per spec Section 9.3.
 */
@Entity
@Table(name = "nce_prompt_dismissal")
public class NcePromptDismissal extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Basic
    @Column(name = "trigger_action", length = 50, nullable = false)
    private String triggerAction;

    @Basic
    @Column(name = "source_type", length = 50, nullable = false)
    private String sourceType;

    @Basic
    @Column(name = "result_id", length = 20)
    private String resultId;

    @Basic
    @Column(name = "context", columnDefinition = "TEXT")
    private String context;

    @Basic
    @Column(name = "dismissed_by", length = 100, nullable = false)
    private String dismissedBy;

    @Basic
    @Column(name = "dismissed_date", nullable = false)
    private Timestamp dismissedDate;

    public NcePromptDismissal() {
        super();
        this.dismissedDate = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getTriggerAction() {
        return triggerAction;
    }

    public void setTriggerAction(String triggerAction) {
        this.triggerAction = triggerAction;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDismissedBy() {
        return dismissedBy;
    }

    public void setDismissedBy(String dismissedBy) {
        this.dismissedBy = dismissedBy;
    }

    public Timestamp getDismissedDate() {
        return dismissedDate;
    }

    public void setDismissedDate(Timestamp dismissedDate) {
        this.dismissedDate = dismissedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NcePromptDismissal that = (NcePromptDismissal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
