package org.openelisglobal.sample.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.SafeHtml.SafeListLevel;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.annotations.ValidTime;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class SampleTbEntryForm extends BaseForm {

    private static final long serialVersionUID = 1L;

    private Boolean rememberSiteAndRequester;

    private String labnoForSearch;

    private String sysUserId;

    @ValidDate(relative = DateRelation.TODAY)
    private String currentDate = "";

    // for display
    private List<IdValuePair> referralOrganizations;

    private List<IdValuePair> genders;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbOrderReason;

    private List<IdValuePair> tbOrderReasons;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbDiagnosticReason;

    private List<IdValuePair> tbDiagnosticReasons;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbFollowupReason;

    private List<IdValuePair> tbFollowupReasons;

    @SafeHtml(level = SafeListLevel.NONE)
    private String tbFollowupPeriodLine1;

    @SafeHtml(level = SafeListLevel.NONE)
    private String tbFollowupPeriodLine2;

    private List<IdValuePair> tbFollowupPeriodsLine1;

    private List<IdValuePair> tbFollowupPeriodsLine2;

    private List<IdValuePair> tbDiagnosticMethods;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbAspect;

    private List<IdValuePair> tbAspects;

    // for display
    private List<IdValuePair> sampleTypes;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String patientPK;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String guid;
    // private UUID fhirUuid;

    @NotBlank()
    @Pattern(regexp = ValidationHelper.GENDER_REGEX)
    private String patientGender;
    // ages are display only
    private String patientAge;

    @ValidDate(relative = DateRelation.PAST)
    private String patientBirthDate = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String requesterName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String externalOrderNumber;

    @NotBlank()
    @ValidAccessionNumber()
    private String labNo;

    @ValidDate(relative = DateRelation.PAST)
    private String requestDate;

    @NotEmpty()
    private List<String> newSelectedTests;

    @NotBlank()
    private String selectedTbMethod;

    // for updates
    private List<String> selectedTestToRemove;

    // for updates
    private String selectedMethodToRemove;

    @NotBlank()
    @ValidDate(relative = DateRelation.PAST)
    private String receivedDate;

    @ValidTime()
    private String receivedTime;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String referringPatientNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String referringSiteCode;

    @SafeHtml()
    private String referringSiteName;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String providerPersonId;

    @ValidName(nameType = NameType.FIRST_NAME)
    private String providerFirstName;

    @ValidName(nameType = NameType.LAST_NAME)
    private String providerLastName;

    @ValidName(nameType = NameType.FIRST_NAME)
    private String patientFirstName;

    @ValidName(nameType = NameType.LAST_NAME)
    private String patientLastName;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    private String patientPhone;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientAddress;

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX)
    private String tbSubjectNumber;

    @NotNull()
    private Boolean modified = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String sampleId;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String tbSpecimenNature;

    private List<IdValuePair> tbSpecimenNatures;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String rejectReason;

    private boolean readOnly = false;

    // for display
    private List<IdValuePair> testSectionList;

    // for display
    private List<IdValuePair> rejectReasonList;

    public SampleTbEntryForm() {
        setFormName("sampleTbEntryForm");
    }

    public Boolean getRememberSiteAndRequester() {
        return rememberSiteAndRequester;
    }

    public void setRememberSiteAndRequester(Boolean rememberSiteAndRequester) {
        this.rememberSiteAndRequester = rememberSiteAndRequester;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public List<IdValuePair> getReferralOrganizations() {
        return referralOrganizations;
    }

    public void setReferralOrganizations(List<IdValuePair> referralOrganizations) {
        this.referralOrganizations = referralOrganizations;
    }

    public List<IdValuePair> getSampleTypes() {
        return sampleTypes;
    }

    public void setSampleTypes(List<IdValuePair> sampleTypes) {
        this.sampleTypes = sampleTypes;
    }

    public String getPatientPK() {
        return patientPK;
    }

    public void setPatientPK(String patientPK) {
        this.patientPK = patientPK;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getExternalOrderNumber() {
        return externalOrderNumber;
    }

    public void setExternalOrderNumber(String externalOrderNumber) {
        this.externalOrderNumber = externalOrderNumber;
    }

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    public String getReferringPatientNumber() {
        return referringPatientNumber;
    }

    public void setReferringPatientNumber(String referringPatientNumber) {
        this.referringPatientNumber = referringPatientNumber;
    }

    public String getReferringSiteCode() {
        return referringSiteCode;
    }

    public void setReferringSiteCode(String referringSiteCode) {
        this.referringSiteCode = referringSiteCode;
    }

    public String getReferringSiteName() {
        return referringSiteName;
    }

    public void setReferringSiteName(String referringSiteName) {
        this.referringSiteName = referringSiteName;
    }

    public String getProviderPersonId() {
        return providerPersonId;
    }

    public void setProviderPersonId(String providerPersonId) {
        this.providerPersonId = providerPersonId;
    }

    public String getProviderFirstName() {
        return providerFirstName;
    }

    public void setProviderFirstName(String providerFirstName) {
        this.providerFirstName = providerFirstName;
    }

    public String getProviderLastName() {
        return providerLastName;
    }

    public void setProviderLastName(String providerLastName) {
        this.providerLastName = providerLastName;
    }

    public Boolean getModified() {
        return modified;
    }

    public void setModified(Boolean modified) {
        this.modified = modified;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public List<IdValuePair> getTestSectionList() {
        return testSectionList;
    }

    public void setTestSectionList(List<IdValuePair> testSectionList) {
        this.testSectionList = testSectionList;
    }

    public List<IdValuePair> getRejectReasonList() {
        return rejectReasonList;
    }

    public void setRejectReasonList(List<IdValuePair> rejectReasonList) {
        this.rejectReasonList = rejectReasonList;
    }

    public List<IdValuePair> getGenders() {
        return genders;
    }

    public void setGenders(List<IdValuePair> genders) {
        this.genders = genders;
    }

    public String getTbOrderReason() {
        return tbOrderReason;
    }

    public void setTbOrderReason(String tbOrderReason) {
        this.tbOrderReason = tbOrderReason;
    }

    public List<IdValuePair> getTbOrderReasons() {
        return tbOrderReasons;
    }

    public void setTbOrderReasons(List<IdValuePair> tbOrderReasons) {
        this.tbOrderReasons = tbOrderReasons;
    }

    public String getTbDiagnosticReason() {
        return tbDiagnosticReason;
    }

    public void setTbDiagnosticReason(String tbDiagnosticReason) {
        this.tbDiagnosticReason = tbDiagnosticReason;
    }

    public List<IdValuePair> getTbDiagnosticReasons() {
        return tbDiagnosticReasons;
    }

    public void setTbDiagnosticReasons(List<IdValuePair> tbDiagnosticReasons) {
        this.tbDiagnosticReasons = tbDiagnosticReasons;
    }

    public String getTbFollowupReason() {
        return tbFollowupReason;
    }

    public void setTbFollowupReason(String tbFollowupReason) {
        this.tbFollowupReason = tbFollowupReason;
    }

    public List<IdValuePair> getTbFollowupReasons() {
        return tbFollowupReasons;
    }

    public void setTbFollowupReasons(List<IdValuePair> tbFollowupReasons) {
        this.tbFollowupReasons = tbFollowupReasons;
    }

    public List<IdValuePair> getTbDiagnosticMethods() {
        return tbDiagnosticMethods;
    }

    public void setTbDiagnosticMethods(List<IdValuePair> tbDiagnosticMethods) {
        this.tbDiagnosticMethods = tbDiagnosticMethods;
    }

    public String getTbAspect() {
        return tbAspect;
    }

    public void setTbAspect(String tbAspect) {
        this.tbAspect = tbAspect;
    }

    public List<IdValuePair> getTbAspects() {
        return tbAspects;
    }

    public void setTbAspects(List<IdValuePair> tbAspects) {
        this.tbAspects = tbAspects;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientBirthDate() {
        return patientBirthDate;
    }

    public void setPatientBirthDate(String patientBirthDate) {
        this.patientBirthDate = patientBirthDate;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public String getTbSubjectNumber() {
        return tbSubjectNumber;
    }

    public void setTbSubjectNumber(String tbSubjectNumber) {
        this.tbSubjectNumber = tbSubjectNumber;
    }

    public String getTbSpecimenNature() {
        return tbSpecimenNature;
    }

    public void setTbSpecimenNature(String tbSpecimenNature) {
        this.tbSpecimenNature = tbSpecimenNature;
    }

    public List<IdValuePair> getTbSpecimenNatures() {
        return tbSpecimenNatures;
    }

    public void setTbSpecimenNatures(List<IdValuePair> tbSpecimenNatures) {
        this.tbSpecimenNatures = tbSpecimenNatures;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getTbFollowupPeriodLine1() {
        return tbFollowupPeriodLine1;
    }

    public void setTbFollowupPeriodLine1(String tbFollowupPeriodLine1) {
        this.tbFollowupPeriodLine1 = tbFollowupPeriodLine1;
    }

    public String getTbFollowupPeriodLine2() {
        return tbFollowupPeriodLine2;
    }

    public void setTbFollowupPeriodLine2(String tbFollowupPeriodLine2) {
        this.tbFollowupPeriodLine2 = tbFollowupPeriodLine2;
    }

    public List<IdValuePair> getTbFollowupPeriodsLine1() {
        return tbFollowupPeriodsLine1;
    }

    public void setTbFollowupPeriodsLine1(List<IdValuePair> tbFollowupPeriodsLine1) {
        this.tbFollowupPeriodsLine1 = tbFollowupPeriodsLine1;
    }

    public List<IdValuePair> getTbFollowupPeriodsLine2() {
        return tbFollowupPeriodsLine2;
    }

    public void setTbFollowupPeriodsLine2(List<IdValuePair> tbFollowupPeriodsLine2) {
        this.tbFollowupPeriodsLine2 = tbFollowupPeriodsLine2;
    }

    public List<String> getNewSelectedTests() {
        return newSelectedTests;
    }

    public void setNewSelectedTests(List<String> newSelectedTests) {
        this.newSelectedTests = newSelectedTests;
    }

    public List<String> getSelectedTestToRemove() {
        return selectedTestToRemove;
    }

    public void setSelectedTestToRemove(List<String> selectedTestToRemove) {
        this.selectedTestToRemove = selectedTestToRemove;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public String getSelectedTbMethod() {
        return selectedTbMethod;
    }

    public void setSelectedTbMethod(String selectedTbMethod) {
        this.selectedTbMethod = selectedTbMethod;
    }

    public String getSelectedMethodToRemove() {
        return selectedMethodToRemove;
    }

    public void setSelectedMethodToRemove(String selectedMethodToRemove) {
        this.selectedMethodToRemove = selectedMethodToRemove;
    }

    public String getLabnoForSearch() {
        return labnoForSearch;
    }

    public void setLabnoForSearch(String labnoForSearch) {
        this.labnoForSearch = labnoForSearch;
    }
}
