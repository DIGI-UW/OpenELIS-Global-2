package org.openelisglobal.sample.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientClinicalInfo;
import org.openelisglobal.patient.action.bean.PatientEnhancedSearch;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.validation.annotations.ValidDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SamplePatientEntryForm extends BaseForm {

    public interface SamplePatientEntryBatch {
    }

    public interface SamplePatientEntry {
    }

    private Boolean rememberSiteAndRequester;

    @ValidDate(relative = DateRelation.TODAY, groups = { SamplePatientEntry.class, SamplePatientEntryBatch.class })
    private String currentDate = "";

    @Valid
    private List<Project> projects;

    private boolean customNotificationLogic;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> patientEmailNotificationTestIds;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> patientSMSNotificationTestIds;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> providerEmailNotificationTestIds;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> providerSMSNotificationTestIds;

    private PatientUpdateStatus patientUpdateStatus = PatientUpdateStatus.ADD;

    private List<ReferralItem> referralItems;

    // for display
    private List<IdValuePair> referralOrganizations;

    // for display
    private List<IdValuePair> referralReasons;

    // for display
    private List<IdValuePair> sampleTypes;

    // in validator
    private String sampleXML = "";

    @Valid
    private PatientManagementInfo patientProperties;

    // for display
    private PatientSearch patientSearch;

    // for display
    private PatientEnhancedSearch patientEnhancedSearch;

    @Valid
    private PatientClinicalInfo patientClinicalProperties;

    @Valid
    private SampleOrderItem sampleOrderItems;

    // for display
    private List<IdValuePair> initialSampleConditionList;

    // for display
    private List<IdValuePair> sampleNatureList;

    // for display
    private List<IdValuePair> testSectionList;

    @NotNull(groups = { SamplePatientEntry.class })
    private Boolean warning = false;

    private boolean useReferral;

    // for display
    private List<IdValuePair> rejectReasonList;

    public SamplePatientEntryForm() {
        setFormName("samplePatientEntryForm");
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
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

    public PatientUpdateStatus getPatientUpdateStatus() {
        return patientUpdateStatus;
    }

    public void setPatientUpdateStatus(PatientUpdateStatus patientUpdateStatus) {
        this.patientUpdateStatus = patientUpdateStatus;
    }

    public List<IdValuePair> getSampleTypes() {
        return sampleTypes;
    }

    public void setSampleTypes(List<IdValuePair> sampleTypes) {
        this.sampleTypes = sampleTypes;
    }

    public String getSampleXML() {
        return sampleXML;
    }

    public void setSampleXML(String sampleXML) {
        this.sampleXML = sampleXML;
    }

    public PatientManagementInfo getPatientProperties() {
        return patientProperties;
    }

    public void setPatientProperties(PatientManagementInfo patientProperties) {
        this.patientProperties = patientProperties;
    }

    public PatientSearch getPatientSearch() {
        return patientSearch;
    }

    public void setPatientSearch(PatientSearch patientSearch) {
        this.patientSearch = patientSearch;
    }

    public PatientEnhancedSearch getPatientEnhancedSearch() {
        return patientEnhancedSearch;
    }

    public void setPatientEnhancedSearch(PatientEnhancedSearch patientEnhancedSearch) {
        this.patientEnhancedSearch = patientEnhancedSearch;
    }

    public PatientClinicalInfo getPatientClinicalProperties() {
        return patientClinicalProperties;
    }

    public void setPatientClinicalProperties(PatientClinicalInfo patientClinicalProperties) {
        this.patientClinicalProperties = patientClinicalProperties;
    }

    public SampleOrderItem getSampleOrderItems() {
        return sampleOrderItems;
    }

    public void setSampleOrderItems(SampleOrderItem sampleOrderItems) {
        this.sampleOrderItems = sampleOrderItems;
    }

    public List<IdValuePair> getInitialSampleConditionList() {
        return initialSampleConditionList;
    }

    public void setInitialSampleConditionList(List<IdValuePair> initialSampleConditionList) {
        this.initialSampleConditionList = initialSampleConditionList;
    }

    public List<IdValuePair> getTestSectionList() {
        return testSectionList;
    }

    public void setTestSectionList(List<IdValuePair> testSectionList) {
        this.testSectionList = testSectionList;
    }

    public Boolean getWarning() {
        return warning;
    }

    public void setWarning(Boolean warning) {
        this.warning = warning;
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

    public List<ReferralItem> getReferralItems() {
        return referralItems;
    }

    public void setReferralItems(List<ReferralItem> referralItems) {
        this.referralItems = referralItems;
    }

    public List<IdValuePair> getReferralOrganizations() {
        return referralOrganizations;
    }

    public void setReferralOrganizations(List<IdValuePair> referralOrganizations) {
        this.referralOrganizations = referralOrganizations;
    }

    public List<IdValuePair> getReferralReasons() {
        return referralReasons;
    }

    public void setReferralReasons(List<IdValuePair> referralReasons) {
        this.referralReasons = referralReasons;
    }

    public boolean getUseReferral() {
        return useReferral;
    }

    public void setUseReferral(boolean useReferral) {
        this.useReferral = useReferral;
    }

    public List<IdValuePair> getRejectReasonList() {
        return rejectReasonList;
    }

    public void setRejectReasonList(List<IdValuePair> rejectReasonList) {
        this.rejectReasonList = rejectReasonList;
    }

    public Boolean getRememberSiteAndRequester() {
        return rememberSiteAndRequester;
    }

    public void setRememberSiteAndRequester(Boolean rememberSiteAndRequester) {
        this.rememberSiteAndRequester = rememberSiteAndRequester;
    }
}
