package org.openelisglobal.shipment.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.referral.dao.ReferralDAO;
import org.openelisglobal.referral.valueholder.Referral;
import org.openelisglobal.shipment.dao.BoxSampleItemDAO;
import org.openelisglobal.shipment.dto.ReferralTestDTO;
import org.openelisglobal.shipment.dto.SampleItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnassignedSampleItemServiceImpl implements UnassignedSampleItemService {

    private static final Logger logger = LoggerFactory.getLogger(UnassignedSampleItemServiceImpl.class);

    @Autowired
    private ReferralDAO referralDAO;

    @Autowired
    private BoxSampleItemDAO boxSampleItemDAO;

    @Override
    @Transactional(readOnly = true)
    public List<SampleItemDTO> searchUnassignedByAccessionNumber(String searchTerm) {
        try {
            // Get all sample item IDs that are already in boxes
            List<String> excludedSampleItemIds = boxSampleItemDAO.getAllAssignedSampleItemIds();

            // Search for sample items with unassigned referrals matching the accession
            // number — pass String IDs directly (SampleItem.id is String)
            List<Object[]> results = referralDAO.searchUnassignedByAccessionNumber(searchTerm,
                    excludedSampleItemIds != null ? excludedSampleItemIds : new ArrayList<>());

            return buildSampleItemDTOs(results);
        } catch (Exception e) {
            logger.error("Error searching unassigned sample items by accession number: {}", searchTerm, e);
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SampleItemDTO> getAllUnassigned() {
        try {
            List<String> excludedSampleItemIds = boxSampleItemDAO.getAllAssignedSampleItemIds();
            List<Object[]> results = referralDAO.getUnassignedReferralsGroupedBySampleItem(excludedSampleItemIds);
            return buildSampleItemDTOs(results);
        } catch (Exception e) {
            logger.error("Error getting all unassigned sample items", e);
            LogEvent.logError(e);
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAlreadyAssigned(String sampleItemId) {
        try {
            return boxSampleItemDAO.existsBySampleItemId(sampleItemId);
        } catch (Exception e) {
            logger.error("Error checking if sample item is already assigned: {}", sampleItemId, e);
            LogEvent.logError(e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SampleItemDTO getSampleItemById(String sampleItemId) {
        try {
            // Get all referrals for this sample item
            Integer sampleItemIdInt = Integer.parseInt(sampleItemId);

            List<Referral> referrals = referralDAO.getReferralsBySampleItemId(sampleItemIdInt);

            if (referrals.isEmpty()) {
                logger.warn("No referrals found for sample item: {}", sampleItemId);
                return null;
            }

            // Build DTO from the first referral (to get sample item info)
            Referral firstReferral = referrals.get(0);
            var analysis = firstReferral.getAnalysis();
            if (analysis == null || analysis.getSampleItem() == null) {
                logger.warn("Analysis or SampleItem is null for referral: {}", firstReferral.getId());
                return null;
            }

            var sampleItem = analysis.getSampleItem();
            var sample = sampleItem.getSample();
            var typeOfSample = sampleItem.getTypeOfSample();

            // Suffix accession number with sort order (e.g., DEV012-1, DEV012-2)
            String accessionNumber = sample != null ? sample.getAccessionNumber() : "";
            String sortOrder = sampleItem.getSortOrder();
            if (sortOrder != null && !sortOrder.isEmpty()) {
                accessionNumber = accessionNumber + "-" + sortOrder;
            }

            SampleItemDTO dto = new SampleItemDTO();
            dto.setSampleItemId(sampleItem.getId());
            dto.setAccessionNumber(accessionNumber);
            dto.setTypeOfSample(typeOfSample != null ? typeOfSample.getDescription() : "");
            dto.setTypeOfSampleId(typeOfSample != null ? typeOfSample.getId() : "");
            dto.setCollectionDate(sample != null ? sample.getCollectionDate() : null);

            // Check if already assigned
            if (isAlreadyAssigned(sampleItemId)) {
                var boxSampleItem = boxSampleItemDAO.findBySampleItemId(sampleItemId);
                if (boxSampleItem != null && boxSampleItem.getShippingBox() != null) {
                    dto.setAssignedBoxId(boxSampleItem.getShippingBox().getId());
                    dto.setAssignedBoxName(boxSampleItem.getShippingBox().getBoxId());
                }
            }

            // Add all referral tests
            List<ReferralTestDTO> referralTests = new ArrayList<>();
            for (Referral referral : referrals) {
                ReferralTestDTO testDTO = buildReferralTestDTO(referral);
                if (testDTO != null) {
                    referralTests.add(testDTO);
                }
            }
            dto.setReferralTests(referralTests);

            return dto;
        } catch (Exception e) {
            logger.error("Error getting sample item by ID: {}", sampleItemId, e);
            LogEvent.logError(e);
            return null;
        }
    }

    /**
     * Build SampleItemDTOs from query results and populate with referral tests
     *
     * @param results - Query results [sampleItemId, accessionNumber,
     *                typeOfSampleName, typeOfSampleId, collectionDate]
     * @return List of SampleItemDTO with referral tests populated
     */
    private List<SampleItemDTO> buildSampleItemDTOs(List<Object[]> results) {
        List<SampleItemDTO> dtos = new ArrayList<>();

        // First pass: count how many sample items share each accession number
        java.util.Map<String, Integer> accessionCounts = new java.util.HashMap<>();
        for (Object[] row : results) {
            String accession = (String) row[1];
            accessionCounts.merge(accession, 1, Integer::sum);
        }

        for (Object[] row : results) {
            String sampleItemId = (String) row[0];
            String accessionNumber = (String) row[1];
            String typeOfSampleName = (String) row[2];
            String typeOfSampleId = (String) row[3];
            Timestamp collectionDate = (Timestamp) row[4];
            String sortOrder = row[5] != null ? row[5].toString() : "1";

            // Suffix accession number with sort order if multiple sample items share it
            String displayAccession = accessionNumber;
            if (accessionCounts.getOrDefault(accessionNumber, 1) > 1) {
                displayAccession = accessionNumber + "-" + sortOrder;
            }

            SampleItemDTO dto = new SampleItemDTO(sampleItemId, displayAccession, typeOfSampleName, typeOfSampleId,
                    collectionDate);

            // Get all referrals for this sample item
            Integer sampleItemIdInt = Integer.parseInt(sampleItemId);
            List<Referral> referrals = referralDAO.getReferralsBySampleItemId(sampleItemIdInt);

            List<ReferralTestDTO> referralTests = new ArrayList<>();
            for (Referral referral : referrals) {
                ReferralTestDTO testDTO = buildReferralTestDTO(referral);
                if (testDTO != null) {
                    referralTests.add(testDTO);
                }
            }

            dto.setReferralTests(referralTests);
            dtos.add(dto);
        }

        return dtos;
    }

    /**
     * Build a ReferralTestDTO from a Referral entity
     *
     * @param referral - The referral entity
     * @return ReferralTestDTO or null if data is invalid
     */
    private ReferralTestDTO buildReferralTestDTO(Referral referral) {
        try {
            if (referral == null) {
                return null;
            }

            String testName = "";
            if (referral.getAnalysis() != null && referral.getAnalysis().getTest() != null) {
                testName = referral.getAnalysis().getTest().getName();
            }

            String organizationName = "";
            if (referral.getOrganization() != null) {
                organizationName = referral.getOrganization().getOrganizationName();
            }

            return new ReferralTestDTO(referral.getId(), testName, organizationName, referral.getRequestDate(),
                    referral.getStatus() != null ? referral.getStatus().toString() : "");
        } catch (Exception e) {
            logger.error("Error building ReferralTestDTO for referral: {}", referral != null ? referral.getId() : null,
                    e);
            return null;
        }
    }
}
