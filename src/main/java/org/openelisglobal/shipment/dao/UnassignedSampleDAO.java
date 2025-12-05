package org.openelisglobal.shipment.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.shipment.valueholder.UnassignedSample;

public interface UnassignedSampleDAO extends BaseDAO<UnassignedSample, Integer> {

    /**
     * Find unassigned sample by sample ID
     *
     * @param sampleId Sample ID
     * @return UnassignedSample or null if not found
     */
    UnassignedSample findBySampleId(Integer sampleId);

    /**
     * Find all unassigned samples (not yet assigned to a box)
     *
     * @return List of unassigned samples
     */
    List<UnassignedSample> findAllUnassigned();

    /**
     * Find unassigned samples by destination facility
     *
     * @param facilityId Destination facility ID
     * @return List of unassigned samples
     */
    List<UnassignedSample> findByDestinationFacilityId(Integer facilityId);

    /**
     * Find unassigned samples by priority
     *
     * @param priority Priority level
     * @return List of unassigned samples
     */
    List<UnassignedSample> findByPriority(String priority);

    /**
     * Find unassigned samples by referral test
     *
     * @param testId Referral test ID
     * @return List of unassigned samples
     */
    List<UnassignedSample> findByReferralTestId(Integer testId);

    /**
     * Count unassigned samples by destination facility
     *
     * @param facilityId Destination facility ID
     * @return Count of unassigned samples
     */
    int countByDestinationFacilityId(Integer facilityId);
}
