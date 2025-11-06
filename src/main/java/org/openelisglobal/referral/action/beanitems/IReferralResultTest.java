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

public interface IReferralResultTest {

    public abstract void setReferredTestId(String referredTestId);

    public abstract String getReferredTestId();

    public abstract String getReferredTestIdShadow();

    public abstract void setReferredTestIdShadow(String referredTestIdShadow);

    public abstract String getReferredResult();

    public abstract void setReferredResult(String referredResult);

    public abstract String getReferredResultType();

    public abstract void setReferredResultType(String referredResultType);

    public abstract String getReferredReportDate();

    public abstract void setReferredReportDate(String referredReportDate);

    public abstract String getReferralResultId();

    public abstract void setReferralResultId(String id);

    public abstract void setReferredDictionaryResult(String id);

    public abstract String getReferredDictionaryResult();

    public abstract String getReferredMultiDictionaryResult();

    public abstract void setReferredMultiDictionaryResult(String multiResultValue);

    public abstract String getReferralId();

    public abstract void setReferralId(String referralId);

    public abstract String getMultiSelectResultValues();

    public abstract void setMultiSelectResultValues(String multiSelectResultValues);
}
