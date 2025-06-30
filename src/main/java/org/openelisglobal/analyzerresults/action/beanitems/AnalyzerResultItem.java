/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzerresults.action.beanitems;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.result.form.AnalyzerResultsForm;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidDate;

public class AnalyzerResultItem implements Serializable {
    static final long serialVersionUID = 1L;

    private String id;
    private String analyzerId;
    private String analysisId;
    private String units;
    private String testName;

    // TODO move all accession number to the same format so they can be validated
    // properly
    // @ValidAccessionNumber(groups = { AnalyzerResultsForm.AnalyzerResuts.class })
    @Pattern(regexp = "^[0-9a-zA-Z -:]*$", groups = { AnalyzerResultsForm.AnalyzerResuts.class })
    private String accessionNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { AnalyzerResultsForm.AnalyzerResuts.class })
    private String result;

    private boolean isControl = false;

    private boolean isAccepted = false;

    private boolean isRejected = false;

    private boolean isDeleted = false;

    private boolean isManual = false;

    private String errorMessage;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { AnalyzerResultsForm.AnalyzerResuts.class })
    private String note;

    private String statusId;
    private String sampleId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { AnalyzerResultsForm.AnalyzerResuts.class })
    private String testId;

    @ValidDate(groups = { AnalyzerResultsForm.AnalyzerResuts.class })
    private String completeDate;

    private boolean isPositive = false;
    private String duplicateAnalyzerResultId;
    private boolean isHighlighted = false;
    private Timestamp lastUpdated;

    private int sampleGroupingNumber = 0;

    private boolean groupIsReadOnly = false;

    private boolean readOnly = false;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { AnalyzerResultsForm.AnalyzerResuts.class })
    private String testResultType = "N";

    private boolean userChoiceReflex;
    private boolean userChoicePending;
    private String siblingReflexKey;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { AnalyzerResultsForm.AnalyzerResuts.class })
    private String reflexSelectionId;

    private String selectionOneText = "";
    private String selectionOneValue = "";
    private String selectionTwoText = "";
    private String selectionTwoValue = "";
    private boolean nonconforming = false;
    private String significantDigits = "";

    public String getSignificantDigits() {
        return significantDigits;
    }

    public void setSignificantDigits(String significantDigits) {
        this.significantDigits = significantDigits;
    }

    private List<Dictionary> dictionaryResultList;

    public AnalyzerResultItem() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getUnits() {
        return units;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setIsControl(boolean isControl) {
        this.isControl = isControl;
    }

    public boolean getIsControl() {
        return isControl;
    }

    public void setIsAccepted(boolean isAccepted) {
        this.isAccepted = isAccepted;
    }

    public boolean getIsAccepted() {
        return isAccepted;
    }

    public void setIsRejected(boolean isRejected) {
        this.isRejected = isRejected;
    }

    public boolean getIsRejected() {
        return isRejected;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestId() {
        return testId;
    }

    public void setCompleteDate(String completeDate) {
        this.completeDate = completeDate;
    }

    public String getCompleteDate() {
        return completeDate;
    }

    public void setPositive(boolean isPositive) {
        this.isPositive = isPositive;
    }

    public boolean getPositive() {
        return isPositive;
    }

    public void setDuplicateAnalyzerResultId(String duplicateAnalyzerResultId) {
        this.duplicateAnalyzerResultId = duplicateAnalyzerResultId;
    }

    public String getDuplicateAnalyzerResultId() {
        return duplicateAnalyzerResultId;
    }

    public void setIsHighlighted(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }

    public boolean getIsHighlighted() {
        return isHighlighted && readOnly;
    }

    public void setLastUpdated(Timestamp lastupdated) {
        lastUpdated = lastupdated;
    }

    public Timestamp getLastUpdated() {
        return lastUpdated;
    }

    public void setSampleGroupingNumber(int sampleGroupingNumber) {
        this.sampleGroupingNumber = sampleGroupingNumber;
    }

    public int getSampleGroupingNumber() {
        return sampleGroupingNumber;
    }

    public void setManual(boolean isManual) {
        this.isManual = isManual;
    }

    public boolean getManual() {
        return isManual;
    }

    public void setGroupIsReadOnly(boolean groupIsReadOnly) {
        this.groupIsReadOnly = groupIsReadOnly;
    }

    public boolean isGroupIsReadOnly() {
        return groupIsReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setTestResultType(String testResultType) {
        this.testResultType = testResultType;
    }

    public String getTestResultType() {
        return testResultType;
    }

    public void setDictionaryResultList(List<Dictionary> dictionaryResultList) {
        this.dictionaryResultList = dictionaryResultList;
    }

    public List<Dictionary> getDictionaryResultList() {
        return dictionaryResultList;
    }

    public boolean isUserChoiceReflex() {
        return userChoiceReflex;
    }

    public void setUserChoiceReflex(boolean userChoiceReflex) {
        this.userChoiceReflex = userChoiceReflex;
    }

    public String getSiblingReflexKey() {
        return siblingReflexKey;
    }

    public void setSiblingReflexKey(String siblingReflexKey) {
        this.siblingReflexKey = siblingReflexKey;
    }

    public String getReflexSelectionId() {
        return reflexSelectionId;
    }

    public void setReflexSelectionId(String reflexSelectionId) {
        this.reflexSelectionId = reflexSelectionId;
    }

    public String getSelectionOneText() {
        return selectionOneText;
    }

    public void setSelectionOneText(String selectionOneText) {
        this.selectionOneText = selectionOneText;
    }

    public String getSelectionOneValue() {
        return selectionOneValue;
    }

    public void setSelectionOneValue(String selectionOneValue) {
        this.selectionOneValue = selectionOneValue;
    }

    public String getSelectionTwoText() {
        return selectionTwoText;
    }

    public void setSelectionTwoText(String selectionTwoText) {
        this.selectionTwoText = selectionTwoText;
    }

    public String getSelectionTwoValue() {
        return selectionTwoValue;
    }

    public void setSelectionTwoValue(String selectionTwoValue) {
        this.selectionTwoValue = selectionTwoValue;
    }

    public boolean isUserChoicePending() {
        return userChoicePending;
    }

    public void setUserChoicePending(boolean userChoicePending) {
        this.userChoicePending = userChoicePending;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public boolean isNonconforming() {
        return nonconforming;
    }

    public void setNonconforming(boolean nonconforming) {
        this.nonconforming = nonconforming;
    }
}
