package org.openelisglobal.qaevent.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.qaevent.valueholder.retroCI.QaEventItem;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.annotations.ValidTime;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class NonConformityForm extends BaseForm {

    public interface NonConformity {
    }

    public interface NonConformitySearch {
    }

    @NotNull(groups = { NonConformity.class })
    private Boolean readOnly = Boolean.TRUE;

    @NotBlank(groups = { NonConformity.class })
    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { NonConformity.class })
    private String sampleId = "";

    @NotBlank(groups = { NonConformity.class })
    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { NonConformity.class })
    private String patientId = "";

    // in validator
    private String sampleItemsTypeOfSampleIds = "";

    @ValidDate(relative = DateRelation.PAST, groups = { NonConformity.class })
    private String date = "";

    @ValidTime(groups = { NonConformity.class })
    private String time = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { NonConformity.class })
    private String projectId = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String project = "";

    // unused?
    @Valid
    private List<Project> projects;

    @NotNull(groups = { NonConformity.class })
    private Boolean subjectNew = Boolean.TRUE;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX, groups = { NonConformity.class })
    private String subjectNo = "";

    @NotNull(groups = { NonConformity.class })
    private Boolean newSTNumber = Boolean.TRUE;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX, groups = { NonConformity.class })
    private String STNumber = "";

    @NotNull(groups = { NonConformity.class })
    private Boolean nationalIdNew = Boolean.TRUE;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX, groups = { NonConformity.class })
    private String nationalId = "";

    @ValidAccessionNumber(groups = { NonConformity.class, NonConformitySearch.class })
    private String labNo = "";

    @NotNull(groups = { NonConformity.class })
    private Boolean doctorNew = Boolean.TRUE;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String doctor = "";

    @NotNull(groups = { NonConformity.class })
    private Boolean serviceNew = Boolean.TRUE;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String service = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String newServiceName = "";

    @Valid
    private List<QaEventItem> qaEvents;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String section = "";

    // for display
    private List<TestSection> sections;

    // for display
    private List<IdValuePair> qaEventTypes;

    // for display
    private List<IdValuePair> typeOfSamples;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { NonConformity.class })
    private String commentId = "";

    // @NotNull(groups = { NonConformity.class })
    private Boolean commentNew;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String comment = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String biologest = "";

    // for display
    private List<IdValuePair> siteList;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { NonConformity.class })
    private String requesterSampleID = "";

    @NotNull(groups = { NonConformity.class })
    private String providerNew = "";

    @ValidName(nameType = NameType.LAST_NAME, groups = { NonConformity.class })
    private String providerLastName = "";

    @ValidName(nameType = NameType.FIRST_NAME, groups = { NonConformity.class })
    private String providerFirstName = "";

    @Pattern(regexp = ValidationHelper.PHONE_REGEX, groups = { NonConformity.class })
    private String providerWorkPhone = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String providerStreetAddress = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String providerCity = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String providerCommune = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { NonConformity.class })
    private String providerDepartment = "";

    // for display
    private List<IdValuePair> departments;

    public NonConformityForm() {
        setFormName("NonConformityForm");
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getSampleItemsTypeOfSampleIds() {
        return sampleItemsTypeOfSampleIds;
    }

    public void setSampleItemsTypeOfSampleIds(String sampleItemsTypeOfSampleIds) {
        this.sampleItemsTypeOfSampleIds = sampleItemsTypeOfSampleIds;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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

    public Boolean getSubjectNew() {
        return subjectNew;
    }

    public void setSubjectNew(Boolean subjectNew) {
        this.subjectNew = subjectNew;
    }

    public String getSubjectNo() {
        return subjectNo;
    }

    public void setSubjectNo(String subjectNo) {
        this.subjectNo = subjectNo;
    }

    public Boolean getNewSTNumber() {
        return newSTNumber;
    }

    public void setNewSTNumber(Boolean newSTNumber) {
        this.newSTNumber = newSTNumber;
    }

    public String getSTNumber() {
        return STNumber;
    }

    public void setSTNumber(String STNumber) {
        this.STNumber = STNumber;
    }

    public Boolean getNationalIdNew() {
        return nationalIdNew;
    }

    public void setNationalIdNew(Boolean nationalIdNew) {
        this.nationalIdNew = nationalIdNew;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public Boolean getDoctorNew() {
        return doctorNew;
    }

    public void setDoctorNew(Boolean doctorNew) {
        this.doctorNew = doctorNew;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    public Boolean getServiceNew() {
        return serviceNew;
    }

    public void setServiceNew(Boolean serviceNew) {
        this.serviceNew = serviceNew;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getNewServiceName() {
        return newServiceName;
    }

    public void setNewServiceName(String newServiceName) {
        this.newServiceName = newServiceName;
    }

    public List<QaEventItem> getQaEvents() {
        return qaEvents;
    }

    public void setQaEvents(List<QaEventItem> qaEvents) {
        this.qaEvents = qaEvents;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
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

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public Boolean getCommentNew() {
        return commentNew;
    }

    public void setCommentNew(Boolean commentNew) {
        this.commentNew = commentNew;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getBiologest() {
        return biologest;
    }

    public void setBiologest(String biologest) {
        this.biologest = biologest;
    }

    public List<IdValuePair> getSiteList() {
        return siteList;
    }

    public void setSiteList(List<IdValuePair> siteList) {
        this.siteList = siteList;
    }

    public String getRequesterSampleID() {
        return requesterSampleID;
    }

    public void setRequesterSampleID(String requesterSampleID) {
        this.requesterSampleID = requesterSampleID;
    }

    public String getProviderNew() {
        return providerNew;
    }

    public void setProviderNew(String providerNew) {
        this.providerNew = providerNew;
    }

    public String getProviderLastName() {
        return providerLastName;
    }

    public void setProviderLastName(String providerLastName) {
        this.providerLastName = providerLastName;
    }

    public String getProviderFirstName() {
        return providerFirstName;
    }

    public void setProviderFirstName(String providerFirstName) {
        this.providerFirstName = providerFirstName;
    }

    public String getProviderWorkPhone() {
        return providerWorkPhone;
    }

    public void setProviderWorkPhone(String providerWorkPhone) {
        this.providerWorkPhone = providerWorkPhone;
    }

    public String getProviderStreetAddress() {
        return providerStreetAddress;
    }

    public void setProviderStreetAddress(String providerStreetAddress) {
        this.providerStreetAddress = providerStreetAddress;
    }

    public String getProviderCity() {
        return providerCity;
    }

    public void setProviderCity(String providerCity) {
        this.providerCity = providerCity;
    }

    public String getProviderCommune() {
        return providerCommune;
    }

    public void setProviderCommune(String providerCommune) {
        this.providerCommune = providerCommune;
    }

    public String getProviderDepartment() {
        return providerDepartment;
    }

    public void setProviderDepartment(String providerDepartment) {
        this.providerDepartment = providerDepartment;
    }

    public List<IdValuePair> getDepartments() {
        return departments;
    }

    public void setDepartments(List<IdValuePair> departments) {
        this.departments = departments;
    }
}
