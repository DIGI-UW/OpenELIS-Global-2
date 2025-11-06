package org.openelisglobal.sample.bean;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.groups.Default;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.sample.form.SampleEditForm;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidTime;

public class SampleEditItem {
    private String accessionNumber;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SampleEditForm.SampleEdit.class })
    private String analysisId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { Default.class, SampleEditForm.SampleEdit.class })
    private String sampleType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { Default.class, SampleEditForm.SampleEdit.class })
    private String testName;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SampleEditForm.SampleEdit.class })
    private String sampleItemId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { SampleEditForm.SampleEdit.class })
    private String testId;

    private boolean canCancel = false;

    private boolean canceled;

    private boolean add;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { Default.class, SampleEditForm.SampleEdit.class })
    private String status;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { Default.class, SampleEditForm.SampleEdit.class })
    private String sortOrder;

    private boolean canRemoveSample = false;

    private boolean removeSample;

    @ValidDate(relative = DateRelation.PAST, groups = { SampleEditForm.SampleEdit.class })
    private String collectionDate;

    @ValidTime(groups = { SampleEditForm.SampleEdit.class })
    private String collectionTime;

    private boolean sampleItemChanged = false;

    private boolean hasResults = false;

    // for display in the react Data table. set as the testId
    private String id;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
        this.id = testId;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public void setCanCancel(boolean hasResults) {
        canCancel = hasResults;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean remove) {
        canceled = remove;
    }

    public boolean isAdd() {
        return add;
    }

    public void setAdd(boolean add) {
        this.add = add;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isCanRemoveSample() {
        return canRemoveSample;
    }

    public void setCanRemoveSample(boolean canRemoveSample) {
        this.canRemoveSample = canRemoveSample;
    }

    public boolean isRemoveSample() {
        return removeSample;
    }

    public void setRemoveSample(boolean removeSample) {
        this.removeSample = removeSample;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getCollectionTime() {
        return collectionTime;
    }

    public void setCollectionTime(String collectionTime) {
        this.collectionTime = collectionTime;
    }

    public boolean isSampleItemChanged() {
        return sampleItemChanged;
    }

    public void setSampleItemChanged(boolean sampleItemChanged) {
        this.sampleItemChanged = sampleItemChanged;
    }

    public boolean isHasResults() {
        return hasResults;
    }

    public void setHasResults(boolean hasResults) {
        this.hasResults = hasResults;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
