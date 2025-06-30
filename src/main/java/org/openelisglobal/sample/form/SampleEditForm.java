package org.openelisglobal.sample.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.sample.bean.SampleEditItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class SampleEditForm extends BaseForm {

    public interface SampleEdit {
    }

    @NotNull(groups = { SampleEdit.class })
    private Boolean noSampleFound = Boolean.FALSE;

    @NotNull(groups = { SampleEdit.class })
    private Boolean isConfirmationSample = Boolean.FALSE;

    @NotNull(groups = { SampleEdit.class })
    private Boolean isEditable = Boolean.TRUE;

    @ValidName(nameType = NameType.FULL_NAME, groups = { SampleEdit.class })
    private String patientName = "";

    @ValidDate(relative = DateRelation.PAST)
    private String dob = "";

    @Pattern(regexp = ValidationHelper.GENDER_REGEX, groups = { SampleEdit.class })
    private String gender = "";

    @Pattern(regexp = ValidationHelper.PATIENT_ID_REGEX, groups = { SampleEdit.class })
    private String nationalId = "";

    @ValidAccessionNumber(groups = { SampleEdit.class }, searchValue = true)
    private String accessionNumber;

    @ValidAccessionNumber(groups = { SampleEdit.class })
    private String newAccessionNumber = "";

    @Valid
    private List<SampleEditItem> existingTests;

    @Valid
    private List<SampleEditItem> possibleTests;

    @NotNull(groups = { SampleEdit.class })
    private Boolean searchFinished = false;

    // for display
    private List<IdValuePair> sampleTypes;

    // in validator
    private String maxAccessionNumber = "";

    // in validator
    private String sampleXML = "";

    // for display
    private List<IdValuePair> initialSampleConditionList;

    // for display
    private List<IdValuePair> sampleNatureList;

    @ValidDate(relative = DateRelation.TODAY, groups = { SampleEdit.class })
    private String currentDate = "";

    // for display
    private List<IdValuePair> testSectionList;

    // for display
    private List<IdValuePair> rejectReasonList;

    @Valid
    private SampleOrderItem sampleOrderItems;

    // for display
    private PatientSearch patientSearch;

    @NotNull(groups = { SampleEdit.class })
    private Boolean ableToCancelResults = false;

    @NotNull(groups = { SampleEdit.class })
    private Boolean warning = false;

    // in validator
    private String idSeparator = ConfigurationProperties.getInstance().getPropertyValue("default.idSeparator");

    // in validator
    private String accessionFormat = ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat);

    // in validator
    private int editableAccession = AccessionNumberUtil.getChangeableLength();

    // in validator
    private int nonEditableAccession = AccessionNumberUtil.getInvarientLength();

    // in validator
    private int maxAccessionLength = editableAccession + nonEditableAccession;

    private boolean customNotificationLogic;
    private List<String> patientEmailNotificationTestIds;
    private List<String> patientSMSNotificationTestIds;
    private List<String> providerEmailNotificationTestIds;
    private List<String> providerSMSNotificationTestIds;

    public SampleEditForm() {
        setFormName("SampleEditForm");
    }

    public Boolean getNoSampleFound() {
        return noSampleFound;
    }

    public void setNoSampleFound(Boolean noSampleFound) {
        this.noSampleFound = noSampleFound;
    }

    public Boolean getIsConfirmationSample() {
        return isConfirmationSample;
    }

    public void setIsConfirmationSample(Boolean isConfirmationSample) {
        this.isConfirmationSample = isConfirmationSample;
    }

    public Boolean getIsEditable() {
        return isEditable;
    }

    public void setIsEditable(Boolean isEditable) {
        this.isEditable = isEditable;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getNewAccessionNumber() {
        return newAccessionNumber;
    }

    public void setNewAccessionNumber(String newAccessionNumber) {
        this.newAccessionNumber = newAccessionNumber;
    }

    public List<SampleEditItem> getExistingTests() {
        return existingTests;
    }

    public void setExistingTests(List<SampleEditItem> existingTests) {
        this.existingTests = existingTests;
    }

    public List<SampleEditItem> getPossibleTests() {
        return possibleTests;
    }

    public void setPossibleTests(List<SampleEditItem> possibleTests) {
        this.possibleTests = possibleTests;
    }

    public Boolean getSearchFinished() {
        return searchFinished;
    }

    public void setSearchFinished(Boolean searchFinished) {
        this.searchFinished = searchFinished;
    }

    public List<IdValuePair> getSampleTypes() {
        return sampleTypes;
    }

    public void setSampleTypes(List<IdValuePair> sampleTypes) {
        this.sampleTypes = sampleTypes;
    }

    public String getMaxAccessionNumber() {
        return maxAccessionNumber;
    }

    public void setMaxAccessionNumber(String maxAccessionNumber) {
        this.maxAccessionNumber = maxAccessionNumber;
    }

    public String getSampleXML() {
        return sampleXML;
    }

    public void setSampleXML(String sampleXML) {
        this.sampleXML = sampleXML;
    }

    public List<IdValuePair> getInitialSampleConditionList() {
        return initialSampleConditionList;
    }

    public void setInitialSampleConditionList(List<IdValuePair> initialSampleConditionList) {
        this.initialSampleConditionList = initialSampleConditionList;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public List<IdValuePair> getTestSectionList() {
        return testSectionList;
    }

    public void setTestSectionList(List<IdValuePair> testSectionList) {
        this.testSectionList = testSectionList;
    }

    public SampleOrderItem getSampleOrderItems() {
        return sampleOrderItems;
    }

    public void setSampleOrderItems(SampleOrderItem sampleOrderItems) {
        this.sampleOrderItems = sampleOrderItems;
    }

    public PatientSearch getPatientSearch() {
        return patientSearch;
    }

    public void setPatientSearch(PatientSearch patientSearch) {
        this.patientSearch = patientSearch;
    }

    public Boolean getAbleToCancelResults() {
        return ableToCancelResults;
    }

    public void setAbleToCancelResults(Boolean ableToCancelResults) {
        this.ableToCancelResults = ableToCancelResults;
    }

    public Boolean getWarning() {
        return warning;
    }

    public void setWarning(Boolean warning) {
        this.warning = warning;
    }

    public String getIdSeparator() {
        return idSeparator;
    }

    public void setIdSeparator(String idSeparator) {
        this.idSeparator = idSeparator;
    }

    public String getAccessionFormat() {
        return accessionFormat;
    }

    public void setAccessionFormat(String accessionFormat) {
        this.accessionFormat = accessionFormat;
    }

    public int getEditableAccession() {
        return editableAccession;
    }

    public void setEditableAccession(int editableAccession) {
        this.editableAccession = editableAccession;
    }

    public int getNonEditableAccession() {
        return nonEditableAccession;
    }

    public void setNonEditableAccession(int nonEditableAccession) {
        this.nonEditableAccession = nonEditableAccession;
    }

    public int getMaxAccessionLength() {
        return maxAccessionLength;
    }

    public void setMaxAccessionLength(int maxAccessionLength) {
        this.maxAccessionLength = maxAccessionLength;
    }

    public List<IdValuePair> getSampleNatureList() {
        return sampleNatureList;
    }

    public void setSampleNatureList(List<IdValuePair> sampleNatureList) {
        this.sampleNatureList = sampleNatureList;
    }

    public boolean getCustomNotificationLogic() {
        return customNotificationLogic;
    }

    public void setCustomNotificationLogic(boolean customNotificationLogic) {
        this.customNotificationLogic = customNotificationLogic;
    }

    public List<String> getPatientEmailNotificationTestIds() {
        return patientEmailNotificationTestIds;
    }

    public void setPatientEmailNotificationTestIds(List<String> patientEmailNotificationTestIds) {
        this.patientEmailNotificationTestIds = patientEmailNotificationTestIds;
    }

    public List<String> getPatientSMSNotificationTestIds() {
        return patientSMSNotificationTestIds;
    }

    public void setPatientSMSNotificationTestIds(List<String> patientSMSNotificationTestIds) {
        this.patientSMSNotificationTestIds = patientSMSNotificationTestIds;
    }

    public List<String> getProviderEmailNotificationTestIds() {
        return providerEmailNotificationTestIds;
    }

    public void setProviderEmailNotificationTestIds(List<String> providerEmailNotificationTestIds) {
        this.providerEmailNotificationTestIds = providerEmailNotificationTestIds;
    }

    public List<String> getProviderSMSNotificationTestIds() {
        return providerSMSNotificationTestIds;
    }

    public void setProviderSMSNotificationTestIds(List<String> providerSMSNotificationTestIds) {
        this.providerSMSNotificationTestIds = providerSMSNotificationTestIds;
    }

    public List<IdValuePair> getRejectReasonList() {
        return rejectReasonList;
    }

    public void setRejectReasonList(List<IdValuePair> rejectReasonList) {
        this.rejectReasonList = rejectReasonList;
    }
}
