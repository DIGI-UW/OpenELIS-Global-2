package org.openelisglobal.reports.form;

import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.reports.action.implementation.ReportSpecificationList;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.validation.annotations.ValidDate;

// values not preserved, so security validation not a large concern
// values used in jasperreports
public class ReportForm extends BaseForm {

    public enum DateType {
        RESULT_DATE("report.label.datetype.resultdate"), ORDER_DATE("report.label.datetype.orderdate"),
        PRINT_DATE("report.label.datetype.printdate");

        String messageKey;

        DateType(String messageKey) {
            this.messageKey = messageKey;
        }

        public String getMessageKey() {
            return this.messageKey;
        }
    }

    public enum ReceptionTime {
        NORMAL_WORK_HOURS, OUT_OF_NORMAL_WORK_HOURS;
    }

    private boolean noRequestSpecifications = false;

    private String type = "";

    private String reportName = "";

    private String report = "glen";

    private boolean useAccessionDirect = false;

    private String accessionDirect = "";

    // private Collection selectedHighAccession;

    private boolean useHighAccessionDirect = false;

    private String highAccessionDirect = "";

    // private Collection patientNumberList;

    private boolean usePatientNumberDirect = false;

    private String patientNumberDirect = "";

    private boolean useUpperPatientNumberDirect = false;

    private String patientUpperNumberDirect = "";

    private boolean useLowerDateRange = false;

    @ValidDate
    private String lowerDateRange = "";

    private boolean useUpperDateRange = false;

    @ValidDate
    private String upperDateRange = "";

    private boolean useLocationCode = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String locationCode = "";

    private List<Organization> locationCodeList;

    private boolean useProjectCode = false;

    private boolean useDashboard = false;

    private boolean useExportDateType = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String projectCode = "";

    private String vlStudyType = "";

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> analysisIds;

    private boolean usePredefinedDateRanges = false;

    @Pattern(regexp = "^$|^year$|^months3$|^months6$|^months12$|^custom$")
    private String datePeriod = "";

    // for display
    private List<IdValuePair> monthList;

    // for display
    private List<IdValuePair> yearList;

    @Pattern(regexp = "^[0-9]*$")
    private String lowerMonth = "";

    @Pattern(regexp = "^[0-9]*$")
    private String lowerYear = "";

    @Pattern(regexp = "^[0-9]*$")
    private String upperMonth = "";

    @Pattern(regexp = "^[0-9]*$")
    private String upperYear = "";

    private ReportSpecificationList selectList;

    private List<Project> projectCodeList;

    private String instructions = "";

    private Integer experimentId;

    private boolean usePatientSearch;

    private PatientSearch patientSearch;

    private String selPatient;

    private boolean useSiteSearch;

    private boolean useArvOrganizationSearch;

    private String referringSiteId;

    private List<IdValuePair> referringSiteList;

    private List<Organization> arvOrganizationList;

    private String arvSiteId;

    private List<IdValuePair> arvSiteList;

    private String referringSiteDepartmentId;

    private List<IdValuePair> referringSiteDepartmentList;

    private boolean onlyResults;

    private DateType dateType;

    private List<OrderPriority> priority;

    private List<ReceptionTime> receptionTime;

    private List<String> labSections;

    private Boolean useStatisticsParams = false;

    private String programSampleId;

    private String erPercent;

    private String erIntensity;

    private String erScore;

    private String prPercent;

    private String prIntensity;

    private String prScore;

    private String mib;

    private String pattern;

    private String herAssesment;

    private String herScore;

    private String diagnosis;

    private String molecularSubType;

    private String conclusion;

    private String ihcScore;

    private String ihcRatio;

    private String averageChrom;

    private String averageHer2;

    private String numberOfcancerNuclei;

    private List<String> codedConclusions;

    // for display
    private List<IdValuePair> priorityList;

    // for display
    private List<IdValuePair> receptionTimeList;

    public ReportForm() {
        setFormName("ReportForm");
    }

    public boolean getNoRequestSpecifications() {
        return noRequestSpecifications;
    }

