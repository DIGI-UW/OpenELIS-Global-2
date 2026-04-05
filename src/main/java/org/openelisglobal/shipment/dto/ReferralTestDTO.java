package org.openelisglobal.shipment.dto;

import java.sql.Timestamp;

/**
 * DTO representing a referral test associated with a sample item
 */
public class ReferralTestDTO {

    private String referralId;
    private String testName;
    private String organizationName;
    private Timestamp requestDate;
    private String status;

    public ReferralTestDTO() {
    }

    public ReferralTestDTO(String referralId, String testName, String organizationName, Timestamp requestDate,
            String status) {
        this.referralId = referralId;
        this.testName = testName;
        this.organizationName = organizationName;
        this.requestDate = requestDate;
        this.status = status;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public Timestamp getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Timestamp requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
