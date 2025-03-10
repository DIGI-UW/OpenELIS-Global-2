package org.openelisglobal.qaevent.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.sql.Date;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;

public class RejectionForm extends BaseForm {

    private static final long serialVersionUID = 1L;

    private Boolean readOnly = Boolean.TRUE;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String patientId = "";

    private Date rejectionDate;

    private String rejectionDateForDisplay;

    private Date collectionDate;

    private String collectionDateForDisplay;

    private Date receptionDate;

    private String receptionDateForDisplay;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String projectId = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String project = "";

    @Valid
    private List<Project> projects;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String subjectNo = "";

    @ValidAccessionNumber()
    private String labNo = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String doctor = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String qaEventId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sectionId = "";

    // for display
    private List<TestSection> sections;

    private String qaEventTypeId;

    // for display
    private List<IdValuePair> qaEventTypes;

    private String typeOfSampleId;

    // for display
    private List<IdValuePair> typeOfSamples;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String comment = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String biologist = "";

    // for display
    private List<IdValuePair> siteList;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String organizationId;

    private String samplerName = "";

    public RejectionForm() {
        setFormName("RejectionForm");
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    public String getSubjectNo() {
        return subjectNo;
    }

    public void setSubjectNo(String subjectNo) {
        this.subjectNo = subjectNo;
    }

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    public List<TestSection> getSections() {
        return sections;
    }

    public void setSections(List<TestSection> sections) {
        this.sections = sections;
    }

    public List<IdValuePair> getQaEventTypes() {
        return qaEventTypes;
    }

    public void setQaEventTypes(List<IdValuePair> qaEventTypes) {
        this.qaEventTypes = qaEventTypes;
    }

    public List<IdValuePair> getTypeOfSamples() {
        return typeOfSamples;
    }

    public void setTypeOfSamples(List<IdValuePair> typeOfSamples) {
        this.typeOfSamples = typeOfSamples;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<IdValuePair> getSiteList() {
        return siteList;
    }

    public void setSiteList(List<IdValuePair> siteList) {
        this.siteList = siteList;
    }

    public Date getRejectionDate() {
        return rejectionDate;
    }

    public void setRejectionDate(Date rejectionDate) {
        this.rejectionDate = rejectionDate;
    }

    public Date getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Date collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Date getReceptionDate() {
        return receptionDate;
    }

    public void setReceptionDate(Date receptionDate) {
        this.receptionDate = receptionDate;
    }

    public String getBiologist() {
        return biologist;
    }

    public void setBiologist(String biologist) {
        this.biologist = biologist;
    }

    public String getSamplerName() {
        return samplerName;
    }

    public void setSamplerName(String samplerName) {
        this.samplerName = samplerName;
    }

    public String getQaEventId() {
        return qaEventId;
    }

    public void setQaEventId(String qaEventId) {
        this.qaEventId = qaEventId;
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getQaEventTypeId() {
        return qaEventTypeId;
    }

    public void setQaEventTypeId(String qaEventTypeId) {
        this.qaEventTypeId = qaEventTypeId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getTypeOfSampleId() {
        return typeOfSampleId;
    }

    public void setTypeOfSampleId(String typeOfSampleId) {
        this.typeOfSampleId = typeOfSampleId;
    }

    public String getRejectionDateForDisplay() {
        return rejectionDateForDisplay;
    }

    public void setRejectionDateForDisplay(String rejectionDateForDisplay) {
        this.rejectionDateForDisplay = rejectionDateForDisplay;
    }

    public String getCollectionDateForDisplay() {
        return collectionDateForDisplay;
    }

    public void setCollectionDateForDisplay(String collectionDateForDisplay) {
        this.collectionDateForDisplay = collectionDateForDisplay;
    }

    public String getReceptionDateForDisplay() {
        return receptionDateForDisplay;
    }

    public void setReceptionDateForDisplay(String receptionDateForDisplay) {
        this.receptionDateForDisplay = receptionDateForDisplay;
    }
}
