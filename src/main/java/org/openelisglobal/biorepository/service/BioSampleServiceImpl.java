package org.openelisglobal.biorepository.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.biorepository.dao.BioSampleDAO;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.storage.service.SampleStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for BioSample entity operations.
 *
 * BioSample is now an extension record for SampleItem, containing only
 * biorepository-specific metadata. Core sample data (barcode, type, quantity,
 * dates) is stored in Sample and SampleItem entities.
 *
 * Use SampleService/SampleItemService for core sample operations. Use this
 * service for biorepository-specific extensions.
 */
@Service
public class BioSampleServiceImpl extends AuditableBaseObjectServiceImpl<BioSample, Integer>
        implements BioSampleService {

    private static final Logger logger = LoggerFactory.getLogger(BioSampleServiceImpl.class);

    @Autowired
    protected BioSampleDAO baseObjectDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleStorageService sampleStorageService;

    BioSampleServiceImpl() {
        super(BioSample.class);
    }

    @Override
    protected BioSampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public BioSample getBySampleItemId(Integer sampleItemId) {
        return baseObjectDAO.getBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getByShipmentId(Integer shipmentId) {
        return baseObjectDAO.getByShipmentId(shipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getByBiosafetyLevel(BiosafetyLevel biosafetyLevel) {
        return baseObjectDAO.getByBiosafetyLevel(biosafetyLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getByEthicsApprovalRef(String ethicsApprovalRef) {
        return baseObjectDAO.getByEthicsApprovalRef(ethicsApprovalRef);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getByMtaReference(String mtaReference) {
        return baseObjectDAO.getByMtaReference(mtaReference);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getByPrincipalInvestigator(String principalInvestigator) {
        return baseObjectDAO.getByPrincipalInvestigator(principalInvestigator);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySampleItemId(Integer sampleItemId) {
        return baseObjectDAO.existsBySampleItemId(sampleItemId);
    }

    @Override
    @Transactional
    public BioSample createForSampleItem(SampleItem sampleItem, BioSample bioSample) {
        if (sampleItem == null) {
            throw new IllegalArgumentException("SampleItem cannot be null");
        }
        if (sampleItem.getId() == null) {
            throw new IllegalArgumentException("SampleItem must be persisted first");
        }
        if (existsBySampleItemId(Integer.valueOf(sampleItem.getId()))) {
            throw new IllegalStateException("BioSample extension already exists for SampleItem: " + sampleItem.getId());
        }

        bioSample.setSampleItem(sampleItem);
        return save(bioSample);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByShipmentId(Integer shipmentId) {
        return baseObjectDAO.countByShipmentId(shipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByBiosafetyLevel(BiosafetyLevel biosafetyLevel) {
        return baseObjectDAO.countByBiosafetyLevel(biosafetyLevel);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean barcodeExists(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return false;
        }
        List<SampleItem> sampleItems = sampleItemService.getSampleItemsByExternalID(barcode.trim());
        return sampleItems != null && !sampleItems.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getAllWithRelationships(int limit) {
        return baseObjectDAO.getAllWithRelationships(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getByShipmentIdWithRelationships(Integer shipmentId) {
        return baseObjectDAO.getByShipmentIdWithRelationships(shipmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getBySampleItemIds(List<Integer> sampleItemIds) {
        return baseObjectDAO.getBySampleItemIds(sampleItemIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getExpiringSamples(int daysWindow) {
        java.time.LocalDate cutoffDate = java.time.LocalDate.now().plusDays(daysWindow);
        java.sql.Date sqlCutoffDate = java.sql.Date.valueOf(cutoffDate);
        return baseObjectDAO.getExpiringSamplesBefore(sqlCutoffDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getExpiredSamples() {
        java.sql.Date today = java.sql.Date.valueOf(java.time.LocalDate.now());
        return baseObjectDAO.getExpiredSamples(today);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getSamplesForDisposalDashboard() {
        return baseObjectDAO.getSamplesWithRetentionData();
    }

    @Override
    @Transactional
    public Map<String, Object> disposeBioSample(String sampleItemId, String reason, String method, String notes) {
        // First, dispose via SampleStorageService (updates SampleItem status and
        // creates audit trail)
        Map<String, Object> result = sampleStorageService.disposeSampleItem(sampleItemId, reason, method, notes);

        // Then, update BioSample workflowStatus to DISPOSED if it exists
        try {
            Integer sampleItemIdInt = Integer.valueOf(sampleItemId);
            BioSample bioSample = getBySampleItemId(sampleItemIdInt);
            if (bioSample != null) {
                bioSample.setWorkflowStatus(BioSample.WorkflowStatus.DISPOSED);
                update(bioSample);
                logger.debug("Updated BioSample {} workflowStatus to DISPOSED", bioSample.getId());
            }
        } catch (NumberFormatException e) {
            // sampleItemId might be an accession number or external ID, try to get from
            // result
            String resolvedId = (String) result.get("sampleItemId");
            if (resolvedId != null && !resolvedId.equals(sampleItemId)) {
                try {
                    Integer resolvedIdInt = Integer.valueOf(resolvedId);
                    BioSample bioSample = getBySampleItemId(resolvedIdInt);
                    if (bioSample != null) {
                        bioSample.setWorkflowStatus(BioSample.WorkflowStatus.DISPOSED);
                        update(bioSample);
                        logger.debug("Updated BioSample {} workflowStatus to DISPOSED (resolved from {})",
                                bioSample.getId(), sampleItemId);
                    }
                } catch (NumberFormatException ex) {
                    logger.warn("Could not parse resolved SampleItem ID for BioSample lookup: {}", resolvedId);
                }
            } else {
                logger.warn("Could not parse SampleItem ID for BioSample lookup: {}", sampleItemId);
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getByOriginLab(String originLab) {
        return baseObjectDAO.getByOriginLab(originLab);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BioSample> getByProjectId(String projectId) {
        return baseObjectDAO.getByProjectId(projectId);
    }
}
