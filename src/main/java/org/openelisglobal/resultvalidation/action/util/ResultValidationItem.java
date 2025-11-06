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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.resultvalidation.action.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.result.action.util.ResultItem;
import org.openelisglobal.result.valueholder.Result;

public class ResultValidationItem implements ResultItem, Serializable {

    private static final long serialVersionUID = 1L;
    private String accessionNumber;
    private String sequenceNumber;
    private boolean showSampleDetails = true;
    /*
     * What the h*** is a group separator? It is a work around for the grouped
     * results forms. If the issue were just displaying there wouldn't be an issue
     * with using nested collections but Struts 1.x makes it difficult to recover
     * the entered data with nested iterators so we are using a single iterator with
     * marked TestResultItems to do the grouping correctly.
     */
    private boolean isGroupSeparator = false;
    private int sampleGroupingNumber = 1; // display only -- groups like samples together

    private static String NO = "no";

    private String noteId;
    private String sampleSource;
    private String testDate;
    private String receivedDate;

    private String analysisMethod;
    private String testName;
    private String testId;

    private String resultValue;
    private String remarks;

    private String unitsOfMeasure = "";
    private String testSortNumber;
    private String resultType;

    private boolean isModified = false;
    private Analysis analysis;
    private String resultId;
    private Result result;
    private String resultLimitId;
    private List<IdValuePair> dictionaryResults;
    private String remove = NO;
    private String note;
    private String pastNotes;
    private boolean valid = true;
    private boolean notIncludedInWorkplan = false;
    private boolean readOnly = false;
    private String multiSelectResultValues = "{}";
    private String initialSampleCondition;
    private String sampleType;
    private boolean failedValidation = false;
    private boolean isReflexGroup = false;
    private boolean isChildReflex = false;
    private boolean nonconforming = false;
    private String qualifiedDictionaryId;
    private String qualifiedResultValue = "";
    private String qualificationResultId;
    private boolean hasQualifiedResult = false;
    private boolean normalResult;
    private String normalRange;
    private String patientName;
    private double lowerCritical;
    private double higherCritical;

    @Override
    public String getAccessionNumber() {
        return accessionNumber;
    }

