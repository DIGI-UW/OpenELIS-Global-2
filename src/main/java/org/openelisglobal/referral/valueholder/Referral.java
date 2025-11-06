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
import java.util.UUID;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.organization.valueholder.Organization;

public class Referral extends BaseObject<String> {

    private static final long serialVersionUID = 1L;
    private String id;
    private String organizationName;
    private Timestamp requestDate;
    private Timestamp sendReadyDate;
    private Timestamp sentDate;
    private Timestamp resultRecievedDate;
    private String referralReasonId;
    private String referralTypeId;
    private String requesterName;
    private ReferralStatus status;
    private UUID fhirUuid;

    private ValueHolderInterface analysis = new ValueHolder();
    private ValueHolderInterface organization = new ValueHolder();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public Timestamp getSendReadyDate() {
        return sendReadyDate;
    }

    public void setSendReadyDate(Timestamp sendReadyDate) {
        this.sendReadyDate = sendReadyDate;
    }

    public Timestamp getSentDate() {
        return sentDate;
    }

    public void setSentDate(Timestamp sentDate) {
        this.sentDate = sentDate;
    }

    public Timestamp getResultRecievedDate() {
        return resultRecievedDate;
    }

    public void setResultRecievedDate(Timestamp resultRecievedDate) {
        this.resultRecievedDate = resultRecievedDate;
    }

    public String getReferralTypeId() {
        return referralTypeId;
    }

    public void setReferralTypeId(String referralTypeId) {
        this.referralTypeId = referralTypeId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public Analysis getAnalysis() {
        return (Analysis) analysis.getValue();
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis.setValue(analysis);
    }

    public Organization getOrganization() {
        return (Organization) organization.getValue();
    }

    public void setOrganization(Organization organization) {
        this.organization.setValue(organization);
    }

    public boolean isCanceled() {
        return ReferralStatus.CANCELED.equals(status);
    }

    public void setRequestDate(Timestamp requestDate) {
        this.requestDate = requestDate;
    }

    public Timestamp getRequestDate() {
        return requestDate;
    }

    public void setReferralReasonId(String referralReasonId) {
        this.referralReasonId = referralReasonId;
    }

    public String getReferralReasonId() {
        return referralReasonId;
    }

    public ReferralStatus getStatus() {
        return status;
    }

    public void setStatus(ReferralStatus status) {
        this.status = status;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }
}
