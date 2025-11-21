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
package org.openelisglobal.test.beanItems;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.openelisglobal.analysis.valueholder.ResultFile;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.referral.action.beanitems.ReferralItem;
import org.openelisglobal.result.action.util.ResultItem;
import org.openelisglobal.result.form.LogbookResultsForm;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidAccessionNumber;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;
import org.openelisglobal.workplan.form.WorkplanForm;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestResultItem implements ResultItem, Serializable {

    private static final long serialVersionUID = 1L;

    @ValidAccessionNumber(format = AccessionFormat.UNFORMATTED, groups = { WorkplanForm.PrintWorkplan.class,
            LogbookResultsForm.LogbookResults.class })
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

    /*
     * Used for inserting Patient Names/o into lists of TestResultItems
     */
    private boolean isServingAsTestGroupIdentifier = false;

    private static String NO = "no";

    @SuppressWarnings("unused")
    private static String YES = "yes";

    public enum Method {
        DNA, MANUAL, AUTO;
    }

    public enum ResultDisplayType {
        TEXT, POS_NEG, POS_NEG_IND, HIV, SYPHILIS;
    }

    private String sampleSource;

    @ValidDate(relative = DateRelation.PAST, acceptTime = true, groups = { LogbookResultsForm.LogbookResults.class })
    private String testDate;

    @ValidDate(relative = DateRelation.PAST, acceptTime = true, groups = { WorkplanForm.PrintWorkplan.class })
    private String receivedDate;
    /*
     * N.B. test method is the type of test it is (HIV etc). analysisMethod is the
     * way the analysis is done automatic or manual
     */
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class,
            LogbookResultsForm.LogbookResults.class })
    private String testMethod;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String analysisMethod;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { WorkplanForm.PrintWorkplan.class })
    private String testName;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String testId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String testKit1InventoryId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String testKitId;

    private boolean testKitInactive = false;
    private double upperNormalRange = 0;
    private double lowerNormalRange = 0;
    private double upperAbnormalRange;
    private double lowerAbnormalRange;
    private String normalRange = "";
    private double lowerCritical;
    private double higherCritical;

    private int significantDigits = 0;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String shadowResultValue;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String resultValue;

    private String remarks;

    @ValidName(nameType = NameType.FULL_NAME)
    private String technician;

    private boolean reportable;
    private String patientName;

    private String patientId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { WorkplanForm.PrintWorkplan.class })
    private String patientInfo;

    private String nationalId;
    private String unitsOfMeasure = "";

    // private String testSortNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String resultType;

    private ResultDisplayType resultDisplayType = ResultDisplayType.TEXT;
    private boolean isModified = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String analysisId;

    private String analysisStatusId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String resultId;

    private Result result;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String technicianSignatureId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String resultLimitId;

    private List<IdValuePair> dictionaryResults;
    private List<IdValuePair> methods;
    private List<IdValuePair> referralOrganizations;
    private List<IdValuePair> referralReasons;
    private String remove = NO;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String note;

    private String pastNotes;
    private boolean valid = true;
    private boolean normal = true;
    private boolean notIncludedInWorkplan = false;
    private boolean isUserChoiceReflex = false;
    private String siblingReflexKey;
    private String thisReflexKey;
    private boolean readOnly = false;
    private boolean referredOut = false;
    private boolean referralCanceled = false;

    // This is the workaround for the checkbox stickiness issue
    private boolean shadowReferredOut = false;
    private boolean shadowRejected = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String referralId = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String referralReasonId = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String multiSelectResultValues;

    private String initialSampleCondition;
    private String sampleType;
    private boolean failedValidation = false;
    private boolean nonconforming = false;
    private String testSortOrder = null;
    private boolean isReflexGroup = false;
    private int reflexParentGroup = 0;
    private boolean isChildReflex = false;
    private boolean displayResultAsLog = false;
    private String qualifiedDictionaryId = null;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String qualifiedResultValue = "";

    private String qualifiedResultId;
    private boolean hasQualifiedResult = false;
    private String nextVisitDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String forceTechApproval;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String reflexJSONResult;

    private boolean rejected = false;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { LogbookResultsForm.LogbookResults.class })
    private String rejectReasonId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { LogbookResultsForm.LogbookResults.class })
    private String considerRejectReason;

    private boolean refer;

    private String defaultResultValue;

    private ReferralItem referralItem;
    private ResultFileForm resultFile;

    public String getConsiderRejectReason() {
        return considerRejectReason;
    }

    public void setConsiderRejectReason(String considerRejectReason) {
        this.considerRejectReason = considerRejectReason;
    }

    public String getRejectReasonId() {
        return rejectReasonId;
    }

    public void setRejectReasonId(String rejectReasonId) {
        this.rejectReasonId = rejectReasonId;
    }

    public boolean isRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }

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

    public String getTestKit1InventoryId() {
        return testKit1InventoryId;
    }

    public void setTestKit1InventoryId(String testKit1InventoryId) {
        this.testKit1InventoryId = testKit1InventoryId;
    }

    public void setModified(boolean isModified) {
        this.isModified = isModified;
    }

    public boolean isReferredOut() {
        return referredOut;
    }

    public void setReferredOut(boolean referredOut) {
        this.referredOut = referredOut;
    }

    public boolean isShadowReferredOut() {
        return shadowReferredOut;
    }

    public void setShadowReferredOut(boolean shadowReferredOut) {
        this.shadowReferredOut = shadowReferredOut;
    }

    public boolean isShadowRejected() {
        return shadowRejected;
    }

    public void setShadowRejected(boolean shadowRejected) {
        this.shadowRejected = shadowRejected;
    }

    public String getTechnicianSignatureId() {
        return technicianSignatureId;
    }

    public void setTechnicianSignatureId(String technicianId) {
        technicianSignatureId = technicianId;
    }

    public String getTestKitInventoryId() {
        return testKit1InventoryId;
    }

    public void setTestKitInventoryId(String testKit1) {
        testKit1InventoryId = testKit1;
    }

    public void setTestKitInactive(boolean testKitInactive) {
        this.testKitInactive = testKitInactive;
    }

    public boolean getTestKitInactive() {
        return testKitInactive;
    }

    public String getTestKitId() {
        return testKitId;
    }

    public void setTestKitId(String testKitId) {
        this.testKitId = testKitId;
    }

    public String getResultDisplayType() {
        return resultDisplayType.toString();
    }

    @JsonIgnore()
    public ResultDisplayType getRawResultDisplayType() {
        return resultDisplayType;
    }

    public void setResultDisplayType(ResultDisplayType resultType) {
        resultDisplayType = resultType;
    }

    @JsonIgnore()
    public ResultDisplayType getEnumResultType() {
        return resultDisplayType;
    }

    public String getUnitsOfMeasure() {
        return unitsOfMeasure;
    }

    public void setUnitsOfMeasure(String unitsOfMeasure) {
        this.unitsOfMeasure = unitsOfMeasure;
    }

    public double getUpperNormalRange() {
        return upperNormalRange;
    }

    public void setUpperNormalRange(double upperNormalRange) {
        this.upperNormalRange = upperNormalRange;
    }

    public double getLowerNormalRange() {
        return lowerNormalRange;
    }

    public void setLowerNormalRange(double lowerNormalRange) {
        this.lowerNormalRange = lowerNormalRange;
    }

    public double getUpperAbnormalRange() {
        return upperAbnormalRange;
    }

    public void setUpperAbnormalRange(double upperAbnormalRange) {
        this.upperAbnormalRange = upperAbnormalRange;
    }

    public double getLowerAbnormalRange() {
        return lowerAbnormalRange;
    }

    public void setLowerAbnormalRange(double lowerAbnormalRange) {
        this.lowerAbnormalRange = lowerAbnormalRange;
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

    public String getReportable() {
        return reportable ? IActionConstants.YES : IActionConstants.NO;
    }

    public void setReportable(boolean reportable) {
        this.reportable = reportable;
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

    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
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

    public String getRemove() {
        return remove;
    }

    public void setRemove(String remove) {
        this.remove = remove;
    }

    @JsonIgnore()
    public boolean isRemoved() {
        return NO.equals(remove);
    }

    /*
     * public void setTestSortNumber(String testSortNumber) { this.testSortNumber =
     * testSortNumber; } public String getTestSortNumber() { return testSortNumber;
     * }
     */ public String getSampleSource() {
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

    public String getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(Method method) {
        testMethod = method == Method.AUTO ? "Auto" : "Manual";
    }

    @Override
    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String results) {
        resultValue = results;
        setShadowResultValue(results);
    }

    public String getResultValueLog() {
        try {
            DecimalFormat df = new DecimalFormat("###.##");
            double val = Double.parseDouble(this.resultValue);
            return df.format(Math.log10(val));
        } catch (Exception e) {
            return "--";
        }
    }

    public String getShadowResultValue() {
        return shadowResultValue;
    }

    public void setShadowResultValue(String shadowResultValue) {
        this.shadowResultValue = shadowResultValue;
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

    public String getTechnician() {
        return technician;
    }

    public void setTechnician(String technicien) {
        technician = technicien;
    }

    public void setIsModified(boolean isModified) {
        this.isModified = isModified;
    }

    public boolean getIsModified() {
        return isModified;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public void setAnalysisStatusId(String analysisStatusId) {
        this.analysisStatusId = analysisStatusId;
    }

    public String getAnalysisStatusId() {
        return analysisStatusId;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public void setDictionaryResults(List<IdValuePair> dictonaryResults) {
        dictionaryResults = dictonaryResults;
    }

    public List<IdValuePair> getDictionaryResults() {
        return dictionaryResults == null ? new ArrayList<>() : dictionaryResults;
    }

    public void setMethods(List<IdValuePair> methods) {
        this.methods = methods;
    }

    public List<IdValuePair> getMethods() {
        return methods == null ? new ArrayList<>() : methods;
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
        notIncludedInWorkplan = include;
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
        setResultId(result == null ? "" : result.getId());
        this.result = result;
    }

    public Result getResult() {
        return result;
    }

    public void setUserChoiceReflex(boolean isUserChoiceReflex) {
        this.isUserChoiceReflex = isUserChoiceReflex;
    }

    public boolean isUserChoiceReflex() {
        return isUserChoiceReflex;
    }

    public void setSiblingReflexKey(String siblingReflexKey) {
        this.siblingReflexKey = siblingReflexKey;
    }

    public String getSiblingReflexKey() {
        return siblingReflexKey;
    }

    public void setThisReflexKey(String thisReflexKey) {
        this.thisReflexKey = thisReflexKey;
    }

    public String getThisReflexKey() {
        return thisReflexKey;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralReasonId(String referralReasonId) {
        this.referralReasonId = referralReasonId;
    }

    public String getReferralReasonId() {
        return referralReasonId;
    }

    public void setReferralCanceled(boolean referralCanceled) {
        this.referralCanceled = referralCanceled;
    }

    public boolean isReferralCanceled() {
        return referralCanceled;
    }

    public void setMultiSelectResultValues(String newMultiSelectResults) {
        multiSelectResultValues = newMultiSelectResults;
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

    @Override
    public String getSequenceAccessionNumber() {
        return getAccessionNumber() + "-" + getSequenceNumber();
    }

    @Override
    public String getTestSortOrder() {
        return testSortOrder;
    }

    public void setTestSortOrder(String testSortOrder) {
        this.testSortOrder = testSortOrder;
    }

    public int getReflexParentGroup() {
        return reflexParentGroup;
    }

    public void setReflexParentGroup(int reflexParentGroup) {
        this.reflexParentGroup = reflexParentGroup;
    }

    public boolean isChildReflex() {
        return isChildReflex;
    }

    public void setChildReflex(boolean isChildReflex) {
        this.isChildReflex = isChildReflex;
    }

    public boolean isReflexGroup() {
        return isReflexGroup;
    }

    public void setReflexGroup(boolean isReflexGroup) {
        this.isReflexGroup = isReflexGroup;
    }

    public boolean isNormal() {
        return normal;
    }

    public void setNormal(boolean normal) {
        this.normal = normal;
    }

    public boolean isDisplayResultAsLog() {
        return displayResultAsLog;
    }

    public void setDisplayResultAsLog(boolean displayResultAsLog) {
        this.displayResultAsLog = displayResultAsLog;
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

    public String getQualifiedResultId() {
        return qualifiedResultId;
    }

    public boolean isHasQualifiedResult() {
        return hasQualifiedResult;
    }

    public void setHasQualifiedResult(boolean hasQualifiedResult) {
        this.hasQualifiedResult = hasQualifiedResult;
    }

    public void setQualifiedResultId(String qualifiedResultId) {
        this.qualifiedResultId = qualifiedResultId;
    }

    public String getNextVisitDate() {
        return nextVisitDate;
    }

    public void setNextVisitDate(String nextVisitDate) {
        this.nextVisitDate = nextVisitDate;
    }

    public String getForceTechApproval() {
        return forceTechApproval;
    }

    public void setForceTechApproval(String forceTechApproval) {
        this.forceTechApproval = forceTechApproval;
    }

    public String getReflexJSONResult() {
        return reflexJSONResult;
    }

    public void setReflexJSONResult(String reflexJSONResult) {
        this.reflexJSONResult = reflexJSONResult;
    }

    public String getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(String normalRange) {
        this.normalRange = normalRange;
    }

    public int getSignificantDigits() {
        return significantDigits;
    }

    public void setSignificantDigits(int significantDigits) {
        this.significantDigits = significantDigits;
    }

    public boolean isServingAsTestGroupIdentifier() {
        return isServingAsTestGroupIdentifier;
    }

    public void setServingAsTestGroupIdentifier(boolean isServingAsTestGroupIdentifier) {
        this.isServingAsTestGroupIdentifier = isServingAsTestGroupIdentifier;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public boolean isRefer() {
        return refer;
    }

    public void setRefer(boolean refer) {
        this.refer = refer;
    }

    public String getDefaultResultValue() {
        return defaultResultValue;
    }

    public void setDefaultResultValue(String defaultResultValue) {
        this.defaultResultValue = defaultResultValue;
    }

    public ReferralItem getReferralItem() {
        return referralItem;
    }

    public void setReferralItem(ReferralItem referralItem) {
        this.referralItem = referralItem;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public ResultFileForm getResultFile() {
        return resultFile;
    }

    public void setResultFile(ResultFileForm resultFile) {
        this.resultFile = resultFile;
    }

    public static class ResultFileForm extends ResultFile {
        private static final long serialVersionUID = 3142138533368581327L;

        private String base64Content;

        public String getBase64Content() {
            return base64Content;
        }

        public void setBase64Content(String base64Content) {
            this.base64Content = base64Content;
            if (base64Content != null && base64Content.contains(";base64,")) {
                String[] contentInfo = base64Content.split(";base64,", 2);
                setFileType(contentInfo[0]);
                setContent(Base64.getDecoder().decode(contentInfo[1]));

            }
        }

    }
}
