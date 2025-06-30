package org.openelisglobal.patient.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dictionary.ObservationHistoryList;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.saving.form.IAccessionerForm;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.annotations.ValidTime;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class PatientEntryByProjectForm extends BaseForm implements IAccessionerForm {
    // for display
    private Map<String, Object> formLists;

    // for display
    private Map<String, ObservationHistoryList> dictionaryLists;

    // for display
    private Map<String, OrganizationTypeList> organizationTypeLists;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String patientPK = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String samplePK = "";

    @ValidDate(relative = DateRelation.PAST)
    private String interviewDate = "";

    @ValidTime
    private String interviewTime = "";

    @ValidDate(relative = DateRelation.PAST)
    private String receivedDateForDisplay = "";

    @ValidTime
    private String receivedTimeForDisplay = "";

    @ValidDate(relative = DateRelation.TODAY)
    private String currentDate = "";

    private PatientUpdateStatus patientUpdateStatus = PatientUpdateStatus.ADD;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String subjectNumber = "";

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String siteSubjectNumber = "";

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String upidCode = "";

    @ValidAccessionNumber(format = AccessionFormat.PROGRAMNUM)
    private String labNo = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String centerName = "";

    @Min(0)
    private Integer centerCode;

    @ValidName(nameType = NameType.FIRST_NAME)
    private String firstName = "";

    @ValidName(nameType = NameType.LAST_NAME)
    private String lastName = "";

    @Pattern(regexp = ValidationHelper.GENDER_REGEX)
    private String gender = "";

    @ValidDate(relative = DateRelation.PAST)
    private String birthDateForDisplay = "";

    @Valid
    private ObservationData observations;

    @Valid
    private ProjectData projectData;

    @Valid
    private ElectronicOrder electronicOrder;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String patientFhirUuid;

    public PatientEntryByProjectForm() {
        setFormName("patientEntryByProjectForm");
    }

    public Map<String, Object> getFormLists() {
        return formLists;
    }

    public void setFormLists(Map<String, Object> formLists) {
        this.formLists = formLists;
    }

    public Map<String, ObservationHistoryList> getDictionaryLists() {
        return dictionaryLists;
    }

    public void setDictionaryLists(Map<String, ObservationHistoryList> dictionaryLists) {
        this.dictionaryLists = dictionaryLists;
    }

    public Map<String, OrganizationTypeList> getOrganizationTypeLists() {
        return organizationTypeLists;
    }

    public void setOrganizationTypeLists(Map<String, OrganizationTypeList> organizationTypeLists) {
        this.organizationTypeLists = organizationTypeLists;
    }

    public String getPatientPK() {
        return patientPK;
    }

    public void setPatientPK(String patientPK) {
        this.patientPK = patientPK;
    }

    public String getSamplePK() {
        return samplePK;
    }

    public void setSamplePK(String samplePK) {
        this.samplePK = samplePK;
    }

    public String getInterviewDate() {
        return interviewDate;
    }

    public void setInterviewDate(String interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getInterviewTime() {
        return interviewTime;
    }

    public void setInterviewTime(String interviewTime) {
        this.interviewTime = interviewTime;
    }

    public String getReceivedDateForDisplay() {
        return receivedDateForDisplay;
    }

    public void setReceivedDateForDisplay(String receivedDateForDisplay) {
        this.receivedDateForDisplay = receivedDateForDisplay;
    }

    public String getReceivedTimeForDisplay() {
        return receivedTimeForDisplay;
    }

    public void setReceivedTimeForDisplay(String receivedTimeForDisplay) {
        this.receivedTimeForDisplay = receivedTimeForDisplay;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public PatientUpdateStatus getPatientUpdateStatus() {
        return patientUpdateStatus;
    }

    public void setPatientUpdateStatus(PatientUpdateStatus patientUpdateStatus) {
        this.patientUpdateStatus = patientUpdateStatus;
    }

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public String getSiteSubjectNumber() {
        return siteSubjectNumber;
    }

    public void setSiteSubjectNumber(String siteSubjectNumber) {
        this.siteSubjectNumber = siteSubjectNumber;
    }

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public Integer getCenterCode() {
        return centerCode;
    }

    public void setCenterCode(Integer centerCode) {
        this.centerCode = centerCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDateForDisplay() {
        return birthDateForDisplay;
    }

    public void setBirthDateForDisplay(String birthDateForDisplay) {
        this.birthDateForDisplay = birthDateForDisplay;
    }

    @Override
    public ObservationData getObservations() {
        return observations;
    }

    @Override
    public void setObservations(ObservationData observations) {
        this.observations = observations;
    }

    @Override
    public ProjectData getProjectData() {
        return projectData;
    }

    @Override
    public void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
    }

    @Override
    public ElectronicOrder getElectronicOrder() {
        return electronicOrder;
    }

    @Override
    public void setElectronicOrder(ElectronicOrder electronicOrder) {
        this.electronicOrder = electronicOrder;
    }

    @Override
    public String getPatientFhirUuid() {
        return patientFhirUuid;
    }

    @Override
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
