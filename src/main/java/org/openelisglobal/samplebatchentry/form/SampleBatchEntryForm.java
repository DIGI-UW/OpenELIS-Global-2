package org.openelisglobal.samplebatchentry.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.organization.util.OrganizationTypeList;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.ProjectData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SampleBatchEntryForm extends SamplePatientEntryForm {

    public interface SampleBatchEntrySetup {
    }

    @ValidDate(relative = DateRelation.TODAY, groups = { SampleBatchEntrySetup.class })
    private String currentDate = "";

    @ValidTime(groups = { SampleBatchEntrySetup.class })
    private String currentTime = "";

    // for display
    private String project = "";

    // for display
    private List<Project> projects;

    // for display
    private List<IdValuePair> sampleTypes;

    // in validator
    private String sampleXML = "";

    @Valid
    private SampleOrderItem sampleOrderItems;

    // for display
    private List<IdValuePair> initialSampleConditionList;

    // for display
    private List<IdValuePair> sampleNatureList;

    @Override
    public List<IdValuePair> getSampleNatureList() {
        return sampleNatureList;
    }

    @Override
    public void setSampleNatureList(List<IdValuePair> sampleNatureList) {
        this.sampleNatureList = sampleNatureList;
    }

    // for display
    private List<IdValuePair> testSectionList;

    private boolean patientInfoCheck = false;

    private boolean facilityIDCheck = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SampleBatchEntrySetup.class })
    private String facilityID;

    // TODO switch to enums
    @Pattern(regexp = "^On Demand$|^Pre-Printed$", groups = { SampleBatchEntrySetup.class })
    private String method;

    // TODO switch to enums
    @Pattern(regexp = "^routine$|^viralLoad$|^EID$", groups = { SampleBatchEntrySetup.class })
    private String study;

    @ValidAccessionNumber
    private String labNo;

    @Valid
    private PatientManagementInfo patientProperties;

    /// for display
    private PatientSearch patientSearch;

    // in validator
    private String programCode;

    @Valid
    private ProjectData projectDataVL;

    @Valid
    private ProjectData projectDataEID;

    @Valid
    private ProjectData projectData;

    @Valid
    private ObservationData observations;

    // for display
    private Map<String, OrganizationTypeList> organizationTypeLists;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SampleBatchEntrySetup.class })
    private String sampleTypeSelect;

    private boolean localDBOnly;

    private boolean warning;

    public SampleBatchEntryForm() {
        setFormName("sampleBatchEntryForm");
    }

    @Override
    public String getCurrentDate() {
        return currentDate;
    }

    @Override
    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public String getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(String currentTime) {
        this.currentTime = currentTime;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    @Override
    public List<Project> getProjects() {
        return projects;
    }

    @Override
    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    @Override
    public List<IdValuePair> getSampleTypes() {
        return sampleTypes;
    }

    @Override
    public void setSampleTypes(List<IdValuePair> sampleTypes) {
        this.sampleTypes = sampleTypes;
    }

    @Override
    public String getSampleXML() {
        return sampleXML;
    }

    @Override
    public void setSampleXML(String sampleXML) {
        this.sampleXML = sampleXML;
    }

    @Override
    public SampleOrderItem getSampleOrderItems() {
        return sampleOrderItems;
    }

    @Override
    public void setSampleOrderItems(SampleOrderItem sampleOrderItems) {
        this.sampleOrderItems = sampleOrderItems;
    }

    @Override
    public List<IdValuePair> getInitialSampleConditionList() {
        return initialSampleConditionList;
    }

    @Override
    public void setInitialSampleConditionList(List<IdValuePair> initialSampleConditionList) {
        this.initialSampleConditionList = initialSampleConditionList;
    }

    @Override
    public List<IdValuePair> getTestSectionList() {
        return testSectionList;
    }

    @Override
    public void setTestSectionList(List<IdValuePair> testSectionList) {
        this.testSectionList = testSectionList;
    }

    public Boolean getPatientInfoCheck() {
        return patientInfoCheck;
    }

    public void setPatientInfoCheck(Boolean patientInfoCheck) {
        this.patientInfoCheck = patientInfoCheck;
    }

    public Boolean getFacilityIDCheck() {
        return facilityIDCheck;
    }

    public void setFacilityIDCheck(Boolean facilityIDCheck) {
        this.facilityIDCheck = facilityIDCheck;
    }

    public String getFacilityID() {
        return facilityID;
    }

    public void setFacilityID(String facilityID) {
        this.facilityID = facilityID;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStudy() {
        return study;
    }

    public void setStudy(String study) {
        this.study = study;
    }

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    @Override
    public PatientManagementInfo getPatientProperties() {
        return patientProperties;
    }

    @Override
    public void setPatientProperties(PatientManagementInfo patientProperties) {
        this.patientProperties = patientProperties;
    }

    @Override
    public PatientSearch getPatientSearch() {
        return patientSearch;
    }

    @Override
    public void setPatientSearch(PatientSearch patientSearch) {
        this.patientSearch = patientSearch;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public ProjectData getProjectDataVL() {
        return projectDataVL;
    }

    public void setProjectDataVL(ProjectData projectDataVL) {
        this.projectDataVL = projectDataVL;
    }

    public ProjectData getProjectDataEID() {
        return projectDataEID;
    }

    public void setProjectDataEID(ProjectData projectDataEID) {
        this.projectDataEID = projectDataEID;
    }

    public ProjectData getProjectData() {
        return projectData;
    }

    public void setProjectData(ProjectData projectData) {
        this.projectData = projectData;
    }

    public ObservationData getObservations() {
        return observations;
    }

    public void setObservations(ObservationData observations) {
        this.observations = observations;
    }

    public Map<String, OrganizationTypeList> getOrganizationTypeLists() {
        return organizationTypeLists;
    }

    public void setOrganizationTypeLists(Map<String, OrganizationTypeList> organizationTypeLists) {
        this.organizationTypeLists = organizationTypeLists;
    }

    public String getSampleTypeSelect() {
        return sampleTypeSelect;
    }

    public void setSampleTypeSelect(String sampleTypeSelect) {
        this.sampleTypeSelect = sampleTypeSelect;
    }

    public boolean isLocalDBOnly() {
        return localDBOnly;
    }

    public void setLocalDBOnly(boolean localDBOnly) {
        this.localDBOnly = localDBOnly;
    }

    public boolean isWarning() {
        return warning;
    }

    public void setWarning(boolean warning) {
        this.warning = warning;
    }
}
