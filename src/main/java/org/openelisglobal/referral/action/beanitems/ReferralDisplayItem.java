package org.openelisglobal.referral.action.beanitems;

import org.openelisglobal.referral.valueholder.ReferralStatus;

public class ReferralDisplayItem {

    private String resultDate;
    private String accessionNumber;
    private String referredSendDate;
    private ReferralStatus referralStatus;
    private String referralStatusDisplay;
    private String patientLastName;
    private String patientFirstName;
    private String referringTestName;
    private String referralResultsDisplay;
    private String referenceLabDisplay;
    private String notes;
    private String analysisId;

    public String getResultDate() {
        return resultDate;
    }

    public void setResultDate(String resultDate) {
        this.resultDate = resultDate;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getReferredSendDate() {
        return referredSendDate;
    }

    public void setReferredSendDate(String referredSendDate) {
        this.referredSendDate = referredSendDate;
    }

    public ReferralStatus getReferralStatus() {
        return referralStatus;
    }

    public void setReferralStatus(ReferralStatus referralStatus) {
        this.referralStatus = referralStatus;
    }

    public String getReferralStatusDisplay() {
        return referralStatusDisplay;
    }

    public void setReferralStatusDisplay(String referralStatusDisplay) {
        this.referralStatusDisplay = referralStatusDisplay;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public String getReferringTestName() {
        return referringTestName;
    }

    public void setReferringTestName(String referringTestName) {
        this.referringTestName = referringTestName;
    }

    public String getReferralResultsDisplay() {
        return referralResultsDisplay;
    }

    public void setReferralResultsDisplay(String referralResultsDisplay) {
        this.referralResultsDisplay = referralResultsDisplay;
    }

    public String getReferenceLabDisplay() {
        return referenceLabDisplay;
    }

    public void setReferenceLabDisplay(String referenceLabDisplay) {
        this.referenceLabDisplay = referenceLabDisplay;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
}
