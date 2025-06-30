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

import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.referral.form.ReferredOutTestsForm;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidDate;

public class ReferredTest implements IReferralResultTest {

    private String referralId;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredTestId;
    // the shadow is to track if the test has been changed by the user
    private String referredTestIdShadow;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredResult = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredDictionaryResult;

    // for display
    private List<IdValuePair> dictionaryResults = new ArrayList<>();

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredResultType = "";

    @ValidDate(groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredReportDate;

    @Pattern(regexp = ValidationHelper.ID_REGEX, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referralResultId;

    private boolean remove = false;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String referredMultiDictionaryResult;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE, groups = { ReferredOutTestsForm.ReferredOut.class })
    private String multiSelectResultValues = "{}";

    @Override
    public String getReferralId() {
        return referralId;
    }

    @Override
    public void setReferralId(String referralId) {
        this.referralId = referralId;
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

    @Override
    public String getReferredTestId() {
        return referredTestId;
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

    @Override
    public String getReferredReportDate() {
        return referredReportDate;
    }

    @Override
    public void setReferredReportDate(String referredReportDate) {
        this.referredReportDate = referredReportDate == null ? "" : referredReportDate;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public boolean isRemove() {
        return remove;
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
    public void setReferredDictionaryResult(String referredDictionaryResult) {
        this.referredDictionaryResult = referredDictionaryResult;
    }

    @Override
    public String getReferredDictionaryResult() {
        return referredDictionaryResult;
    }

    public void setDictionaryResults(List<IdValuePair> dictionaryResults) {
        this.dictionaryResults = dictionaryResults;
    }

    public List<IdValuePair> getDictionaryResults() {
        return dictionaryResults;
    }

    @Override
    public String getReferredMultiDictionaryResult() {
        return referredMultiDictionaryResult;
    }

    @Override
    public void setReferredMultiDictionaryResult(String referredMultiDictionaryResult) {
        this.referredMultiDictionaryResult = referredMultiDictionaryResult;
    }

    @Override
    public String getMultiSelectResultValues() {
        return multiSelectResultValues;
    }

    @Override
    public void setMultiSelectResultValues(String multiSelectResultValues) {
        this.multiSelectResultValues = multiSelectResultValues;
    }
}
