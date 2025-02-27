package org.openelisglobal.barcode.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.form.BaseForm;
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
import org.openelisglobal.samplebatchentry.form.SampleBatchEntryForm.SampleBatchEntrySetup;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidTime;

// values used for fetching, tight validation not needed
public class PrintBarcodeForm extends BaseForm {
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String accessionNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientId;

    @ValidDate(relative = DateRelation.TODAY)
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

    private List<IdValuePair> sampleNatureList;

    private String startingAtAccession;

    public PrintBarcodeForm() {
        setFormName("PrintBarcodeForm");
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getCurrentDate() {
        return currentDate;
    }

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

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
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

    public boolean isPatientInfoCheck() {
        return patientInfoCheck;
    }

    public void setPatientInfoCheck(boolean patientInfoCheck) {
        this.patientInfoCheck = patientInfoCheck;
    }

    public boolean isFacilityIDCheck() {
        return facilityIDCheck;
    }

    public void setFacilityIDCheck(boolean facilityIDCheck) {
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

    public List<IdValuePair> getSampleNatureList() {
        return sampleNatureList;
    }

    public void setSampleNatureList(List<IdValuePair> sampleNatureList) {
        this.sampleNatureList = sampleNatureList;
    }

    public String getStartingAtAccession() {
        return startingAtAccession;
    }

    public void setStartingAtAccession(String startingAtAccession) {
        this.startingAtAccession = startingAtAccession;
    }
}
