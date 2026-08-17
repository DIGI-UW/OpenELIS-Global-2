package org.openelisglobal.qaevent.bean;

import java.util.Date;

/**
 * Flat row for the cross-NCE CAPA Register (OGC-707). Projected directly from
 * an HQL join of {@code nce_action_log} and {@code nc_event}.
 *
 * <p>
 * Completion is read from the parent NCE
 * ({@code nc_event.status}/{@code date_completed}), not from the action log's
 * own {@code date_completed}/{@code effective} columns: those are
 * null/unwritten in the React corrective-action flow (the resolve step marks
 * the parent NCE completed instead). Register status (open/overdue/completed)
 * is derived on the client from these fields.
 */
public class CapaRegisterItem {

    private final Integer id;
    private final Integer nceEventId;
    private final String nceNumber;
    private final String nceStatus;
    private final String correctiveAction;
    private final String actionType;
    private final String personResponsible;
    private final Date dueDate;
    private final Date dateCompleted;

    public CapaRegisterItem(Integer id, Integer nceEventId, String nceNumber, String nceStatus, String correctiveAction,
            String actionType, String personResponsible, Date dueDate, Date dateCompleted) {
        this.id = id;
        this.nceEventId = nceEventId;
        this.nceNumber = nceNumber;
        this.nceStatus = nceStatus;
        this.correctiveAction = correctiveAction;
        this.actionType = actionType;
        this.personResponsible = personResponsible;
        this.dueDate = dueDate;
        this.dateCompleted = dateCompleted;
    }

    public Integer getId() {
        return id;
    }

    public Integer getNceEventId() {
        return nceEventId;
    }

    public String getNceNumber() {
        return nceNumber;
    }

    public String getNceStatus() {
        return nceStatus;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public String getActionType() {
        return actionType;
    }

    public String getPersonResponsible() {
        return personResponsible;
    }

    // java.util.Date params so Hibernate's constructor-projection type match
    // succeeds (it derives
    // java.util.Date from the DATE columns). Serialize as yyyy-MM-dd strings (as
    // the NCE dashboard
    // does) so the client compares/derives status without timezone or epoch-millis
    // ambiguity.
    public String getDueDate() {
        return dueDate == null ? null : new java.sql.Date(dueDate.getTime()).toLocalDate().toString();
    }

    public String getDateCompleted() {
        return dateCompleted == null ? null : new java.sql.Date(dateCompleted.getTime()).toLocalDate().toString();
    }
}
