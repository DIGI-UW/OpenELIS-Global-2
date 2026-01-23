package org.openelisglobal.biorepository.dao;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification;
import org.openelisglobal.biorepository.valueholder.DocumentationVerification.OverallStatus;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for DocumentationVerification entity operations. Documentation
 * verification is now at shipment level (one verification per shipment).
 */
public interface DocumentationVerificationDAO extends BaseDAO<DocumentationVerification, Integer> {

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
}
