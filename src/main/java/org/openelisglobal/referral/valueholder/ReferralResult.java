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
package org.openelisglobal.referral.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.result.valueholder.Result;

public class ReferralResult extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private String referralId;
    private String testId;
    private Timestamp referralReportDate;
    private ValueHolderInterface result = new ValueHolder();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public Timestamp getReferralReportDate() {
        return referralReportDate;
    }

    public void setReferralReportDate(Timestamp referralReportDate) {
        this.referralReportDate = referralReportDate;
    }

    public void setResult(Result result) {
        this.result.setValue(result);
    }

    public Result getResult() {
        return (Result) result.getValue();
    }
}
