package org.openelisglobal.result.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.result.controller.StatusResultsController.DropPair;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.validation.annotations.ValidDate;

// only used to communicate from server to client
// does not require validation
public class StatusResultsForm extends BaseForm implements ResultsPagingForm {
    private PagingBean paging;

    private Boolean singlePatient = false;

    @ValidDate
    private String currentDate = "";

    @ValidDate
    private String collectionDate = "";

    @ValidDate
    private String recievedDate = "";

    private String selectedTest = "";

    private List<IdValuePair> testSelections;

    private String selectedAnalysisStatus;

    private List<DropPair> analysisStatusSelections;

    private String selectedSampleStatus;

    private List<DropPair> sampleStatusSelections;

    private Boolean displayTestMethod = true;

    private Boolean displayTestKit = true;

    private List<TestResultItem> testResult;

    private List<InventoryKitItem> inventoryItems;

    private List<String> hivKits;

    private List<String> syphilisKits;

    private Boolean searchFinished = false;

    private String type = "";

    private List<IdValuePair> referralReasons;

    private List<IdValuePair> rejectReasons;

    private Boolean displayTestSections = false;

    private String testSectionId;

    private List<IdValuePair> referralOrganizations;

    // for display
    private List<IdValuePair> methods;

    public StatusResultsForm() {
        setFormName("StatusResultsForm");
    }

    @Override
    public PagingBean getPaging() {
        return paging;
    }

    @Override
    public void setPaging(PagingBean paging) {
        this.paging = paging;
    }

    public Boolean getSinglePatient() {
        return singlePatient;
    }

    public void setSinglePatient(Boolean singlePatient) {
        this.singlePatient = singlePatient;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getRecievedDate() {
        return recievedDate;
    }

    public void setRecievedDate(String recievedDate) {
        this.recievedDate = recievedDate;
    }

    public String getSelectedTest() {
        return selectedTest;
    }

    public void setSelectedTest(String selectedTest) {
        this.selectedTest = selectedTest;
    }

    public List<IdValuePair> getTestSelections() {
        return testSelections;
    }

    public void setTestSelections(List<IdValuePair> testSelections) {
        this.testSelections = testSelections;
    }

    public String getSelectedAnalysisStatus() {
        return selectedAnalysisStatus;
    }

    public void setSelectedAnalysisStatus(String selectedAnalysisStatus) {
        this.selectedAnalysisStatus = selectedAnalysisStatus;
    }

    public List<DropPair> getAnalysisStatusSelections() {
        return analysisStatusSelections;
    }

    public void setAnalysisStatusSelections(List<DropPair> analysisStatusSelections) {
        this.analysisStatusSelections = analysisStatusSelections;
    }

    public String getSelectedSampleStatus() {
        return selectedSampleStatus;
    }

    public void setSelectedSampleStatus(String selectedSampleStatus) {
        this.selectedSampleStatus = selectedSampleStatus;
    }

    public List<DropPair> getSampleStatusSelections() {
        return sampleStatusSelections;
    }

    public void setSampleStatusSelections(List<DropPair> sampleStatusSelections) {
        this.sampleStatusSelections = sampleStatusSelections;
    }

    public Boolean getDisplayTestMethod() {
        return displayTestMethod;
    }

    public void setDisplayTestMethod(Boolean displayTestMethod) {
        this.displayTestMethod = displayTestMethod;
    }

    public Boolean getDisplayTestKit() {
        return displayTestKit;
    }

    public void setDisplayTestKit(Boolean displayTestKit) {
        this.displayTestKit = displayTestKit;
    }

    @Override
    public List<TestResultItem> getTestResult() {
        return testResult;
    }

    @Override
    public void setTestResult(List<TestResultItem> testResult) {
        this.testResult = testResult;
    }

    public List<InventoryKitItem> getInventoryItems() {
        return inventoryItems;
    }

    public void setInventoryItems(List<InventoryKitItem> inventoryItems) {
        this.inventoryItems = inventoryItems;
    }

    public List<String> getHivKits() {
        return hivKits;
    }

    public void setHivKits(List<String> hivKits) {
        this.hivKits = hivKits;
    }

    public List<String> getSyphilisKits() {
        return syphilisKits;
    }

    public void setSyphilisKits(List<String> syphilisKits) {
        this.syphilisKits = syphilisKits;
    }

    public Boolean getSearchFinished() {
        return searchFinished;
    }

    public void setSearchFinished(Boolean searchFinished) {
        this.searchFinished = searchFinished;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<IdValuePair> getReferralReasons() {
        return referralReasons;
    }

    public void setReferralReasons(List<IdValuePair> referralReasons) {
        this.referralReasons = referralReasons;
    }

    public List<IdValuePair> getRejectReasons() {
        return rejectReasons;
    }

    public void setRejectReasons(List<IdValuePair> rejectReasons) {
        this.rejectReasons = rejectReasons;
    }

    public Boolean getDisplayTestSections() {
        return displayTestSections;
    }

    public void setDisplayTestSections(Boolean displayTestSections) {
        this.displayTestSections = displayTestSections;
    }

    @Override
    public String getTestSectionId() {
        return testSectionId;
    }

    @Override
    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public void setReferralOrganizations(List<IdValuePair> referralOrganizations) {
        this.referralOrganizations = referralOrganizations;
    }

    public List<IdValuePair> getReferralOrganizations() {
        return referralOrganizations;
    }

    public boolean getSearchByRange() {
        return false;
    }

    public List<IdValuePair> getMethods() {
        return methods;
    }

    public void setMethods(List<IdValuePair> methods) {
        this.methods = methods;
    }
}
