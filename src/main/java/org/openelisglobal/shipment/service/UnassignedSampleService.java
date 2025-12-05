package org.openelisglobal.shipment.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.shipment.valueholder.UnassignedSample;

public interface UnassignedSampleService {

    /**
     * Get unassigned sample by ID
     *
     * @param id Unassigned sample ID
     * @return UnassignedSample or null if not found
     */
    UnassignedSample getUnassignedSampleById(Integer id);

    /**
     * Get unassigned sample by sample ID
     *
     * @param sampleId Sample ID
     * @return UnassignedSample or null if not found
     */
    UnassignedSample getUnassignedSampleBySampleId(Integer sampleId);

    /**
     * Get all unassigned samples (not yet assigned to a box)
     *
     * @return List of unassigned samples
     */
    List<UnassignedSample> getAllUnassignedSamples();

    /**
     * Get unassigned samples by destination facility
     *
     * @param facilityId Destination facility ID
     * @return List of unassigned samples
     */
    List<UnassignedSample> getUnassignedSamplesByDestinationFacility(Integer facilityId);

    /**
     * Get unassigned samples by priority
     *
     * @param priority Priority level
     * @return List of unassigned samples
     */
    List<UnassignedSample> getUnassignedSamplesByPriority(String priority);

    /**
     * Get unassigned samples by referral test
     *
     * @param testId Referral test ID
     * @return List of unassigned samples
     */
    List<UnassignedSample> getUnassignedSamplesByReferralTest(Integer testId);

    /**
     * Create a new unassigned sample entry
     *
     * @param unassignedSample UnassignedSample to create
     * @return Created UnassignedSample with ID
     */
    UnassignedSample createUnassignedSample(UnassignedSample unassignedSample);

    /**
     * Mark sample as assigned to a box
     *
     * @param id Unassigned sample ID
     */
    void markSampleAsAssigned(Integer id);

    /**
     * Delete unassigned sample entry
     *
     * @param id Unassigned sample ID
     */
    void deleteUnassignedSample(Integer id);

    /**
     * Get unassigned samples for dashboard with metadata Services MUST compile all
     * DTOs within transaction to prevent LazyInitializationException
     *
     * @return List of unassigned sample data as Maps
     */
    List<Map<String, Object>> getUnassignedSamplesForDashboard();

    /**
     * Count unassigned samples by destination facility
     *
     * @param facilityId Destination facility ID
     * @return Count of unassigned samples
     */
    int countUnassignedSamplesByFacility(Integer facilityId);
}
