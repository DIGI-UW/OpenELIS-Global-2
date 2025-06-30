package org.openelisglobal.qaevent.valueholder;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import java.sql.Date;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

public class NcEvent extends BaseObject<String> {
    private String id;
    private Date reportDate;
    private String name;
    private String nameOfReporter;
    private String nceNumber;
    private Date dateOfEvent;
    private String labOrderNumber;
    private String prescriberName;
    private String site;
    private Integer reportingUnitId;
    private String description;
    private String suspectedCauses;
    private String proposedAction;
    private String laboratoryComponent;
    private String consequenceId;
    private String recurrenceId;
    private String severityScore;
    private String colorCode;
    private String correctiveAction;
    private String controlAction;
    private String comments;
    private String effective;
    private String signature;
    private Date dateCompleted;
    private Integer nceCategoryId;
    private String severityId;
    private Integer nceTypeId;
    private String status;
    private String discussionDate;

    @Override
    @Id
    @Column(name = "id", nullable = false)
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Basic
    @Column(name = "report_date", nullable = true)
    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }

    @Basic
    @Column(name = "name", nullable = true, length = 200)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "name_of_reporter", nullable = true, length = 200)
    public String getNameOfReporter() {
        return nameOfReporter;
    }

    public void setNameOfReporter(String nameOfReporter) {
        this.nameOfReporter = nameOfReporter;
    }

    @Basic
    @Column(name = "nce_number", nullable = true, length = 20)
    public String getNceNumber() {
        return nceNumber;
    }

    public void setNceNumber(String nceNumber) {
        this.nceNumber = nceNumber;
    }

    @Basic
    @Column(name = "date_of_event", nullable = true)
    public Date getDateOfEvent() {
        return dateOfEvent;
    }

    public void setDateOfEvent(Date dateOfEvent) {
        this.dateOfEvent = dateOfEvent;
    }

    @Basic
    @Column(name = "lab_order_number", nullable = true, length = 30)
    public String getLabOrderNumber() {
        return labOrderNumber;
    }

    public void setLabOrderNumber(String labOrderNumber) {
        this.labOrderNumber = labOrderNumber;
    }

    @Basic
    @Column(name = "prescriber_name", nullable = true, length = 200)
    public String getPrescriberName() {
        return prescriberName;
    }

    public void setPrescriberName(String prescriberName) {
        this.prescriberName = prescriberName;
    }

    @Basic
    @Column(name = "site", nullable = true, length = 200)
    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    @Basic
    @Column(name = "reporting_unit_id", nullable = true, length = 10)
    public Integer getReportingUnitId() {
        return reportingUnitId;
    }

    public void setReportingUnitId(Integer reportingUnitId) {
        this.reportingUnitId = reportingUnitId;
    }

    @Basic
    @Column(name = "description", nullable = true, length = -1)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Basic
    @Column(name = "suspected_causes", nullable = true, length = -1)
    public String getSuspectedCauses() {
        return suspectedCauses;
    }

    public void setSuspectedCauses(String suspectedCauses) {
        this.suspectedCauses = suspectedCauses;
    }

    @Basic
    @Column(name = "proposed_action", nullable = true, length = -1)
    public String getProposedAction() {
        return proposedAction;
    }

    public void setProposedAction(String proposedAction) {
        this.proposedAction = proposedAction;
    }

    @Basic
    @Column(name = "laboratory_component", nullable = true, length = 10)
    public String getLaboratoryComponent() {
        return laboratoryComponent;
    }

    public void setLaboratoryComponent(String laboratoryComponent) {
        this.laboratoryComponent = laboratoryComponent;
    }

    @Basic
    @Column(name = "severity_id")
    public String getConsequenceId() {
        return consequenceId;
    }

    public void setConsequenceId(String consequenceId) {
        this.consequenceId = consequenceId;
    }

    @Basic
    @Column(name = "recurrence_id", nullable = true, length = 10)
    public String getRecurrenceId() {
        return recurrenceId;
    }

    public void setRecurrenceId(String recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    @Basic
    @Column(name = "severity_score", nullable = true, length = 5)
    public String getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(String severityScore) {
        this.severityScore = severityScore;
    }

    @Basic
    @Column(name = "color_code", nullable = true, length = 5)
    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    @Basic
    @Column(name = "corrective_action", nullable = true, length = -1)
    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    @Basic
    @Column(name = "control_action", nullable = true, length = -1)
    public String getControlAction() {
        return controlAction;
    }

    public void setControlAction(String controlAction) {
        this.controlAction = controlAction;
    }

    @Basic
    @Column(name = "comments", nullable = true, length = -1)
    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Basic
    @Column(name = "effective", nullable = true, length = 5)
    public String getEffective() {
        return effective;
    }

    public void setEffective(String effective) {
        this.effective = effective;
    }

    @Basic
    @Column(name = "signature", nullable = true, length = 200)
    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Basic
    @Column(name = "date_completed", nullable = true)
    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NcEvent ncEvent = (NcEvent) o;
        return id == ncEvent.id && Objects.equals(reportDate, ncEvent.reportDate) && Objects.equals(name, ncEvent.name)
                && Objects.equals(nameOfReporter, ncEvent.nameOfReporter)
                && Objects.equals(nceNumber, ncEvent.nceNumber) && Objects.equals(dateOfEvent, ncEvent.dateOfEvent)
                && Objects.equals(labOrderNumber, ncEvent.labOrderNumber)
                && Objects.equals(prescriberName, ncEvent.prescriberName) && Objects.equals(site, ncEvent.site)
                && Objects.equals(reportingUnitId, ncEvent.reportingUnitId)
                && Objects.equals(description, ncEvent.description)
                && Objects.equals(suspectedCauses, ncEvent.suspectedCauses)
                && Objects.equals(proposedAction, ncEvent.proposedAction)
                && Objects.equals(laboratoryComponent, ncEvent.laboratoryComponent)
                && Objects.equals(consequenceId, ncEvent.consequenceId)
                && Objects.equals(recurrenceId, ncEvent.recurrenceId)
                && Objects.equals(severityScore, ncEvent.severityScore) && Objects.equals(colorCode, ncEvent.colorCode)
                && Objects.equals(correctiveAction, ncEvent.correctiveAction)
                && Objects.equals(controlAction, ncEvent.controlAction) && Objects.equals(comments, ncEvent.comments)
                && Objects.equals(effective, ncEvent.effective) && Objects.equals(signature, ncEvent.signature)
                && Objects.equals(dateCompleted, ncEvent.dateCompleted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reportDate, name, nameOfReporter, nceNumber, dateOfEvent, labOrderNumber,
                prescriberName, site, reportingUnitId, description, suspectedCauses, proposedAction,
                laboratoryComponent, consequenceId, recurrenceId, severityScore, colorCode, correctiveAction,
                controlAction, comments, effective, signature, dateCompleted);
    }

    @Basic
    @Column(name = "nce_category_id", nullable = true)
    public Integer getNceCategoryId() {
        return nceCategoryId;
    }

    public void setNceCategoryId(Integer nceCategoryId) {
        this.nceCategoryId = nceCategoryId;
    }

    @Basic
    @Column(name = "severity_id", nullable = true, length = 10)
    public String getSeverityId() {
        return severityId;
    }

    public void setSeverityId(String severityId) {
        this.severityId = severityId;
    }

    @Basic
    @Column(name = "nce_type_id", nullable = true)
    public Integer getNceTypeId() {
        return nceTypeId;
    }

    public void setNceTypeId(Integer nceTypeId) {
        this.nceTypeId = nceTypeId;
    }

    @Basic
    @Column(name = "status", nullable = true)
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Basic
    @Column(name = "discussion_date", nullable = true)
    public String getDiscussionDate() {
        return discussionDate;
    }

    public void setDiscussionDate(String discussionDate) {
        this.discussionDate = discussionDate;
    }
}
