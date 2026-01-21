package org.openelisglobal.biorepository.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Entity representing the 7-point documentation verification checklist for a
 * biorepository sample. Tracks verification status for each checkpoint: -
 * Points 1-2: Auto-verified (system validates against shipment/project data) -
 * Points 3-5: Manual verification required - Points 6-7: Conditional (N/A
 * allowed with justification)
 *
 * Part of the ISO 20387:2018 compliant sample intake workflow.
 */
@Entity
@Table(name = "documentation_verification", schema = "clinlims")
public class DocumentationVerification extends BaseObject<Integer> {

    /**
     * Status for individual verification checkpoints.
     */
    public enum VerificationItemStatus {
        PENDING, // Not yet verified
        VERIFIED, // Verification complete
        N_A // Not applicable (requires justification)
    }

    /**
     * Overall verification status for the sample.
     */
    public enum OverallStatus {
        PENDING, // Verification in progress
        VERIFIED, // All required checks passed
        QUARANTINE // Verification failed, sample quarantined
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "doc_verification_generator")
    @SequenceGenerator(name = "doc_verification_generator", sequenceName = "documentation_verification_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotNull(message = "Shipment is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "documentationVerification" })
    private Shipment shipment;

    // ========================================
    // 7-Point Verification Checklist - Boolean checks
    // ========================================

    // Points 1-2: Auto-verified (system validates against shipment/project data)
    @Column(name = "check_sample_identifiers", nullable = false)
    private boolean checkSampleIdentifiers = false;

    @Column(name = "check_project_linkage", nullable = false)
    private boolean checkProjectLinkage = false;

    // Points 3-5: Manual verification
    @Column(name = "check_ethics_approval", nullable = false)
    private boolean checkEthicsApproval = false;

    @Column(name = "check_biosafety_match", nullable = false)
    private boolean checkBiosafetyMatch = false;

    @Column(name = "check_packaging_integrity", nullable = false)
    private boolean checkPackagingIntegrity = false;

    // Points 6-7: Conditional (N/A allowed with justification)
    @Column(name = "check_consent_record", nullable = false)
    private boolean checkConsentRecord = false;

    @Column(name = "check_mta_documented", nullable = false)
    private boolean checkMtaDocumented = false;

    // ========================================
    // Status per checkpoint
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "status_sample_identifiers", length = 20)
    private VerificationItemStatus statusSampleIdentifiers = VerificationItemStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_project_linkage", length = 20)
    private VerificationItemStatus statusProjectLinkage = VerificationItemStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_ethics_approval", length = 20)
    private VerificationItemStatus statusEthicsApproval = VerificationItemStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_consent_record", length = 20)
    private VerificationItemStatus statusConsentRecord = VerificationItemStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_mta_documented", length = 20)
    private VerificationItemStatus statusMtaDocumented = VerificationItemStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_biosafety_match", length = 20)
    private VerificationItemStatus statusBiosafetyMatch = VerificationItemStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_packaging_integrity", length = 20)
    private VerificationItemStatus statusPackagingIntegrity = VerificationItemStatus.PENDING;

    // ========================================
    // N/A justifications for conditional items
    // ========================================

    @Column(name = "na_justification_consent", columnDefinition = "TEXT")
    private String naJustificationConsent;

    @Column(name = "na_justification_mta", columnDefinition = "TEXT")
    private String naJustificationMta;

    // ========================================
    // Overall verification status
    // ========================================

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 20)
    private OverallStatus overallStatus = OverallStatus.PENDING;

    // ========================================
    // Verification metadata
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SystemUser verifiedByUser;

    @Column(name = "verified_timestamp")
    private Timestamp verifiedTimestamp;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    // Default constructor required by JPA
    public DocumentationVerification() {
    }

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    /**
     * Constructor with required shipment.
     *
     * @param shipment the shipment this verification belongs to
     */
    public DocumentationVerification(Shipment shipment) {
        this.shipment = shipment;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    // Auto-verified checks (Points 1-2)
    public boolean isCheckSampleIdentifiers() {
        return checkSampleIdentifiers;
    }

    public void setCheckSampleIdentifiers(boolean checkSampleIdentifiers) {
        this.checkSampleIdentifiers = checkSampleIdentifiers;
    }

    public boolean isCheckProjectLinkage() {
        return checkProjectLinkage;
    }

    public void setCheckProjectLinkage(boolean checkProjectLinkage) {
        this.checkProjectLinkage = checkProjectLinkage;
    }

    // Manual checks (Points 3-5)
    public boolean isCheckEthicsApproval() {
        return checkEthicsApproval;
    }

    public void setCheckEthicsApproval(boolean checkEthicsApproval) {
        this.checkEthicsApproval = checkEthicsApproval;
    }

    public boolean isCheckBiosafetyMatch() {
        return checkBiosafetyMatch;
    }

    public void setCheckBiosafetyMatch(boolean checkBiosafetyMatch) {
        this.checkBiosafetyMatch = checkBiosafetyMatch;
    }

    public boolean isCheckPackagingIntegrity() {
        return checkPackagingIntegrity;
    }

    public void setCheckPackagingIntegrity(boolean checkPackagingIntegrity) {
        this.checkPackagingIntegrity = checkPackagingIntegrity;
    }

    // Conditional checks (Points 6-7)
    public boolean isCheckConsentRecord() {
        return checkConsentRecord;
    }

    public void setCheckConsentRecord(boolean checkConsentRecord) {
        this.checkConsentRecord = checkConsentRecord;
    }

    public boolean isCheckMtaDocumented() {
        return checkMtaDocumented;
    }

    public void setCheckMtaDocumented(boolean checkMtaDocumented) {
        this.checkMtaDocumented = checkMtaDocumented;
    }

    // Status getters/setters
    public VerificationItemStatus getStatusSampleIdentifiers() {
        return statusSampleIdentifiers;
    }

    public void setStatusSampleIdentifiers(VerificationItemStatus statusSampleIdentifiers) {
        this.statusSampleIdentifiers = statusSampleIdentifiers;
    }

    public VerificationItemStatus getStatusProjectLinkage() {
        return statusProjectLinkage;
    }

    public void setStatusProjectLinkage(VerificationItemStatus statusProjectLinkage) {
        this.statusProjectLinkage = statusProjectLinkage;
    }

    public VerificationItemStatus getStatusEthicsApproval() {
        return statusEthicsApproval;
    }

    public void setStatusEthicsApproval(VerificationItemStatus statusEthicsApproval) {
        this.statusEthicsApproval = statusEthicsApproval;
    }

    public VerificationItemStatus getStatusConsentRecord() {
        return statusConsentRecord;
    }

    public void setStatusConsentRecord(VerificationItemStatus statusConsentRecord) {
        this.statusConsentRecord = statusConsentRecord;
    }

    public VerificationItemStatus getStatusMtaDocumented() {
        return statusMtaDocumented;
    }

    public void setStatusMtaDocumented(VerificationItemStatus statusMtaDocumented) {
        this.statusMtaDocumented = statusMtaDocumented;
    }

    public VerificationItemStatus getStatusBiosafetyMatch() {
        return statusBiosafetyMatch;
    }

    public void setStatusBiosafetyMatch(VerificationItemStatus statusBiosafetyMatch) {
        this.statusBiosafetyMatch = statusBiosafetyMatch;
    }

    public VerificationItemStatus getStatusPackagingIntegrity() {
        return statusPackagingIntegrity;
    }

    public void setStatusPackagingIntegrity(VerificationItemStatus statusPackagingIntegrity) {
        this.statusPackagingIntegrity = statusPackagingIntegrity;
    }

    // N/A justifications
    public String getNaJustificationConsent() {
        return naJustificationConsent;
    }

    public void setNaJustificationConsent(String naJustificationConsent) {
        this.naJustificationConsent = naJustificationConsent;
    }

    public String getNaJustificationMta() {
        return naJustificationMta;
    }

    public void setNaJustificationMta(String naJustificationMta) {
        this.naJustificationMta = naJustificationMta;
    }

    // Overall status
    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    // Verification metadata
    public SystemUser getVerifiedByUser() {
        return verifiedByUser;
    }

    public void setVerifiedByUser(SystemUser verifiedByUser) {
        this.verifiedByUser = verifiedByUser;
    }

    public Timestamp getVerifiedTimestamp() {
        return verifiedTimestamp;
    }

    public void setVerifiedTimestamp(Timestamp verifiedTimestamp) {
        this.verifiedTimestamp = verifiedTimestamp;
    }

    public String getVerificationNotes() {
        return verificationNotes;
    }

    public void setVerificationNotes(String verificationNotes) {
        this.verificationNotes = verificationNotes;
    }

    /**
     * Checks if all required verifications are complete (verified or N/A with
     * justification).
     * 
     * @return true if all required items are complete
     */
    public boolean isComplete() {
        return isItemComplete(statusSampleIdentifiers, null) && isItemComplete(statusProjectLinkage, null)
                && isItemComplete(statusEthicsApproval, null) && isItemComplete(statusBiosafetyMatch, null)
                && isItemComplete(statusPackagingIntegrity, null)
                && isItemComplete(statusConsentRecord, naJustificationConsent)
                && isItemComplete(statusMtaDocumented, naJustificationMta);
    }

    /**
     * Checks if a single verification item is complete. VERIFIED is always
     * complete. N/A requires justification for conditional items.
     */
    private boolean isItemComplete(VerificationItemStatus status, String naJustification) {
        if (VerificationItemStatus.VERIFIED.equals(status)) {
            return true;
        }
        if (VerificationItemStatus.N_A.equals(status)) {
            // N/A is complete only if justification provided (for items that require it)
            return naJustification == null || (naJustification != null && !naJustification.trim().isEmpty());
        }
        return false;
    }

    /**
     * Counts completed verification items.
     * 
     * @return number of items with VERIFIED or valid N/A status
     */
    public int getCompletedCount() {
        int count = 0;
        if (isItemComplete(statusSampleIdentifiers, null))
            count++;
        if (isItemComplete(statusProjectLinkage, null))
            count++;
        if (isItemComplete(statusEthicsApproval, null))
            count++;
        if (isItemComplete(statusBiosafetyMatch, null))
            count++;
        if (isItemComplete(statusPackagingIntegrity, null))
            count++;
        if (isItemComplete(statusConsentRecord, naJustificationConsent))
            count++;
        if (isItemComplete(statusMtaDocumented, naJustificationMta))
            count++;
        return count;
    }

    /**
     * Total number of verification items.
     * 
     * @return 7 (the number of verification checkpoints)
     */
    public int getTotalCount() {
        return 7;
    }

    /**
     * Updates overall status based on individual item statuses. Call this after
     * updating any individual status.
     */
    public void updateOverallStatus() {
        if (isComplete()) {
            this.overallStatus = OverallStatus.VERIFIED;
        } else {
            // Check if any item explicitly failed (future enhancement)
            this.overallStatus = OverallStatus.PENDING;
        }
    }
}
