package org.openelisglobal.resultvalidation.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.paging.PagingBean;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.validation.annotations.ValidDate;

public class ResultValidationForm extends BaseForm implements ValidationPagingForm {
    public interface ResultValidation {
    }

    private boolean searchFinished;

    // for display
    private PagingBean paging;

    @ValidDate(relative = DateRelation.TODAY, groups = { ResultValidation.class })
    private String currentDate = "";

    @Valid
    private List<AnalysisItem> resultList;

    @Pattern(regexp = "^[a-zA-Z0-9 -]*$", groups = { ResultValidation.class })
    private String testSection = "";

    private String accessionNumber = "";

    @ValidDate
    private String testDate = "";

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    // for display
    private String testName = "";

    // for display
    private List<IdValuePair> testSections;

    // for display
    private List<IdValuePair> testSectionsByName;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultValidation.class })
    private String testSectionId;

    @NotNull(groups = { ResultValidation.class })
    private Boolean displayTestSections = true;

    public ResultValidationForm() {
        setFormName("ResultValidationForm");
    }

    @Override
    public PagingBean getPaging() {
        return paging;
    }

    @Override
    public void setPaging(PagingBean paging) {
        this.paging = paging;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
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
        return testSection;
    }

    public void setTestSection(String testSection) {
        this.testSection = testSection;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
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

    public void setSearchFinished(boolean searchFinished) {
        this.searchFinished = searchFinished;
    }

    @Override
    public boolean getSearchFinished() {
        return searchFinished;
    }

    public String getTestDate() {
        return testDate;
    }

    public void setTestDate(String testDate) {
        this.testDate = testDate;
    }
}
