package org.openelisglobal.sample.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.patient.saving.form.IAccessionerForm;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidTime;

public class SampleEntryByProjectForm extends BaseForm implements IAccessionerForm {

    private static final long serialVersionUID = 1L;

    @ValidDate(relative = DateRelation.TODAY)
    private String currentDate = "";

    @ValidDate(relative = DateRelation.PAST)
    private String receivedDateForDisplay = "";

    @ValidTime
    private String receivedTimeForDisplay = "";

    @ValidDate(relative = DateRelation.PAST)
    private String interviewDate = "";

    @ValidTime
    private String interviewTime = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String project = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientFhirUuid = "";

    @ValidAccessionNumber(format = AccessionFormat.PROGRAMNUM /* , dateValidate = true */)
    private String labNo = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String doctor = "";

    // @NotBlank
    @Size(max = 10)
    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String subjectNumber = "";

    // @NotBlank may be subjectNumber or siteSubjectNumber
    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String siteSubjectNumber = "";

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String upidCode = "";

    @NotBlank
    @Pattern(regexp = ValidationHelper.GENDER_REGEX)
    private String gender = "";

    @ValidDate(relative = DateRelation.PAST)
    private String birthDateForDisplay = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String samplePK = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String patientPK = "";

    private PatientUpdateStatus patientUpdateStatus = PatientUpdateStatus.ADD;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientLastUpdated = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String personLastUpdated = "";

    @Valid
    private ProjectData ProjectData;

    @Valid
    private ObservationData observations;

    private ElectronicOrder electronicOrder = null;

    @Min(0)
    private Integer centerCode;

    // for display
    private Map<String, List<Organization>> organizationTypeLists;

    // for display
    private Map<String, List<Dictionary>> dictionaryLists;

    // for display
    private Map<String, List<Dictionary>> formLists;

    // for display
    private List<IdValuePair> genders;

    // for display
    private PatientSearch patientSearch;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String domain = "";

    public SampleEntryByProjectForm() {
        setFormName("sampleEntryByProjectForm");
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    @Override
    public String getReceivedDateForDisplay() {
        return receivedDateForDisplay;
    }

    public void setReceivedDateForDisplay(String receivedDateForDisplay) {
        this.receivedDateForDisplay = receivedDateForDisplay;
    }

    @Override
    public String getReceivedTimeForDisplay() {
        return receivedTimeForDisplay;
    }

    public void setReceivedTimeForDisplay(String receivedTimeForDisplay) {
        this.receivedTimeForDisplay = receivedTimeForDisplay;
    }

    @Override
    public String getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(String interviewDate) {
        this.interviewDate = interviewDate;
    }

    @Override
    public String getInterviewTime() {
        return interviewTime;
    }

    public void setInterviewTime(String interviewTime) {
        this.interviewTime = interviewTime;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    @Override
    public String getLabNo() {
        return labNo;
    }

    @Override
    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    @Override
    public String getSubjectNumber() {
        return subjectNumber;
    }

    @Override
    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    @Override
    public String getSiteSubjectNumber() {
        return siteSubjectNumber;
    }

    @Override
    public void setSiteSubjectNumber(String siteSubjectNumber) {
        this.siteSubjectNumber = siteSubjectNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public List<IdValuePair> getGenders() {
        return genders;
    }

    public void setGenders(List<IdValuePair> genders) {
        this.genders = genders;
    }

    @Override
    public String getBirthDateForDisplay() {
        return birthDateForDisplay;
    }

    @Override
    public void setBirthDateForDisplay(String birthDateForDisplay) {
        this.birthDateForDisplay = birthDateForDisplay;
    }

    @Override
    public String getSamplePK() {
        return samplePK;
    }

    public void setSamplePK(String samplePK) {
        this.samplePK = samplePK;
    }

    @Override
    public String getPatientPK() {
        return patientPK;
    }

    public void setPatientPK(String patientPK) {
        this.patientPK = patientPK;
    }

    public PatientUpdateStatus getPatientUpdateStatus() {
        return patientUpdateStatus;
    }

    public void setPatientUpdateStatus(PatientUpdateStatus patientUpdateStatus) {
        this.patientUpdateStatus = patientUpdateStatus;
    }

    public String getPatientLastUpdated() {
        return patientLastUpdated;
    }

    public void setPatientLastUpdated(String patientLastUpdated) {
        this.patientLastUpdated = patientLastUpdated;
    }

    public String getPersonLastUpdated() {
        return personLastUpdated;
    }

    public void setPersonLastUpdated(String personLastUpdated) {
        this.personLastUpdated = personLastUpdated;
    }

    @Override
    public ProjectData getProjectData() {
        return ProjectData;
    }

    @Override
    public void setProjectData(ProjectData projectData) {
        ProjectData = projectData;
    }

    @Override
    public ObservationData getObservations() {
        return observations;
    }

    @Override
    public void setObservations(ObservationData observations) {
        this.observations = observations;
    }

    public Map<String, List<Organization>> getOrganizationTypeLists() {
        return organizationTypeLists;
    }

    public void setOrganizationTypeLists(Map<String, List<Organization>> organizationTypeLists) {
        this.organizationTypeLists = organizationTypeLists;
    }

    public Map<String, List<Dictionary>> getDictionaryLists() {
        return dictionaryLists;
    }

    public void setDictionaryLists(Map<String, List<Dictionary>> dictionaryLists) {
        this.dictionaryLists = dictionaryLists;
    }

    public Map<String, List<Dictionary>> getFormLists() {
        return formLists;
    }

    public void setFormLists(Map<String, List<Dictionary>> formLists) {
        this.formLists = formLists;
    }

    public PatientSearch getPatientSearch() {
        return patientSearch;
    }

    public void setPatientSearch(PatientSearch patientSearch) {
        this.patientSearch = patientSearch;
    }

    @Override
    public Integer getCenterCode() {
        return centerCode;
    }

    public void setCenterCode(Integer centerCode) {
        this.centerCode = centerCode;
    }

    public ElectronicOrder getElectronicOrder() {
        return electronicOrder;
    }

    public void setElectronicOrder(ElectronicOrder electronicOrder) {
        this.electronicOrder = electronicOrder;
    }

    public String getPatientFhirUuid() {
        return patientFhirUuid;
    }

    public void setPatientFhirUuid(String patientFhirUuid) {
        this.patientFhirUuid = patientFhirUuid;
    }

    public String getUpidCode() {
        return upidCode;
    }

    public void setUpidCode(String upidCode) {
        this.upidCode = upidCode;
    }
}
