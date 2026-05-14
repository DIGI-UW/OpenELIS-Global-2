package org.openelisglobal.shipment.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a sample item (physical specimen) with its associated
 * referral tests.
 *
 * This is the correct granularity for shipment operations because: - A Sample
 * can have multiple SampleItems (e.g., blood → serum + plasma) - Each
 * SampleItem has a specific TypeOfSample (serum, urine, etc.) -
 * Referrals/Analyses are linked to SampleItems, not Samples
 *
 * In the frontend, this is displayed as a "sample" to match user mental model.
 */
public class SampleItemDTO {

    private String sampleItemId; // PK of SampleItem
    private String accessionNumber; // From Sample
    private String typeOfSample; // TypeOfSample.description (e.g., "Sérum", "Urines")
    private String typeOfSampleId; // TypeOfSample PK
    private List<ReferralTestDTO> referralTests = new ArrayList<>(); // Grouped tests for this SampleItem
    private Timestamp collectionDate; // From Sample
    private Integer assignedBoxId; // If already assigned to a box
    private String assignedBoxName; // If already assigned
    private Integer boxSampleItemId; // PK of BoxSampleItem (for reception status updates)
    private String receptionStatus; // Reception status (PENDING, RECEIVED_GOOD, etc.)
    private String receptionNotes; // Non-conformity notes from reception

    public SampleItemDTO() {
    }

    public SampleItemDTO(String sampleItemId, String accessionNumber, String typeOfSample, String typeOfSampleId,
            Timestamp collectionDate) {
        this.sampleItemId = sampleItemId;
        this.accessionNumber = accessionNumber;
        this.typeOfSample = typeOfSample;
        this.typeOfSampleId = typeOfSampleId;
        this.collectionDate = collectionDate;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getTypeOfSample() {
        return typeOfSample;
    }

    public void setTypeOfSample(String typeOfSample) {
        this.typeOfSample = typeOfSample;
    }

    public String getTypeOfSampleId() {
        return typeOfSampleId;
    }

    public void setTypeOfSampleId(String typeOfSampleId) {
        this.typeOfSampleId = typeOfSampleId;
    }

    public List<ReferralTestDTO> getReferralTests() {
        return referralTests;
    }

    public void setReferralTests(List<ReferralTestDTO> referralTests) {
        this.referralTests = referralTests;
    }

    public void addReferralTest(ReferralTestDTO referralTest) {
        this.referralTests.add(referralTest);
    }

    public Timestamp getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Timestamp collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Integer getAssignedBoxId() {
        return assignedBoxId;
    }

    public void setAssignedBoxId(Integer assignedBoxId) {
        this.assignedBoxId = assignedBoxId;
    }

    public String getAssignedBoxName() {
        return assignedBoxName;
    }

    public void setAssignedBoxName(String assignedBoxName) {
        this.assignedBoxName = assignedBoxName;
    }

    public Integer getBoxSampleItemId() {
        return boxSampleItemId;
    }

    public void setBoxSampleItemId(Integer boxSampleItemId) {
        this.boxSampleItemId = boxSampleItemId;
    }

    public String getReceptionStatus() {
        return receptionStatus;
    }

    public void setReceptionStatus(String receptionStatus) {
        this.receptionStatus = receptionStatus;
    }

    public String getReceptionNotes() {
        return receptionNotes;
    }

    public void setReceptionNotes(String receptionNotes) {
        this.receptionNotes = receptionNotes;
    }

    /**
     * Get a comma-separated string of all test names for this sample item
     *
     * @return Comma-separated test names (e.g., "HIV, TB, Malaria")
     */
    public String getReferralTestsAsString() {
        if (referralTests == null || referralTests.isEmpty()) {
            return "";
        }
        return referralTests.stream().map(ReferralTestDTO::getTestName).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
