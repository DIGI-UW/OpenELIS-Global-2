package org.openelisglobal.qaevent.valueholder;

import java.sql.Date;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

public class NceActionLog extends BaseObject<String> {

    private String id;
    private String correctiveAction;
    private String actionType;
    private String personResponsible;
    private Date dateCompleted;
    private Integer turnAroundTime;
    private Integer ncEventId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public void setTurnAroundTime(Integer turnAoundTime) {
        this.turnAroundTime = turnAoundTime;
    }

    public Integer getNcEventId() {
        return ncEventId;
    }

    public void setNcEventId(Integer ncEventId) {
        this.ncEventId = ncEventId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NceActionLog that = (NceActionLog) o;
        return id == that.id && Objects.equals(correctiveAction, that.correctiveAction)
                && Objects.equals(actionType, that.actionType)
                && Objects.equals(personResponsible, that.personResponsible)
                && Objects.equals(dateCompleted, that.dateCompleted)
                && Objects.equals(turnAroundTime, that.turnAroundTime) && Objects.equals(ncEventId, that.ncEventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, correctiveAction, actionType, personResponsible, dateCompleted, turnAroundTime,
                ncEventId);
    }
}
