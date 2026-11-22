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

/**
 * Append-only audit row: a single Referral.status transition.
 * {@code fromStatus} is null on the initial creation row written when the
 * Referral is first persisted at DRAFT. {@code changedByUserId} is "1" for
 * FHIR/system-triggered transitions per the OE sys-user convention.
 */
public class ReferralStatusHistory extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private String referralId;
    private ReferralStatus fromStatus;
    private ReferralStatus toStatus;
    private String changedByUserId;
    private Timestamp changedAt;
    private String notes;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public ReferralStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(ReferralStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public ReferralStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(ReferralStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(String changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    public Timestamp getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Timestamp changedAt) {
        this.changedAt = changedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
