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
package org.openelisglobal.referral.action.beanitems;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidDate;

public class ReferralItem implements IReferralResultTest, Serializable {

    private static final long serialVersionUID = 1L;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referralId;

    private String accessionNumber;
    private String sampleType;
    private String referringTestName = "";
    private String referralResults = "";
    private ReferralStatus referralStatus;

    @ValidDate(groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referralDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referrer;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredInstituteId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredTestId;
    // the shadow is to track if the test has been changed by the user
    private String referredTestIdShadow;
    private List<IdValuePair> testSelectionList;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredResult = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredDictionaryResult;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredMultiDictionaryResult = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredResultType = "";

    private List<IdValuePair> dictionaryResults = new ArrayList<>();

    @ValidDate(groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredSendDate;

    @ValidDate(groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredReportDate;

    private String pastNotes;
    private String note;

    // can't be used as this is an xml wad, but it should be safe since this field
    // is meant to be parsed
    // @CustomSafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = {
    // ReferredOutTestsForm.ReferredOut.class })
    private String additionalTestsXMLWad;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referralReasonId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referralResultId;

    private boolean modified = false;

    @Valid
    private List<ReferredTest> additionalTests;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String inLabResultId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String multiSelectResultValues;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String qualifiedResultValue;

    @Override
    public String getReferralId() {
        return referralId;
    }

    @Override
    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

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

    public String getReferringTestName() {
        return referringTestName;
    }

    public void setReferringTestName(String referringTestName) {
        this.referringTestName = referringTestName;
    }

    public String getReferralResults() {
        return referralResults;
    }

    public String getReferralDate() {
        return referralDate;
    }

    public void setReferralDate(String referralDate) {
        this.referralDate = referralDate;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getReferredInstituteId() {
        return referredInstituteId;
    }

    public void setReferredInstituteId(String referredInstituteId) {
        this.referredInstituteId = referredInstituteId;
    }

    @Override
    public String getReferredTestId() {
        return referredTestId;
    }

    @Override
    public void setReferredTestId(String referredTestId) {
        this.referredTestId = referredTestId;
    }

    @Override
    public String getReferredTestIdShadow() {
        return referredTestIdShadow;
    }

    @Override
    public void setReferredTestIdShadow(String referredTestIdShadow) {
        this.referredTestIdShadow = referredTestIdShadow;
    }

    public List<IdValuePair> getTestSelectionList() {
        return testSelectionList;
    }

    public void setTestSelectionList(List<IdValuePair> testSelectionList) {
        this.testSelectionList = testSelectionList;
    }

    @Override
    public String getReferredResult() {
        return referredResult;
    }

    @Override
    public void setReferredResult(String referredResult) {
        this.referredResult = referredResult;
    }

    @Override
    public String getReferredResultType() {
        return referredResultType;
    }

    @Override
    public void setReferredResultType(String referredResultType) {
        this.referredResultType = referredResultType;
    }

    public List<IdValuePair> getDictionaryResults() {
        return dictionaryResults;
    }

    public void setDictionaryResults(List<IdValuePair> dictionaryResults) {
        this.dictionaryResults = dictionaryResults;
    }

    public String getReferredSendDate() {
        return referredSendDate;
    }

    public void setReferredSendDate(String referredRecieveDate) {
        referredSendDate = referredRecieveDate;
    }

    @Override
    public String getReferredReportDate() {
        return referredReportDate;
    }

    @Override
    public void setReferredReportDate(String referredReportDate) {
        this.referredReportDate = referredReportDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAdditionalTestsXMLWad() {
        return additionalTestsXMLWad;
    }

    public void setAdditionalTestsXMLWad(String additionalTestsXMLWad) {
        this.additionalTestsXMLWad = additionalTestsXMLWad;
    }

    public void setReferralReasonId(String referralReasonId) {
        this.referralReasonId = referralReasonId;
    }

    public String getReferralReasonId() {
        return referralReasonId;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isModified() {
        return modified;
    }

    public void setAdditionalTests(List<ReferredTest> additionalTests) {
        this.additionalTests = additionalTests;
    }

    public List<ReferredTest> getAdditionalTests() {
        return additionalTests;
    }

    @Override
    public void setReferralResultId(String referralResultId) {
        this.referralResultId = referralResultId;
    }

    @Override
    public String getReferralResultId() {
        return referralResultId;
    }

    @Override
    public String getReferredDictionaryResult() {
        return referredDictionaryResult;
    }

    @Override
    public void setReferredDictionaryResult(String referredDictionaryResult) {
        this.referredDictionaryResult = referredDictionaryResult;
    }

    public String getInLabResultId() {
        return inLabResultId;
    }

    public void setInLabResultId(String inLabResultId) {
        this.inLabResultId = inLabResultId;
    }

    public void setReferralResults(String referralResults) {
        this.referralResults = referralResults;
    }

    @Override
    public void setReferredMultiDictionaryResult(String referredMultiDictionaryResult) {
        this.referredMultiDictionaryResult = referredMultiDictionaryResult;
    }

    @Override
    public String getReferredMultiDictionaryResult() {
        return referredMultiDictionaryResult;
    }

    public void setPastNotes(String pastNotes) {
        this.pastNotes = pastNotes;
    }

    public String getPastNotes() {
        return pastNotes;
    }

    @Override
    public String getMultiSelectResultValues() {
        return multiSelectResultValues;
    }

    @Override
    public void setMultiSelectResultValues(String multiSelectResultValues) {
        this.multiSelectResultValues = multiSelectResultValues;
    }

    public String getQualifiedResultValue() {
        return qualifiedResultValue;
    }

    public void setQualifiedResultValue(String qualifiedResultValue) {
        this.qualifiedResultValue = qualifiedResultValue;
    }

    public ReferralStatus getReferralStatus() {
        return referralStatus;
    }

    public void setReferralStatus(ReferralStatus referralStatus) {
        this.referralStatus = referralStatus;
    }
}
