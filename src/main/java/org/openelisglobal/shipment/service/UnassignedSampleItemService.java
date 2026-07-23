package org.openelisglobal.shipment.service;

import java.util.List;
import org.openelisglobal.shipment.dto.SampleItemDTO;

/**
 * Service for managing unassigned sample items (sample items not yet in any
 * shipping box).
 *
 * This service groups referrals by SampleItem to provide the correct
 * granularity for shipment operations.
 */
public interface UnassignedSampleItemService {

    /**
     * Search for unassigned sample items by accession number. Groups referrals by
     * SampleItem and returns only SampleItems not yet assigned to a box. Each
     * SampleItemDTO includes the type of sample and all associated referral tests.
     *
     * @param searchTerm - Partial or full accession number
     * @return List of SampleItemDTO with grouped referrals and type of sample
     */
    List<SampleItemDTO> searchUnassignedByAccessionNumber(String searchTerm);

    /**
     * Get all unassigned sample items (not in any box). Groups by SampleItem,
     * showing type of sample and all associated tests. Excludes sample items that
     * are already assigned to boxes.
     *
     * @return List of SampleItemDTO with grouped referrals
     */
    List<SampleItemDTO> getAllUnassigned();

    /**
     * Check if a sample item is already assigned to a box.
     *
     * @param sampleItemId - The SampleItem PK
     * @return true if assigned, false otherwise
     */
    boolean isAlreadyAssigned(String sampleItemId);

    /**
     * Get the SampleItemDTO for a specific sample item, including all its referral
     * tests.
     *
     * @param sampleItemId - The SampleItem PK
     * @return SampleItemDTO with referral tests, or null if not found
     */
    SampleItemDTO getSampleItemById(String sampleItemId);
}
