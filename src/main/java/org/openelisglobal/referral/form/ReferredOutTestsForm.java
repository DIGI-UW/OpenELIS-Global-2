package org.openelisglobal.referral.form;

import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.patient.action.bean.PatientSearch;
import org.openelisglobal.referral.action.beanitems.ReferralDisplayItem;

public class ReferredOutTestsForm extends BaseForm {

    private static final long serialVersionUID = 4554404181763445892L;

    public enum ReferDateType {
        SENT, RESULT
    }

    public enum SearchType {
        TEST_AND_DATES, LAB_NUMBER, PATIENT
    }

    public interface ReferredOut {
    }

    private SearchType searchType;

    @Valid
    private List<ReferralDisplayItem> referralDisplayItems;

    private ReferDateType dateType;

    private String startDate;

    private String endDate;

    private List<IdValuePair> testUnitSelectionList;

    private List<String> testUnitIds;

    private List<IdValuePair> testSelectionList;

    private List<String> testIds;

    private String labNumber;

    private PatientSearch patientSearch = new PatientSearch();

    private String selPatient;

    private List<String> analysisIds;

    private boolean searchFinished;

    public ReferredOutTestsForm() {
        setFormName("referredOutTestsForm");
    }

    public List<ReferralDisplayItem> getReferralDisplayItems() {
        return referralDisplayItems;
    }

    public void setReferralDisplayItems(List<ReferralDisplayItem> referralDisplayItems) {
        this.referralDisplayItems = referralDisplayItems;
    }

    public PatientSearch getPatientSearch() {
        return patientSearch;
    }

    public void setPatientSearch(PatientSearch patientSearch) {
        this.patientSearch = patientSearch;
    }

    public ReferDateType getDateType() {
        return dateType;
    }

    public void setDateType(ReferDateType dateType) {
        this.dateType = dateType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<IdValuePair> getTestUnitSelectionList() {
        return testUnitSelectionList;
    }

    public void setTestUnitSelectionList(List<IdValuePair> testUnitSelectionList) {
        this.testUnitSelectionList = testUnitSelectionList;
    }

    public List<String> getTestUnitIds() {
        return testUnitIds;
    }

    public void setTestUnitIds(List<String> testUnitIds) {
        this.testUnitIds = testUnitIds;
    }

    public List<IdValuePair> getTestSelectionList() {
        return testSelectionList;
    }

    public void setTestSelectionList(List<IdValuePair> testSelectionList) {
        this.testSelectionList = testSelectionList;
    }

    public List<String> getTestIds() {
        return testIds;
    }

    public void setTestIds(List<String> testIds) {
        this.testIds = testIds;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public String getSelPatient() {
        return selPatient;
    }

    public void setSelPatient(String selPatient) {
        this.selPatient = selPatient;
    }

    public List<String> getAnalysisIds() {
        return analysisIds;
    }

    public void setAnalysisIds(List<String> analysisIds) {
        this.analysisIds = analysisIds;
    }

    public boolean isSearchFinished() {
        return searchFinished;
    }

    public void setSearchFinished(boolean searchFinished) {
        this.searchFinished = searchFinished;
    }
}
