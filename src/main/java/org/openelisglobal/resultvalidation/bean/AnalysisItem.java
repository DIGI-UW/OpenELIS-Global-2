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
 */
package org.openelisglobal.resultvalidation.bean;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.resultvalidation.form.ResultValidationForm;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;

public class AnalysisItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String units;

    private String testName;

    @ValidAccessionNumber(groups = { ResultValidationForm.ResultValidation.class })
    private String accessionNumber;

    private String patientName;

    private String patientInfo;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ResultValidationForm.ResultValidation.class })
    private String result;

    private String receivedDate;

    private boolean isAccepted = false;

    private boolean isRejected = false;

    private boolean sampleIsAccepted = false;

    private boolean sampleIsRejected = false;

    private boolean isManual = false;

    private String errorMessage;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ResultValidationForm.ResultValidation.class })
    private String note;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultValidationForm.ResultValidation.class })
    private String noteId;

    private String statusId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultValidationForm.ResultValidation.class })
    private String sampleId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultValidationForm.ResultValidation.class })
    private String analysisId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultValidationForm.ResultValidation.class })
    private String testId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ResultValidationForm.ResultValidation.class })
    private String resultId;

    private double lowerCritical;
    private double higherCritical;
    private String normalRange;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ResultValidationForm.ResultValidation.class })
    private String resultType;

    private String completeDate;

    private boolean isPositive = false;

    private boolean isHighlighted = false;

    private Timestamp lastUpdated;

    private int sampleGroupingNumber = 0;

    private String testSortNumber;

    private String integralResult;

    private String integralAnalysisId;

    private String genscreenResult;

    private String genscreenAnalysisId;

    private String murexResult;

    private String murexAnalysisId;

    private String vironostikaResult;

    private String vironostikaAnalysisId;

    private String genieIIResult;

    private String genieIIAnalysisId;

    private String genieII100Result;

    private String genieII100AnalysisId;

    private String genieII10Result;

    private String genieII10AnalysisId;

    private String westernBlot1Result;

    private String westernBlot1AnalysisId;

    private String westernBlot2Result;

    private String westernBlot2AnalysisId;

    private String p24AgResult;

    private String p24AgAnalysisId;

    private String biolineResult;

    private String biolineAnalysisId;

    private String innoliaResult;

    private String innoliaAnalysisId;

    private String finalResult;

    private String nextTest;
    /*
     * this is very specific to showing calculated results, generalize if there are
     * more than just log calculations
     */
    private boolean displayResultAsLog = false;

    private boolean showAcceptReject = true;

    private List<IdValuePair> methods;
    private List<IdValuePair> referralOrganizations;
    private List<IdValuePair> referralReasons;

    private List<IdValuePair> dictionaryResults;

    private boolean isMultipleResultForSample = false;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ResultValidationForm.ResultValidation.class })
    private String multiSelectResultValues = "{}";

    private boolean readOnly = false;

    private boolean isReflexGroup = false;

    private boolean isChildReflex = false;

    private boolean nonconforming = false;

    private String pastNotes;

    private String qualifiedDictionaryId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ResultValidationForm.ResultValidation.class })
    private String qualifiedResultValue = "";

    private String qualifiedResultId;

    private boolean hasQualifiedResult = false;

    private int significantDigits = 0;

    private String rejectReasonId;

    private boolean valid = true;

    private boolean isNormal;

    public String getRejectReasonId() {
        return rejectReasonId;
    }

    public void setRejectReasonId(String rejectReasonId) {
        this.rejectReasonId = rejectReasonId;
    }

    public AnalysisItem() {
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

    public String getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(String normalRange) {
        this.normalRange = normalRange;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getReceivedDate() {
        return receivedDate;
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

    public boolean isSampleIsAccepted() {
        return sampleIsAccepted;
    }

    public void setSampleIsAccepted(boolean sampleIsAccepted) {
        this.sampleIsAccepted = sampleIsAccepted;
    }

    public boolean isSampleIsRejected() {
        return sampleIsRejected;
    }

    public void setSampleIsRejected(boolean sampleIsRejected) {
        this.sampleIsRejected = sampleIsRejected;
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

    public void setIsHighlighted(boolean isHighlighted) {
        this.isHighlighted = isHighlighted;
    }

    public boolean getIsHighlighted() {
        return isHighlighted;
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

    public void setTestSortNumber(String testSortNumber) {
        this.testSortNumber = testSortNumber;
    }

    public String getTestSortNumber() {
        return testSortNumber;
    }

    public void setManual(boolean isManual) {
        this.isManual = isManual;
    }

    public boolean getManual() {
        return isManual;
    }

    public String getIntegralResult() {
        return integralResult;
    }

    public void setIntegralResult(String integralResult) {
        this.integralResult = integralResult;
    }

    public void setIntegralAnalysisId(String integralAnalysisId) {
        this.integralAnalysisId = integralAnalysisId;
    }

    public String getIntegralAnalysisId() {
        return integralAnalysisId;
    }

    public String getMurexResult() {
        return murexResult;
    }

    public void setMurexResult(String murexResult) {
        this.murexResult = murexResult;
    }

    public void setMurexAnalysisId(String murexAnalysisId) {
        this.murexAnalysisId = murexAnalysisId;
    }

    public String getMurexAnalysisId() {
        return murexAnalysisId;
    }

    public String getVironostikaResult() {
        return vironostikaResult;
    }

    public void setVironostikaResult(String vironostikaResult) {
        this.vironostikaResult = vironostikaResult;
    }

    public void setVironostikaAnalysisId(String vironostikaAnalysisId) {
        this.vironostikaAnalysisId = vironostikaAnalysisId;
    }

    public String getVironostikaAnalysisId() {
        return vironostikaAnalysisId;
    }

    public String getGenieIIResult() {
        return genieIIResult;
    }

    public void setGenieIIResult(String genieIIResult) {
        this.genieIIResult = genieIIResult;
    }

    public void setGenieIIAnalysisId(String genieIIAnalysisId) {
        this.genieIIAnalysisId = genieIIAnalysisId;
    }

    public String getGenieIIAnalysisId() {
        return genieIIAnalysisId;
    }

    public String getGenieII100Result() {
        return genieII100Result;
    }

    public void setGenieII100Result(String genieII100Result) {
        this.genieII100Result = genieII100Result;
    }

    public void setGenieII100AnalysisId(String genieII100AnalysisId) {
        this.genieII100AnalysisId = genieII100AnalysisId;
    }

    public String getGenieII100AnalysisId() {
        return genieII100AnalysisId;
    }

    public String getGenieII10Result() {
        return genieII10Result;
    }

    public void setGenieII10Result(String genieII10Result) {
        this.genieII10Result = genieII10Result;
    }

    public void setGenieII10AnalysisId(String genieII10AnalysisId) {
        this.genieII10AnalysisId = genieII10AnalysisId;
    }

    public String getGenieII10AnalysisId() {
        return genieII10AnalysisId;
    }

    public String getWesternBlot1Result() {
        return westernBlot1Result;
    }

    public void setWesternBlot1Result(String westernBlot1Result) {
        this.westernBlot1Result = westernBlot1Result;
    }

    public void setWesternBlot1AnalysisId(String westernBlot1AnalysisId) {
        this.westernBlot1AnalysisId = westernBlot1AnalysisId;
    }

    public String getWesternBlot1AnalysisId() {
        return westernBlot1AnalysisId;
    }

    public String getWesternBlot2Result() {
        return westernBlot2Result;
    }

    public void setWesternBlot2Result(String westernBlot2Result) {
        this.westernBlot2Result = westernBlot2Result;
    }

    public void setWesternBlot2AnalysisId(String westernBlot2AnalysisId) {
        this.westernBlot2AnalysisId = westernBlot2AnalysisId;
    }

    public String getWesternBlot2AnalysisId() {
        return westernBlot2AnalysisId;
    }

    public String getP24AgResult() {
        return p24AgResult;
    }

    public void setP24AgResult(String p24AgResult) {
        this.p24AgResult = p24AgResult;
    }

    public void setP24AgAnalysisId(String p24AgAnalysisId) {
        this.p24AgAnalysisId = p24AgAnalysisId;
    }

    public String getP24AgAnalysisId() {
        return p24AgAnalysisId;
    }

    public void setFinalResult(String finalResult) {
        this.finalResult = finalResult;
    }

    public String getFinalResult() {
        return finalResult;
    }

    public void setNextTest(String nextTest) {
        this.nextTest = nextTest;
    }

    public String getNextTest() {
        return nextTest;
    }

    public void setDictionaryResults(List<IdValuePair> dictionaryResults) {
        this.dictionaryResults = dictionaryResults;
    }

    public List<IdValuePair> getDictionaryResults() {
        return dictionaryResults;
    }

    public List<IdValuePair> getMethods() {
        return methods;
    }

    public void setMethods(List<IdValuePair> methods) {
        this.methods = methods;
    }

    public List<IdValuePair> getReferralOrganizations() {
        return referralOrganizations;
    }

    public void setReferralOrganizations(List<IdValuePair> referralOrganizations) {
        this.referralOrganizations = referralOrganizations;
    }

    public List<IdValuePair> getReferralReasons() {
        return referralReasons;
    }

    public void setReferralReasons(List<IdValuePair> referralReasons) {
        this.referralReasons = referralReasons;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getResultType() {
        return resultType;
    }

    public void setDisplayResultAsLog(boolean displayResultAsLog) {
        this.displayResultAsLog = displayResultAsLog;
    }

    public boolean isDisplayResultAsLog() {
        return displayResultAsLog;
    }

    public void setShowAcceptReject(boolean showAcceptReject) {
        this.showAcceptReject = showAcceptReject;
    }

    public boolean isShowAcceptReject() {
        return showAcceptReject;
    }

    public void setMultipleResultForSample(boolean isMultipleResultForSample) {
        this.isMultipleResultForSample = isMultipleResultForSample;
    }

    public boolean isMultipleResultForSample() {
        return isMultipleResultForSample;
    }

    public String getMultiSelectResultValues() {
        return multiSelectResultValues;
    }

    public void setMultiSelectResultValues(String multiSelectResultValues) {
        this.multiSelectResultValues = multiSelectResultValues;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
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

    public String getBiolineResult() {
        return biolineResult;
    }

    public void setBiolineResult(String biolineResult) {
        this.biolineResult = biolineResult;
    }

    public String getBiolineAnalysisId() {
        return biolineAnalysisId;
    }

    public void setBiolineAnalysisId(String biolineAnalysisID) {
        biolineAnalysisId = biolineAnalysisID;
    }

    public boolean isNonconforming() {
        return nonconforming;
    }

    public void setNonconforming(boolean nonconforming) {
        this.nonconforming = nonconforming;
    }

    public String getInnoliaResult() {
        return innoliaResult;
    }

    public void setInnoliaResult(String innoliaResult) {
        this.innoliaResult = innoliaResult;
    }

    public String getInnoliaAnalysisId() {
        return innoliaAnalysisId;
    }

    public void setInnoliaAnalysisId(String innoliaAnalysisId) {
        this.innoliaAnalysisId = innoliaAnalysisId;
    }

    public String getPastNotes() {
        return pastNotes;
    }

    public void setPastNotes(String pastNotes) {
        this.pastNotes = pastNotes;
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

    public String getQualifiedResultId() {
        return qualifiedResultId;
    }

    public void setQualifiedResultId(String qualifiedResultId) {
        this.qualifiedResultId = qualifiedResultId;
    }

    public int getSignificantDigits() {
        return significantDigits;
    }

    public void setSignificantDigits(int significantDigits) {
        this.significantDigits = significantDigits;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public boolean isNormal() {
        return isNormal;
    }

    public void setNormal(boolean isNormal) {
        this.isNormal = isNormal;
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

    public String getGenscreenResult() {
        return genscreenResult;
    }

    public void setGenscreenResult(String genscreenResult) {
        this.genscreenResult = genscreenResult;
    }

    public String getGenscreenAnalysisId() {
        return genscreenAnalysisId;
    }

    public void setGenscreenAnalysisId(String genscreenAnalysisId) {
        this.genscreenAnalysisId = genscreenAnalysisId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientInfo() {
        return patientInfo;
    }

    public void setPatientInfo(String patientInfo) {
        this.patientInfo = patientInfo;
    }
}
