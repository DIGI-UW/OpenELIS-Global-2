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

import java.sql.Date;
import java.sql.Timestamp;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * S-14 / OGC-624 subcontract metadata for an outbound referral. Joined 1:1 to
 * {@link Referral} via referral.subcontract_id. Created at DRAFT on every Refer
 * Out save; metadata fields are populated as the user progresses through the
 * inter-lab transfer workflow.
 */
public class ReferralSubcontract extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private SubcontractStatus subcontractStatus = SubcontractStatus.DRAFT;
    private String agreementReference;
    private Timestamp handoffDatetime;
    private String handoffDatetimeForDisplay;
    private Date expectedReturnDate;
    private String expectedReturnDateForDisplay;
    private String cocContactName;
    private String cocContactPhone;
    private String cocContactEmail;
    private String subcontractNotes;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public SubcontractStatus getSubcontractStatus() {
        return subcontractStatus;
    }

    public void setSubcontractStatus(SubcontractStatus subcontractStatus) {
        this.subcontractStatus = subcontractStatus;
    }

    public String getAgreementReference() {
        return agreementReference;
    }

    public void setAgreementReference(String agreementReference) {
        this.agreementReference = agreementReference;
    }

    public Timestamp getHandoffDatetime() {
        return handoffDatetime;
    }

    public void setHandoffDatetime(Timestamp handoffDatetime) {
        this.handoffDatetime = handoffDatetime;
        this.handoffDatetimeForDisplay = DateUtil.convertTimestampToStringDateAndTime(handoffDatetime);
    }

    public String getHandoffDatetimeForDisplay() {
        return handoffDatetimeForDisplay;
    }

    public void setHandoffDatetimeForDisplay(String handoffDatetimeForDisplay) {
        this.handoffDatetimeForDisplay = handoffDatetimeForDisplay;
        this.handoffDatetime = DateUtil.convertStringDateToTimestampLenient(handoffDatetimeForDisplay);
    }

    public Date getExpectedReturnDate() {
        return expectedReturnDate;
    }

    public void setExpectedReturnDate(Date expectedReturnDate) {
        this.expectedReturnDate = expectedReturnDate;
        this.expectedReturnDateForDisplay = expectedReturnDate == null ? null
                : DateUtil.convertSqlDateToStringDate(expectedReturnDate);
    }

    public String getExpectedReturnDateForDisplay() {
        return expectedReturnDateForDisplay;
    }

    public void setExpectedReturnDateForDisplay(String expectedReturnDateForDisplay) {
        this.expectedReturnDateForDisplay = expectedReturnDateForDisplay;
        this.expectedReturnDate = DateUtil.convertStringDateToSqlDate(expectedReturnDateForDisplay);
    }

    public String getCocContactName() {
        return cocContactName;
    }

    public void setCocContactName(String cocContactName) {
        this.cocContactName = cocContactName;
    }

    public String getCocContactPhone() {
        return cocContactPhone;
    }

    public void setCocContactPhone(String cocContactPhone) {
        this.cocContactPhone = cocContactPhone;
    }

    public String getCocContactEmail() {
        return cocContactEmail;
    }

    public void setCocContactEmail(String cocContactEmail) {
        this.cocContactEmail = cocContactEmail;
    }

    public String getSubcontractNotes() {
        return subcontractNotes;
    }

    public void setSubcontractNotes(String subcontractNotes) {
        this.subcontractNotes = subcontractNotes;
    }

    @Override
    public String getDefaultLocalizedName() {
        return agreementReference == null ? "" : agreementReference;
    }
}
