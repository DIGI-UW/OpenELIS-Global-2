package org.openelisglobal.biorepository.service;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification.OverallStatus;
import org.openelisglobal.common.service.BaseObjectService;

/**
 * Service interface for DocumentationVerification entity operations.
 * Documentation verification is now at shipment level (one verification per
 * shipment).
 */
public interface DocumentationVerificationService extends BaseObjectService<DocumentationVerification, Integer> {

    /**
     * Find verification record for a shipment.
     *
     * @param shipmentId the shipment ID
     * @return verification record or null if not found
     */
    DocumentationVerification getByShipmentId(Integer shipmentId);

    /**
     * Find verifications by overall status.
     *
     * @param status the overall verification status
     * @return list of verifications ordered by last updated descending
     */
    List<DocumentationVerification> getByOverallStatus(OverallStatus status);

    /**
     * Find verifications by verifier user.
     *
     * @param verifiedByUserId the verifier's user ID
     * @return list of verifications ordered by verified timestamp descending
     */
    List<DocumentationVerification> getByVerifiedByUserId(Integer verifiedByUserId);

    /**
     * Find pending verifications (incomplete documentation).
     *
     * @param limit maximum results
     * @return list of pending verifications
     */
    List<DocumentationVerification> getPendingVerifications(int limit);

    /**
     * Count verifications by status.
     *
     * @param status the overall verification status
     * @return count of matching verifications
     */
    long countByOverallStatus(OverallStatus status);

    /**
     * Check if a verification record exists for a shipment.
     *
     * @param shipmentId the shipment ID
     * @return true if verification exists
     */
    boolean existsByShipmentId(Integer shipmentId);

    /**
     * Create a new verification record for a shipment with default pending status.
     *
     * @param shipmentId the shipment ID
     * @param sysUserId  the system user ID
     * @return the created verification record
     */
    DocumentationVerification createForShipment(Integer shipmentId, String sysUserId);

    /**
     * Update verification item status.
     *
     * @param verificationId  the verification ID
     * @param itemName        the item name (e.g., "sampleIdentifiers",
     *                        "projectLinkage")
     * @param verified        whether the item is verified
     * @param notApplicable   whether the item is N/A
     * @param naJustification justification if N/A
     * @return the updated verification record
     */
    DocumentationVerification updateVerificationItem(Integer verificationId, String itemName, boolean verified,
            boolean notApplicable, String naJustification);

    /**
     * Complete verification and update shipment documentation status.
     *
     * @param verificationId the verification ID
     * @param verifierUserId the verifier's user ID
     * @return the completed verification record
     */
    DocumentationVerification completeVerification(Integer verificationId, Integer verifierUserId);

    /**
     * Quarantine shipment due to failed verification.
     *
     * @param verificationId   the verification ID
     * @param verifierUserId   the verifier's user ID
     * @param quarantineReason the reason for quarantine
     * @return the updated verification record
     */
    DocumentationVerification quarantineShipment(Integer verificationId, Integer verifierUserId,
            String quarantineReason);
}