    public void setNoRequestSpecifications(boolean noRequestSpecifications) {
        this.noRequestSpecifications = noRequestSpecifications;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public boolean getUseAccessionDirect() {
        return useAccessionDirect;
    }

    public void setUseAccessionDirect(boolean useAccessionDirect) {
        this.useAccessionDirect = useAccessionDirect;
    }

    public String getAccessionDirect() {
        return accessionDirect;
    }

    public String getAccessionDirectNoSuffix() {
        if (accessionDirect == null || !accessionDirect.contains(".")) {
            return accessionDirect;
        } else {
            return accessionDirect.substring(0, accessionDirect.indexOf('.'));
        }
    }

    public void setAccessionDirect(String accessionDirect) {
        this.accessionDirect = accessionDirect;
    }

    /*
     * public Collection getSelectedHighAccession() { return selectedHighAccession;
     * }
     *
     * public void setSelectedHighAccession(Collection selectedHighAccession) {
     * this.selectedHighAccession = selectedHighAccession; }
     */

    public boolean getUseHighAccessionDirect() {
        return useHighAccessionDirect;
    }

    public void setUseHighAccessionDirect(boolean useHighAccessionDirect) {
        this.useHighAccessionDirect = useHighAccessionDirect;
    }

    public String getHighAccessionDirect() {
        return highAccessionDirect;
    }

    public String getHighAccessionDirectNoSuffix() {
        if (highAccessionDirect == null || !highAccessionDirect.contains(".")) {
            return highAccessionDirect;
        } else {
            return highAccessionDirect.substring(0, highAccessionDirect.indexOf('.'));
        }
    }

    public void setHighAccessionDirect(String highAccessionDirect) {
        this.highAccessionDirect = highAccessionDirect;
    }

    /*
     * public Collection getPatientNumberList() { return patientNumberList; }
     *
     * public void setPatientNumberList(Collection patientNumberList) {
     * this.patientNumberList = patientNumberList; }
     */

    public boolean getUsePatientNumberDirect() {
        return usePatientNumberDirect;
    }

    public void setUsePatientNumberDirect(boolean usePatientNumberDirect) {
        this.usePatientNumberDirect = usePatientNumberDirect;
    }

    public String getPatientNumberDirect() {
        return patientNumberDirect;
    }

    public void setPatientNumberDirect(String patientNumberDirect) {
        this.patientNumberDirect = patientNumberDirect;
    }

    public boolean getUseUpperPatientNumberDirect() {
        return useUpperPatientNumberDirect;
    }

    public void setUseUpperPatientNumberDirect(boolean useUpperPatientNumberDirect) {
        this.useUpperPatientNumberDirect = useUpperPatientNumberDirect;
    }

    public String getPatientUpperNumberDirect() {
        return patientUpperNumberDirect;
    }

    public void setPatientUpperNumberDirect(String patientUpperNumberDirect) {
        this.patientUpperNumberDirect = patientUpperNumberDirect;
    }

    public boolean getUseLowerDateRange() {
        return useLowerDateRange;
    }

    public void setUseLowerDateRange(boolean useLowerDateRange) {
        this.useLowerDateRange = useLowerDateRange;
    }

    public String getLowerDateRange() {
        return lowerDateRange;
    }

    public void setLowerDateRange(String lowerDateRange) {
        this.lowerDateRange = lowerDateRange;
    }

    public boolean getUseUpperDateRange() {
        return useUpperDateRange;
    }

    public void setUseUpperDateRange(boolean useUpperDateRange) {
        this.useUpperDateRange = useUpperDateRange;
    }

    public String getUpperDateRange() {
        return upperDateRange;
    }

    public void setUpperDateRange(String upperDateRange) {
        this.upperDateRange = upperDateRange;
    }

    public boolean getUseLocationCode() {
        return useLocationCode;
    }

    public void setUseLocationCode(boolean useLocationCode) {
        this.useLocationCode = useLocationCode;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }

    public List<Organization> getLocationCodeList() {
        return locationCodeList;
    }

    public void setLocationCodeList(List<Organization> locationCodeList) {
        this.locationCodeList = locationCodeList;
    }

    public boolean getUseProjectCode() {
        return useProjectCode;
    }

    public void setUseProjectCode(boolean useProjectCode) {
        this.useProjectCode = useProjectCode;
    }

    public boolean getUseDashboard() {
        return useDashboard;
    }

    public void setUseDashboard(boolean useDashboard) {
        this.useDashboard = useDashboard;
    }

    public boolean getUseExportDateType() {
        return useExportDateType;
    }

    public void setUseExportDateType(boolean useExportDateType) {
        this.useExportDateType = useExportDateType;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getVlStudyType() {
        return vlStudyType;
    }

    public void setVlStudyType(String vlStudyType) {
        this.vlStudyType = vlStudyType;
    }

    public boolean getUsePredefinedDateRanges() {
        return usePredefinedDateRanges;
    }

    public void setUsePredefinedDateRanges(boolean usePredefinedDateRanges) {
        this.usePredefinedDateRanges = usePredefinedDateRanges;
    }

    public String getDatePeriod() {
        return datePeriod;
    }

    public void setDatePeriod(String datePeriod) {
        this.datePeriod = datePeriod;
    }

    public List<IdValuePair> getMonthList() {
        return monthList;
    }

    public void setMonthList(List<IdValuePair> monthList) {
        this.monthList = monthList;
    }

    public List<IdValuePair> getYearList() {
        return yearList;
    }

    public void setYearList(List<IdValuePair> yearList) {
        this.yearList = yearList;
    }

    public String getLowerMonth() {
        return lowerMonth;
    }

    public void setLowerMonth(String lowerMonth) {
        this.lowerMonth = lowerMonth;
    }

    public String getLowerYear() {
        return lowerYear;
    }

    public void setLowerYear(String lowerYear) {
        this.lowerYear = lowerYear;
    }

    public String getUpperMonth() {
        return upperMonth;
    }

    public void setUpperMonth(String upperMonth) {
        this.upperMonth = upperMonth;
    }

    public String getUpperYear() {
        return upperYear;
    }

    public void setUpperYear(String upperYear) {
        this.upperYear = upperYear;
    }

    public ReportSpecificationList getSelectList() {
        return selectList;
    }

    public void setSelectList(ReportSpecificationList selectList) {
        this.selectList = selectList;
    }

    public List<Project> getProjectCodeList() {
        return projectCodeList;
    }

    public void setProjectCodeList(List<Project> projectCodeList) {
        this.projectCodeList = projectCodeList;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Integer getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Integer experimentId) {
        this.experimentId = experimentId;
    }

    public boolean isUsePatientSearch() {
        return usePatientSearch;
    }

    public void setUsePatientSearch(boolean usePatientSearch) {
        this.usePatientSearch = usePatientSearch;
    }

    public PatientSearch getPatientSearch() {
        return patientSearch;
    }

    public void setPatientSearch(PatientSearch patientSearch) {
        this.patientSearch = patientSearch;
    }

    public String getSelPatient() {
        return selPatient;
    }

    public void setSelPatient(String selPatient) {
        this.selPatient = selPatient;
    }

    public List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> getAnalysisIds() {
        return analysisIds;
    }

    public void setAnalysisIds(List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> analysisIds) {
        this.analysisIds = analysisIds;
    }

    public boolean isUseSiteSearch() {
        return useSiteSearch;
    }

    public void setUseSiteSearch(boolean useSiteSearch) {
        this.useSiteSearch = useSiteSearch;
    }

    public List<IdValuePair> getReferringSiteList() {
        return referringSiteList;
    }

    public void setReferringSiteList(List<IdValuePair> referringSiteList) {
        this.referringSiteList = referringSiteList;
    }

    public String getReferringSiteId() {
        return referringSiteId;
    }

    public void setReferringSiteId(String referringSiteId) {
        this.referringSiteId = referringSiteId;
    }

    public String getReferringSiteDepartmentId() {
        return referringSiteDepartmentId;
    }

    public void setReferringSiteDepartmentId(String referringSiteDepartmentId) {
        this.referringSiteDepartmentId = referringSiteDepartmentId;
    }

    public List<IdValuePair> getReferringSiteDepartmentList() {
        return referringSiteDepartmentList;
    }

    public void setReferringSiteDepartmentList(List<IdValuePair> referringSiteDepartmentList) {
        this.referringSiteDepartmentList = referringSiteDepartmentList;
    }

    public boolean isOnlyResults() {
        return onlyResults;
    }

    public void setOnlyResults(boolean onlyResults) {
        this.onlyResults = onlyResults;
    }

    public DateType getDateType() {
        return dateType;
    }

    public void setDateType(DateType dateType) {
        this.dateType = dateType;
    }

    public List<OrderPriority> getPriority() {
        return priority;
    }

    public void setPriority(List<OrderPriority> priority) {
        this.priority = priority;
    }

    public List<String> getLabSections() {
        return labSections;
    }

    public void setLabSections(List<String> labSections) {
        this.labSections = labSections;
    }

    public List<IdValuePair> getPriorityList() {
        return priorityList;
    }

    public void setPriorityList(List<IdValuePair> priorityList) {
        this.priorityList = priorityList;
    }

    public List<IdValuePair> getReceptionTimeList() {
        return receptionTimeList;
    }

    public void setReceptionTimeList(List<IdValuePair> receptionTimeList) {
        this.receptionTimeList = receptionTimeList;
    }

    public Boolean getUseStatisticsParams() {
        return useStatisticsParams;
    }

    public void setUseStatisticsParams(Boolean useStatisticsParams) {
        this.useStatisticsParams = useStatisticsParams;
    }

    public List<ReceptionTime> getReceptionTime() {
        return receptionTime;
    }

    public void setReceptionTime(List<ReceptionTime> receptionTime) {
        this.receptionTime = receptionTime;
    }

    public String getProgramSampleId() {
        return programSampleId;
    }

    public void setProgramSampleId(String programSampleId) {
        this.programSampleId = programSampleId;
    }

    public String getErPercent() {
        return erPercent;
    }

    public void setErPercent(String erPercent) {
        this.erPercent = erPercent;
    }

    public String getErIntensity() {
        return erIntensity;
    }

    public void setErIntensity(String erIntensity) {
        this.erIntensity = erIntensity;
    }

    public String getErScore() {
        return erScore;
    }

    public void setErScore(String erScore) {
        this.erScore = erScore;
    }

    public String getPrPercent() {
        return prPercent;
    }

    public void setPrPercent(String prPercent) {
        this.prPercent = prPercent;
    }

    public String getPrIntensity() {
        return prIntensity;
    }

    public void setPrIntensity(String prIntensity) {
        this.prIntensity = prIntensity;
    }

    public String getPrScore() {
        return prScore;
    }

    public void setPrScore(String prScore) {
        this.prScore = prScore;
    }

    public String getMib() {
        return mib;
    }

    public void setMib(String mib) {
        this.mib = mib;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getHerScore() {
        return herScore;
    }

    public void setHerScore(String herScore) {
        this.herScore = herScore;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getMolecularSubType() {
        return molecularSubType;
    }

    public void setMolecularSubType(String molecularSubType) {
        this.molecularSubType = molecularSubType;
    }

    public String getHerAssesment() {
        return herAssesment;
    }

    public void setHerAssesment(String herAssesment) {
        this.herAssesment = herAssesment;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getIhcScore() {
        return ihcScore;
    }

    public void setIhcScore(String ihcScore) {
        this.ihcScore = ihcScore;
    }

    public String getIhcRatio() {
        return ihcRatio;
    }

    public void setIhcRatio(String ihcRatio) {
        this.ihcRatio = ihcRatio;
    }

    public String getAverageChrom() {
        return averageChrom;
    }

    public void setAverageChrom(String averageChrom) {
        this.averageChrom = averageChrom;
    }

    public String getAverageHer2() {
        return averageHer2;
    }

    public void setAverageHer2(String averageHer2) {
        this.averageHer2 = averageHer2;
    }

    public String getNumberOfcancerNuclei() {
        return numberOfcancerNuclei;
    }

    public void setNumberOfcancerNuclei(String numberOfcancerNuclei) {
        this.numberOfcancerNuclei = numberOfcancerNuclei;
    }

    public String getArvSiteId() {
        return arvSiteId;
    }

    public void setArvSiteId(String arvSiteId) {
        this.arvSiteId = arvSiteId;
    }

    public List<IdValuePair> getArvSiteList() {
        return arvSiteList;
    }

    public void setArvSiteList(List<IdValuePair> arvSiteList) {
        this.arvSiteList = arvSiteList;
    }

    public boolean isUseArvOrganizationSearch() {
        return useArvOrganizationSearch;
    }

    public void setUseArvOrganizationSearch(boolean useArvOrganizationSearch) {
        this.useArvOrganizationSearch = useArvOrganizationSearch;
    }

    public List<Organization> getArvOrganizationList() {
        return arvOrganizationList;
    }

    public void setArvOrganizationList(List<Organization> arvOrganizationList) {
        this.arvOrganizationList = arvOrganizationList;
    }

    public List<String> getCodedConclusions() {
        return codedConclusions;
    }

    public void setCodedConclusions(List<String> codedConclusions) {
        this.codedConclusions = codedConclusions;
    }

}
