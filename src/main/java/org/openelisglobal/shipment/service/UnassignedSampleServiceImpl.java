package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.shipment.dao.UnassignedSampleDAO;
import org.openelisglobal.shipment.valueholder.UnassignedSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for unassigned sample operations
 */
@Service
@Transactional
public class UnassignedSampleServiceImpl implements UnassignedSampleService {

    private static final Logger logger = LoggerFactory.getLogger(UnassignedSampleServiceImpl.class);

    @Autowired
    private UnassignedSampleDAO unassignedSampleDAO;

    @Override
    @Transactional(readOnly = true)
    public UnassignedSample getUnassignedSampleById(Integer id) {
        try {
            return unassignedSampleDAO.get(id).orElse(null);
        } catch (Exception e) {
            logger.error("Error getting unassigned sample by ID", e);
            throw new LIMSRuntimeException("Error getting unassigned sample by ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UnassignedSample getUnassignedSampleBySampleId(Integer sampleId) {
        try {
            return unassignedSampleDAO.findBySampleId(sampleId);
        } catch (Exception e) {
            logger.error("Error getting unassigned sample by sample ID", e);
            throw new LIMSRuntimeException("Error getting unassigned sample by sample ID", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnassignedSample> getAllUnassignedSamples() {
        try {
            return unassignedSampleDAO.findAllUnassigned();
        } catch (Exception e) {
            logger.error("Error getting all unassigned samples", e);
            throw new LIMSRuntimeException("Error getting all unassigned samples", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnassignedSample> getUnassignedSamplesByDestinationFacility(Integer facilityId) {
        try {
            return unassignedSampleDAO.findByDestinationFacilityId(facilityId);
        } catch (Exception e) {
            logger.error("Error getting unassigned samples by destination facility", e);
            throw new LIMSRuntimeException("Error getting unassigned samples by destination facility", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnassignedSample> getUnassignedSamplesByPriority(String priority) {
        try {
            return unassignedSampleDAO.findByPriority(priority);
        } catch (Exception e) {
            logger.error("Error getting unassigned samples by priority", e);
            throw new LIMSRuntimeException("Error getting unassigned samples by priority", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnassignedSample> getUnassignedSamplesByReferralTest(Integer testId) {
        try {
            return unassignedSampleDAO.findByReferralTestId(testId);
        } catch (Exception e) {
            logger.error("Error getting unassigned samples by referral test", e);
            throw new LIMSRuntimeException("Error getting unassigned samples by referral test", e);
        }
    }

    @Override
    public UnassignedSample createUnassignedSample(UnassignedSample unassignedSample) {
        try {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            unassignedSample.setCreatedDate(now);
            unassignedSample.setLastupdated(now);

            Integer id = unassignedSampleDAO.insert(unassignedSample);
            logger.info("Created unassigned sample with ID: {}", id);

            return unassignedSampleDAO.get(id).orElse(null);
        } catch (Exception e) {
            logger.error("Error creating unassigned sample", e);
            throw new LIMSRuntimeException("Error creating unassigned sample", e);
        }
    }

    @Override
    public void markSampleAsAssigned(Integer id) {
        try {
            UnassignedSample unassignedSample = unassignedSampleDAO.get(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unassigned sample not found with ID: " + id));

            unassignedSample.setAssignedDate(new Timestamp(System.currentTimeMillis()));
            unassignedSample.setLastupdated(new Timestamp(System.currentTimeMillis()));

            unassignedSampleDAO.update(unassignedSample);
            logger.info("Marked unassigned sample {} as assigned", id);
        } catch (Exception e) {
            logger.error("Error marking sample as assigned", e);
            throw new LIMSRuntimeException("Error marking sample as assigned", e);
        }
    }

    @Override
    public void deleteUnassignedSample(Integer id) {
        try {
            UnassignedSample unassignedSample = unassignedSampleDAO.get(id).orElse(null);
            if (unassignedSample != null) {
                unassignedSampleDAO.delete(unassignedSample);
                logger.info("Deleted unassigned sample with ID: {}", id);
            }
        } catch (Exception e) {
            logger.error("Error deleting unassigned sample", e);
            throw new LIMSRuntimeException("Error deleting unassigned sample", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUnassignedSamplesForDashboard() {
        try {
            List<UnassignedSample> samples = unassignedSampleDAO.findAllUnassigned();
            List<Map<String, Object>> result = new ArrayList<>();

            // Compile all DTOs within transaction
            for (UnassignedSample sample : samples) {
                Map<String, Object> sampleData = new HashMap<>();
                sampleData.put("id", sample.getId());
                sampleData.put("createdDate", sample.getCreatedDate());
                sampleData.put("priority", sample.getPriority());

                // Eagerly fetch related data within transaction
                if (sample.getSample() != null) {
                    sampleData.put("sampleId", sample.getSample().getId());
                    sampleData.put("accessionNumber", sample.getSample().getAccessionNumber());
                }

                if (sample.getReferralTest() != null) {
                    sampleData.put("testName", sample.getReferralTest().getLocalizedName());
                }

                if (sample.getDestinationFacility() != null) {
                    sampleData.put("destinationFacilityName", sample.getDestinationFacility().getOrganizationName());
                }

                result.add(sampleData);
            }

            return result;
        } catch (Exception e) {
            logger.error("Error getting unassigned samples for dashboard", e);
            throw new LIMSRuntimeException("Error getting unassigned samples for dashboard", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countUnassignedSamplesByFacility(Integer facilityId) {
        try {
            return unassignedSampleDAO.countByDestinationFacilityId(facilityId);
        } catch (Exception e) {
            logger.error("Error counting unassigned samples by facility", e);
            throw new LIMSRuntimeException("Error counting unassigned samples by facility", e);
        }
    }
}
