package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Date;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "nce_action_log", schema = "clinlims")
public class NceActionLog extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nce_action_log_seq_gen")
    @SequenceGenerator(name = "nce_action_log_seq_gen", sequenceName = "nce_action_log_id_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "corrective_action")
    private String correctiveAction;

    @Column(name = "action_type", length = 100)
    private String actionType;

    @Column(name = "person_responsible", length = 200)
    private String personResponsible;

    @Column(name = "date_completed")
    private Date dateCompleted;

    @Column(name = "turn_around_time")
    private Integer turnAroundTime;

    @Column(name = "nce_event_id")
    private Integer ncEventId;

    @Column(name = "effective")
    private Boolean effective;

    @Column(name = "review_comments")
    private String reviewComments;

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Column(name = "review_date")
    private Date reviewDate;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getPersonResponsible() {
        return personResponsible;
    }

    public void setPersonResponsible(String personResponsible) {
        this.personResponsible = personResponsible;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public Integer getTurnAroundTime() {
        return turnAroundTime;
    }

    public void setTurnAroundTime(Integer turnAroundTime) {
        this.turnAroundTime = turnAroundTime;
    }

    public Integer getNcEventId() {
        return ncEventId;
    }

    public void setNcEventId(Integer ncEventId) {
        this.ncEventId = ncEventId;
    }

    public Boolean getEffective() {
        return effective;
    }

    public void setEffective(Boolean effective) {
        this.effective = effective;
    }

    public String getReviewComments() {
        return reviewComments;
    }

    public void setReviewComments(String reviewComments) {
        this.reviewComments = reviewComments;
    }

    public Integer getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Integer reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Date getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(Date reviewDate) {
        this.reviewDate = reviewDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NceActionLog that = (NceActionLog) o;
        return Objects.equals(id, that.id) && Objects.equals(ncEventId, that.ncEventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ncEventId);
    }
}
