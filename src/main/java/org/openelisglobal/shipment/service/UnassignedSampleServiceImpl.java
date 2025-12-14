package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.referral.dao.ReferralDAO;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.referral.valueholder.ReferralStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for unassigned referral samples
 * Uses the existing referral table instead of a separate unassigned_sample table
 */
@Service
@Transactional
public class UnassignedSampleServiceImpl implements UnassignedSampleService {

    private static final Logger logger = LoggerFactory.getLogger(UnassignedSampleServiceImpl.class);

    @Autowired
    private ReferralDAO referralDAO;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUnassignedSamplesForDashboard() {
        try {
            // Get all referrals that are not assigned to a box and not lost
            List<Referral> referrals = referralDAO.getUnassignedReferrals();
            List<Map<String, Object>> result = new ArrayList<>();

            // Compile all DTOs within transaction
            // Note: lost and canceled referrals are already filtered in SQL
            for (Referral referral : referrals) {
                Map<String, Object> sampleData = compileSampleData(referral);
                result.add(sampleData);
            }

            logger.info("Retrieved {} unassigned samples for dashboard", result.size());
            return result;
        } catch (Exception e) {
            logger.error("Error getting unassigned samples for dashboard", e);
            throw new LIMSRuntimeException("Error getting unassigned samples for dashboard", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUnassignedSamplesByDestinationFacility(Integer facilityId) {
        try {
            List<Referral> referrals = referralDAO.getUnassignedReferrals();
            List<Map<String, Object>> result = new ArrayList<>();

            // Note: lost and canceled referrals are already filtered in SQL
            for (Referral referral : referrals) {
                // Filter by facility
                if (referral.getOrganization() != null &&
                    referral.getOrganization().getId().equals(facilityId.toString())) {
                    Map<String, Object> sampleData = compileSampleData(referral);
                    result.add(sampleData);
                }
            }

            logger.info("Retrieved {} unassigned samples for facility {}", result.size(), facilityId);
            return result;
        } catch (Exception e) {
            logger.error("Error getting unassigned samples by destination facility", e);
            throw new LIMSRuntimeException("Error getting unassigned samples by destination facility", e);
        }
    }

    @Override
    public void assignSampleToBox(String referralId, String boxId, String currentUserId) {
        try {
            Referral referral = referralDAO.get(referralId).orElseThrow(
                () -> new IllegalArgumentException("Referral not found with ID: " + referralId)
            );

            if (Boolean.TRUE.equals(referral.getLostStatus())) {
                throw new IllegalStateException("Cannot assign a lost sample to a box");
            }

            if (referral.isCanceled()) {
                throw new IllegalStateException("Cannot assign a canceled referral to a box");
            }

            referral.setAssignedToBoxId(boxId);
            referral.setSysUserId(currentUserId);
            referral.setLastupdated(new Timestamp(System.currentTimeMillis()));

            referralDAO.update(referral);
            logger.info("Assigned referral {} to box {}", referralId, boxId);
        } catch (Exception e) {
            logger.error("Error assigning sample to box", e);
            throw new LIMSRuntimeException("Error assigning sample to box", e);
        }
    }

    @Override
    public void markSampleAsLost(String referralId, String reason, String currentUserId) {
        try {
            Referral referral = referralDAO.get(referralId).orElseThrow(
                () -> new IllegalArgumentException("Referral not found with ID: " + referralId)
            );

            if (referral.isAssignedToBox()) {
                throw new IllegalStateException("Cannot mark as lost a sample already assigned to a box");
            }

            referral.setLostStatus(true);
            referral.setLostDate(new Timestamp(System.currentTimeMillis()));
            referral.setLostReason(reason);
            referral.setSysUserId(currentUserId);
            referral.setLastupdated(new Timestamp(System.currentTimeMillis()));

            referralDAO.update(referral);
            logger.info("Marked referral {} as lost", referralId);
        } catch (Exception e) {
            logger.error("Error marking sample as lost", e);
            throw new LIMSRuntimeException("Error marking sample as lost", e);
        }
    }

    @Override
    public void cancelReferral(String referralId, String reason, String currentUserId) {
        try {
            Referral referral = referralDAO.get(referralId).orElseThrow(
                () -> new IllegalArgumentException("Referral not found with ID: " + referralId)
            );

            if (referral.isAssignedToBox()) {
                throw new IllegalStateException("Cannot cancel a referral already assigned to a box");
            }

            referral.setStatus(ReferralStatus.CANCELED);
            referral.setCancelDate(new Timestamp(System.currentTimeMillis()));
            referral.setCancelReason(reason);
            referral.setSysUserId(currentUserId);
            referral.setLastupdated(new Timestamp(System.currentTimeMillis()));

            referralDAO.update(referral);
            logger.info("Canceled referral {}", referralId);
        } catch (Exception e) {
            logger.error("Error canceling referral", e);
            throw new LIMSRuntimeException("Error canceling referral", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int countUnassignedSamplesByFacility(Integer facilityId) {
        try {
            List<Referral> referrals = referralDAO.getUnassignedReferrals();
            int count = 0;

            for (Referral referral : referrals) {
                if (Boolean.TRUE.equals(referral.getLostStatus()) || referral.isCanceled()) {
                    continue;
                }

                if (referral.getOrganization() != null &&
                    referral.getOrganization().getId().equals(facilityId.toString())) {
                    count++;
                }
            }

            return count;
        } catch (Exception e) {
            logger.error("Error counting unassigned samples by facility", e);
            throw new LIMSRuntimeException("Error counting unassigned samples by facility", e);
        }
    }

    /**
     * Compile all sample data within transaction to prevent LazyInitializationException
     */
    private Map<String, Object> compileSampleData(Referral referral) {
        Map<String, Object> sampleData = new HashMap<>();

        sampleData.put("id", referral.getId());
        sampleData.put("referralDate", referral.getRequestDate());
        sampleData.put("priority", referral.getPriority() != null ? referral.getPriority() : "Normal");

        // Calculate days unassigned
        if (referral.getRequestDate() != null) {
            long daysDiff = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - referral.getRequestDate().getTime()
            );
            sampleData.put("daysUnassigned", daysDiff);
        } else {
            sampleData.put("daysUnassigned", 0L);
        }

        // Get sample information from analysis
        Analysis analysis = referral.getAnalysis();
        if (analysis != null) {
            Sample sample = analysis.getSampleItem().getSample();
            if (sample != null) {
                sampleData.put("accessionNumber", sample.getAccessionNumber());
                sampleData.put("sampleId", sample.getId());
            }

            // Test information
            if (analysis.getTest() != null) {
                sampleData.put("referralTestName", analysis.getTest().getLocalizedName());
                sampleData.put("testId", analysis.getTest().getId());
            }
        }

        // Destination facility information
        if (referral.getOrganization() != null) {
            sampleData.put("destinationFacilityName", referral.getOrganization().getOrganizationName());
            sampleData.put("destinationFacilityId", referral.getOrganization().getId());
        } else if (referral.getOrganizationName() != null) {
            sampleData.put("destinationFacilityName", referral.getOrganizationName());
        }

        // Referral reason
        sampleData.put("referralReasonId", referral.getReferralReasonId());

        return sampleData;
    }
}
