package org.openelisglobal.resultvalidation.form;

import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.inventory.form.InventoryKitItem;
import org.openelisglobal.patient.form.PatientInfoForm;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;

public class AccessionValidationForm extends BaseForm implements PatientInfoForm, ValidationPagingForm {
    private PagingBean paging;

    private Boolean singlePatient = true;

    private String firstName = "";

    private String lastName = "";

    private String dob = "";

    private String gender = "";

    private String st = "";

    private String subjectNumber = "";

    private String nationalId = "";

    private Boolean displayTestMethod = true;

    private Boolean displayTestKit = false;

    private List<InventoryKitItem> inventoryItems;

    private List<String> hivKits;

    private List<String> syphilisKits;

    private Boolean searchFinished = false;

    private String type = "";

    private List<IdValuePair> referralReasons;

    private List<IdValuePair> rejectReasons;

    private List<IdValuePair> testSections;

    private List<IdValuePair> testSectionsByName;

    private String testSectionId;

    private Boolean displayTestSections = false;

    // for display
    private String testName = "";

    @Valid
    private List<AnalysisItem> resultList;

    public AccessionValidationForm() {
        setFormName("AccessionValidationForm");
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

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String getDob() {
        return dob;
    }

    @Override
    public void setDob(String dob) {
        this.dob = dob;
    }

    @Override
    public String getGender() {
        return gender;
    }

    @Override
    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public String getSt() {
        return st;
    }

    @Override
    public void setSt(String st) {
        this.st = st;
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
    public String getNationalId() {
        return nationalId;
    }

    @Override
    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
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

    @Override
    public boolean getSearchFinished() {
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

    public List<IdValuePair> getTestSections() {
        return testSections;
    }

    public void setTestSections(List<IdValuePair> testSections) {
        this.testSections = testSections;
    }

    public List<IdValuePair> getTestSectionsByName() {
        return testSectionsByName;
    }

    public void setTestSectionsByName(List<IdValuePair> testSectionsByName) {
        this.testSectionsByName = testSectionsByName;
    }

    @Override
    public String getTestSectionId() {
        return testSectionId;
    }

    @Override
    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public Boolean getDisplayTestSections() {
        return displayTestSections;
    }

    public void setDisplayTestSections(Boolean displayTestSections) {
        this.displayTestSections = displayTestSections;
    }

    @Override
    public List<AnalysisItem> getResultList() {
        return resultList;
    }

    @Override
    public void setResultList(List<AnalysisItem> resultList) {
        this.resultList = resultList;
    }

    public String getTestSection() {
        return "";
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }
}
