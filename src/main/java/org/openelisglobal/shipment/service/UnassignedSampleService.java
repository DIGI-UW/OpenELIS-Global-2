package org.openelisglobal.shipment.service;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing unassigned referral samples Uses the existing referral
 * table instead of a separate unassigned_sample table
 */
public interface UnassignedSampleService {

    /**
     * Get unassigned samples for dashboard with metadata Services MUST compile all
     * DTOs within transaction to prevent LazyInitializationException
     *
     * @return List of unassigned sample data as Maps
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<Map<String, Object>> getUnassignedSamplesForDashboard();

    /**
     * Get unassigned samples by destination facility
     *
     * @param facilityId Destination facility ID
     * @return List of unassigned sample data as Maps
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    List<Map<String, Object>> getUnassignedSamplesByDestinationFacility(Integer facilityId);

    /**
     * Assign a referral sample to a shipment box
     *
     * @param referralId    Referral ID
     * @param boxId         Box ID
     * @param currentUserId Current user ID for audit trail
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    void assignSampleToBox(String referralId, String boxId, String currentUserId);

    /**
     * Mark a referral sample as lost
     *
     * @param referralId    Referral ID
     * @param reason        Reason for marking as lost
     * @param currentUserId Current user ID for audit trail
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    void markSampleAsLost(String referralId, String reason, String currentUserId);

    /**
     * Cancel a referral
     *
     * @param referralId    Referral ID
     * @param reason        Reason for cancellation
     * @param currentUserId Current user ID for audit trail
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_MANAGE')")
    void cancelReferral(String referralId, String reason, String currentUserId);

    /**
     * Count unassigned samples by destination facility
     *
     * @param facilityId Destination facility ID
     * @return Count of unassigned samples
     */
    @PreAuthorize("hasAuthority('PRIV_SHIPMENT_VIEW')")
    int countUnassignedSamplesByFacility(Integer facilityId);
}
