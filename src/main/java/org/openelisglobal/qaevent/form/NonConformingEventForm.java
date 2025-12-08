package org.openelisglobal.qaevent.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceActionLog;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;

public class NonConformingEventForm extends BaseForm {

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String id;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String currentUserId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String status;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reportDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String name;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reporterName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nceNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateOfEvent;

    @ValidAccessionNumber
    private String labOrderNumber;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String specimen; // FK

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String prescriberName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String site;

    private Integer reportingUnit;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String description;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String suspectedCauses;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String proposedAction;

    @Valid
    private List<SampleItem> specimens;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String specimenId;

    /// for displayNcEvent
    private PatientSearch patientSearch;

    private String laboratoryComponent;

    private String nceCategory;

    private String nceType;

    private String consequences;

    private String recurrence;

    private String severityScore;

    private String colorCode;

    private String correctiveAction;

    private String controlAction;

    private String comments;

    private String discussionDate;

    private List<NceActionLog> actionLog;

    private String actionLogStr;

    private String effective;

    private String dateCompleted;

    private List<IdValuePair> reportingUnits;

    private List<NceCategory> nceCategories;

    private List<NceType> nceTypes;

    private List<IdValuePair> labComponentList;

    private List<IdValuePair> severityConsequencesList;

    private List<IdValuePair> severityRecurrenceList;

    private List<IdValuePair> actionTypeList;

    private List<NcEvent> nceEventsSearchResults;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getNceNumber() {
        return nceNumber;
    }

    public void setNceNumber(String nceNumber) {
        this.nceNumber = nceNumber;
    }

    public String getDateOfEvent() {
        return dateOfEvent;
    }

    public void setDateOfEvent(String dateOfEvent) {
        this.dateOfEvent = dateOfEvent;
    }

    public String getLabOrderNumber() {
        return labOrderNumber;
    }

    public void setLabOrderNumber(String labOrderNumber) {
        this.labOrderNumber = labOrderNumber;
    }

    public String getSpecimen() {
        return specimen;
    }

    public void setSpecimen(String specimen) {
        this.specimen = specimen;
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

    public Integer getReportingUnit() {
        return reportingUnit;
    }

    public void setReportingUnit(Integer reportingUnit) {
        this.reportingUnit = reportingUnit;
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

    public PatientSearch getPatientSearch() {
        return patientSearch;
    }

    public void setPatientSearch(PatientSearch patientSearch) {
        this.patientSearch = patientSearch;
    }

    public List<SampleItem> getSpecimens() {
        return specimens;
    }

    public void setSpecimens(List<SampleItem> specimens) {
        this.specimens = specimens;
    }

    public String getLaboratoryComponent() {
        return laboratoryComponent;
    }

    public void setLaboratoryComponent(String laboratoryComponent) {
        this.laboratoryComponent = laboratoryComponent;
    }

    public String getNceCategory() {
        return nceCategory;
    }

    public void setNceCategory(String nceCategory) {
        this.nceCategory = nceCategory;
    }

    public String getNceType() {
        return nceType;
    }

    public void setNceType(String nceType) {
        this.nceType = nceType;
    }

    public String getConsequences() {
        return consequences;
    }

    public void setConsequences(String consequences) {
        this.consequences = consequences;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
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

    public String getDiscussionDate() {
        return discussionDate;
    }

    public void setDiscussionDate(String discussionDate) {
        this.discussionDate = discussionDate;
    }

    public List<NceActionLog> getActionLog() {
        return actionLog;
    }

    public void setActionLog(List<NceActionLog> actionLog) {
        this.actionLog = actionLog;
    }

    public String getActionLogStr() {
        return actionLogStr;
    }

    public void setActionLogStr(String actionLogStr) {
        this.actionLogStr = actionLogStr;
    }

    public String getEffective() {
        return effective;
    }

    public void setEffective(String effective) {
        this.effective = effective;
    }

    public String getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(String dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public List<IdValuePair> getReportingUnits() {
        return reportingUnits;
    }

    public void setReportingUnits(List<IdValuePair> reportingUnits) {
        this.reportingUnits = reportingUnits;
    }

    public List<NceCategory> getNceCategories() {
        return nceCategories;
    }

    public void setNceCategories(List<NceCategory> nceCategories) {
        this.nceCategories = nceCategories;
    }

    public List<NceType> getNceTypes() {
        return nceTypes;
    }

    public void setNceTypes(List<NceType> list) {
        this.nceTypes = list;
    }

    public List<IdValuePair> getLabComponentList() {
        return labComponentList;
    }

    public void setLabComponentList(List<IdValuePair> labComponentList) {
        this.labComponentList = labComponentList;
    }

    public List<IdValuePair> getSeverityConsequencesList() {
        return severityConsequencesList;
    }

    public void setSeverityConsequencesList(List<IdValuePair> severityConsequencesList) {
        this.severityConsequencesList = severityConsequencesList;
    }

    public List<IdValuePair> getSeverityRecurrenceList() {
        return severityRecurrenceList;
    }

    public void setSeverityRecurrenceList(List<IdValuePair> severityRecurrenceList) {
        this.severityRecurrenceList = severityRecurrenceList;
    }

    public List<IdValuePair> getActionTypeList() {
        return actionTypeList;
    }

    public void setActionTypeList(List<IdValuePair> actionTypeList) {
        this.actionTypeList = actionTypeList;
    }

    public String getSpecimenId() {
        return specimenId;
    }

    public void setSpecimenId(String specimenId) {
        this.specimenId = specimenId;
    }

    public List<NcEvent> getnceEventsSearchResults() {
        return nceEventsSearchResults;
    }

    public void setnceEventsSearchResults(List<NcEvent> nceEventsSearchResults) {
        this.nceEventsSearchResults = nceEventsSearchResults;
    }
}
