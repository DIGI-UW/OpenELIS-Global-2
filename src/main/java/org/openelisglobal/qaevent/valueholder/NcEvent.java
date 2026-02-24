package org.openelisglobal.qaevent.valueholder;

import java.sql.Date;
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

public class NcEvent extends BaseObject<String> {
    public static final String NCE_NUMBER_PREFIX = "NCE-";
    public static final String STATUS_PENDING = "Pending";
    public static final String ACTION_REJECT_SAMPLE = "REJECT_SAMPLE";

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
    private String sourceType;
    private String triggerType;
    private String triggerAction;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameOfReporter() {
        return nameOfReporter;
    }

    public void setNameOfReporter(String nameOfReporter) {
        this.nameOfReporter = nameOfReporter;
    }

    public String getNceNumber() {
        return nceNumber;
    }

    public void setNceNumber(String nceNumber) {
        this.nceNumber = nceNumber;
    }

    public Date getDateOfEvent() {
        return dateOfEvent;
    }

    public void setDateOfEvent(Date dateOfEvent) {
        this.dateOfEvent = dateOfEvent;
    }

    public String getLabOrderNumber() {
        return labOrderNumber;
    }

    public void setLabOrderNumber(String labOrderNumber) {
        this.labOrderNumber = labOrderNumber;
    }

    public String getPrescriberName() {
        return prescriberName;
    }

    public void setPrescriberName(String prescriberName) {
        this.prescriberName = prescriberName;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public Integer getReportingUnitId() {
        return reportingUnitId;
    }

    public void setReportingUnitId(Integer reportingUnitId) {
        this.reportingUnitId = reportingUnitId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuspectedCauses() {
        return suspectedCauses;
    }

    public void setSuspectedCauses(String suspectedCauses) {
        this.suspectedCauses = suspectedCauses;
    }

    public String getProposedAction() {
        return proposedAction;
    }

    public void setProposedAction(String proposedAction) {
        this.proposedAction = proposedAction;
    }

    public String getLaboratoryComponent() {
        return laboratoryComponent;
    }

    public void setLaboratoryComponent(String laboratoryComponent) {
        this.laboratoryComponent = laboratoryComponent;
    }

    public String getConsequenceId() {
        return consequenceId;
    }

    public void setConsequenceId(String consequenceId) {
        this.consequenceId = consequenceId;
    }

    public String getRecurrenceId() {
        return recurrenceId;
    }

    public void setRecurrenceId(String recurrenceId) {
        this.recurrenceId = recurrenceId;
    }

    public String getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(String severityScore) {
        this.severityScore = severityScore;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getCorrectiveAction() {
        return correctiveAction;
    }

    public void setCorrectiveAction(String correctiveAction) {
        this.correctiveAction = correctiveAction;
    }

    public String getControlAction() {
        return controlAction;
    }

    public void setControlAction(String controlAction) {
        this.controlAction = controlAction;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getEffective() {
        return effective;
    }

    public void setEffective(String effective) {
        this.effective = effective;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Date getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Date dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public Integer getNceCategoryId() {
        return nceCategoryId;
    }

    public void setNceCategoryId(Integer nceCategoryId) {
        this.nceCategoryId = nceCategoryId;
    }

    public String getSeverityId() {
        return severityId;
    }

    public void setSeverityId(String severityId) {
        this.severityId = severityId;
    }

    public Integer getNceTypeId() {
        return nceTypeId;
    }

    public void setNceTypeId(Integer nceTypeId) {
        this.nceTypeId = nceTypeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDiscussionDate() {
        return discussionDate;
    }

    public void setDiscussionDate(String discussionDate) {
        this.discussionDate = discussionDate;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerAction() {
        return triggerAction;
    }

    public void setTriggerAction(String triggerAction) {
        this.triggerAction = triggerAction;
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
        return Objects.equals(id, ncEvent.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