    @Override
    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    @Override
    public String getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public void setSequenceNumber(String sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public boolean isShowSampleDetails() {
        return showSampleDetails;
    }

    @Override
    public void setShowSampleDetails(boolean showSampleDetails) {
        this.showSampleDetails = showSampleDetails;
    }

    @Override
    public boolean getIsGroupSeparator() {
        return isGroupSeparator;
    }

    @Override
    public void setIsGroupSeparator(boolean isGroupSeparator) {
        this.isGroupSeparator = isGroupSeparator;
    }

    @Override
    public int getSampleGroupingNumber() {
        return sampleGroupingNumber;
    }

    @Override
    public void setSampleGroupingNumber(int sampleGroupingNumber) {
        this.sampleGroupingNumber = sampleGroupingNumber;
    }

    public void setModified(boolean isModified) {
        this.isModified = isModified;
    }

    public String getUnitsOfMeasure() {
        return unitsOfMeasure;
    }

    public void setUnitsOfMeasure(String unitsOfMeasure) {
        this.unitsOfMeasure = unitsOfMeasure;
    }

    public String getRemove() {
        return remove;
    }

    public void setRemove(String remove) {
        this.remove = remove;
    }

    public boolean isRemoved() {
        return NO.equals(remove);
    }

    public void setTestSortNumber(String testSortNumber) {
        this.testSortNumber = testSortNumber;
    }

    public String getTestSortNumber() {
        return testSortNumber;
    }

    public String getSampleSource() {
        return sampleSource;
    }

    public void setSampleSource(String sampleSource) {
        this.sampleSource = sampleSource;
    }

    public String getTestDate() {
        return testDate;
    }

    public void setTestDate(String testDate) {
        this.testDate = testDate;
    }

    @Override
    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String results) {
        this.resultValue = results;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public void setIsModified(boolean isModified) {
        this.isModified = isModified;
    }

    public boolean getIsModified() {
        return isModified;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public void setDictionaryResults(List<IdValuePair> dictonaryResults) {
        this.dictionaryResults = dictonaryResults;
    }

    public List<IdValuePair> getDictionaryResults() {
        return dictionaryResults == null ? new ArrayList<>() : dictionaryResults;
    }

    public String getResultLimitId() {
        return resultLimitId;
    }

    public void setResultLimitId(String resultLimitId) {
        this.resultLimitId = resultLimitId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setAnalysisMethod(String analysisMethod) {
        this.analysisMethod = analysisMethod;
    }

    public String getAnalysisMethod() {
        return analysisMethod;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }

    public void setNotIncludedInWorkplan(boolean include) {
        this.notIncludedInWorkplan = include;
    }

    public boolean isNotIncludedInWorkplan() {
        return notIncludedInWorkplan;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setResult(Result result) {
        if (result == null) {
            setResultId("");
            setResultValue("");
        } else {
            setResultId(result.getId());
            setResultValue(result.getValue());
        }

        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setMultiSelectResultValues(String newMultiSelectResults) {
        this.multiSelectResultValues = newMultiSelectResults;
    }

    public String getMultiSelectResultValues() {
        return multiSelectResultValues;
    }

    public void setInitialSampleCondition(String initialSampleCondition) {
        this.initialSampleCondition = initialSampleCondition;
    }

    public String getInitialSampleCondition() {
        return initialSampleCondition;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setFailedValidation(boolean failedValidation) {
        this.failedValidation = failedValidation;
    }

    public boolean isFailedValidation() {
        return failedValidation;
    }

    public void setPastNotes(String pastNotes) {
        this.pastNotes = pastNotes;
    }

    public String getPastNotes() {
        return pastNotes;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestId() {
        return testId;
    }

    @Override
    public String getSequenceAccessionNumber() {
        return getAccessionNumber() + "-" + getSequenceNumber();
    }

    @Override
    public String getTestSortOrder() {
        return null;
    }

    public boolean isReflexGroup() {
        return isReflexGroup;
    }

    public void setReflexGroup(boolean isReflexGroup) {
        this.isReflexGroup = isReflexGroup;
    }

    public boolean isChildReflex() {
        return isChildReflex;
    }

    public void setChildReflex(boolean isChildReflex) {
        this.isChildReflex = isChildReflex;
    }

    public boolean isNonconforming() {
        return nonconforming;
    }

    public void setNonconforming(boolean nonconforming) {
        this.nonconforming = nonconforming;
    }

    public String getQualifiedDictionaryId() {
        return qualifiedDictionaryId;
    }

    public void setQualifiedDictionaryId(String qualifiedDictionaryId) {
        this.qualifiedDictionaryId = qualifiedDictionaryId;
    }

    public String getQualifiedResultValue() {
        return qualifiedResultValue;
    }

    public void setQualifiedResultValue(String qualifiedResultValue) {
        this.qualifiedResultValue = qualifiedResultValue;
    }

    public boolean isHasQualifiedResult() {
        return hasQualifiedResult;
    }

    public void setHasQualifiedResult(boolean hasQualifiedResult) {
        this.hasQualifiedResult = hasQualifiedResult;
    }

    public String getQualificationResultId() {
        return qualificationResultId;
    }

    public void setQualificationResultId(String qualificationResultId) {
        this.qualificationResultId = qualificationResultId;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis = analysis;
    }

    public boolean isNormalResult() {
        return normalResult;
    }

    public void setNormalResult(boolean normalResult) {
        this.normalResult = normalResult;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(String normalRange) {
        this.normalRange = normalRange;
    }

    public double getLowerCritical() {
        return lowerCritical;
    }

    public void setLowerCritical(double lowerCritical) {
        this.lowerCritical = lowerCritical;
    }

    public double getHigherCritical() {
        return higherCritical;
    }

    public void setHigherCritical(double higherCritical) {
        this.higherCritical = higherCritical;
    }
}
