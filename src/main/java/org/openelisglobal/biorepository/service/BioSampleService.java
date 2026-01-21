package org.openelisglobal.biorepository.service;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service interface for BioSample entity operations.
 *
 * BioSample is now an extension record for SampleItem, containing only
 * biorepository-specific metadata. Core sample data (barcode, type, quantity,
 * dates) is stored in Sample and SampleItem entities.
 *
 * Use SampleService/SampleItemService for core sample operations. Use this
 * service for biorepository-specific extensions.
 */
public interface BioSampleService extends BaseObjectService<BioSample, Integer> {

    /**
     * Find biorepository extension by SampleItem.
     *
     * @param sampleItemId the sample item ID
     * @return bio sample extension or null if not found
     */
    BioSample getBySampleItemId(Integer sampleItemId);

    /**
     * Find all biorepository samples from a shipment.
     *
     * @param shipmentId the shipment ID
     * @return list of bio samples
     */
    List<BioSample> getByShipmentId(Integer shipmentId);

    /**
     * Find samples by biosafety level.
     *
     * @param biosafetyLevel the biosafety classification
     * @return list of samples with matching biosafety level
     */
    List<BioSample> getByBiosafetyLevel(BiosafetyLevel biosafetyLevel);

    /**
     * Find samples by ethics approval reference.
     *
     * @param ethicsApprovalRef the ethics approval reference
     * @return list of samples with matching ethics reference
     */
    List<BioSample> getByEthicsApprovalRef(String ethicsApprovalRef);

    /**
     * Find samples by MTA reference.
     *
     * @param mtaReference the MTA reference
     * @return list of samples with matching MTA
     */
    List<BioSample> getByMtaReference(String mtaReference);

    /**
     * Find samples by principal investigator.
     *
     * @param principalInvestigator the PI name
     * @return list of samples
     */
    List<BioSample> getByPrincipalInvestigator(String principalInvestigator);

    /**
     * Check if a BioSample extension exists for a SampleItem.
     *
     * @param sampleItemId the sample item ID
     * @return true if extension exists
     */
    boolean existsBySampleItemId(Integer sampleItemId);

    /**
     * Create a BioSample extension for a SampleItem.
     *
     * @param sampleItem the sample item to extend
     * @param bioSample  the biorepository extension data
     * @return the created extension record
     */
    BioSample createForSampleItem(SampleItem sampleItem, BioSample bioSample);

    /**
     * Count samples by shipment.
     *
     * @param shipmentId the shipment ID
     * @return count of samples in shipment
     */
    long countByShipmentId(Integer shipmentId);

    /**
     * Count samples by biosafety level.
     *
     * @param biosafetyLevel the biosafety level
     * @return count of matching samples
     */
    long countByBiosafetyLevel(BiosafetyLevel biosafetyLevel);

    /**
     * Check if a barcode already exists in the system.
     *
     * @param barcode the barcode to check
     * @return true if the barcode exists
     */
    boolean barcodeExists(String barcode);

    /**
     * Get all BioSamples with relationships eagerly loaded. Fetches shipment,
     * sampleItem, sampleItem.typeOfSample, and sampleItem.sample in a single query.
     * Use this method when you need to access related entities outside of a
     * transaction.
     *
     * @param limit maximum number of results
     * @return list of bio samples with relationships loaded
     */
    List<BioSample> getAllWithRelationships(int limit);

    /**
     * Get BioSamples by shipment ID with relationships eagerly loaded. Fetches
     * shipment, sampleItem, sampleItem.typeOfSample, and sampleItem.sample.
     *
     * @param shipmentId the shipment ID
     * @return list of bio samples with relationships loaded
     */
    List<BioSample> getByShipmentIdWithRelationships(Integer shipmentId);

    /**
     * Get BioSamples by list of sample item IDs with relationships eagerly loaded.
     * Fetches shipment, sampleItem, sampleItem.typeOfSample, and sampleItem.sample.
     *
     * @param sampleItemIds list of sample item IDs
     * @return list of bio samples with relationships loaded
     */
    List<BioSample> getBySampleItemIds(List<Integer> sampleItemIds);

    /**
     * Get samples expiring within a specified number of days. Only returns samples
     * with retention_expiry_date set and not already disposed.
     *
     * @param daysWindow number of days from today
     * @return list of bio samples expiring within the window
     */
    List<BioSample> getExpiringSamples(int daysWindow);

    /**
     * Get samples that have already expired (retention_expiry_date < today). Only
     * returns samples not already disposed.
     *
     * @return list of expired bio samples
     */
    List<BioSample> getExpiredSamples();

    /**
     * Get all samples with retention data for the disposal dashboard. Includes
     * expired, expiring soon, and samples without retention policy.
     *
     * @return list of bio samples with retention info
     */
    List<BioSample> getSamplesForDisposalDashboard();

    /**
     * Dispose a BioSample by updating both SampleItem status via
     * SampleStorageService and BioSample workflowStatus to DISPOSED.
     *
     * @param sampleItemId the sample item ID to dispose
     * @param reason       the disposal reason
     * @param method       the disposal method
     * @param notes        optional notes
     * @return disposal result map from SampleStorageService
     */
    java.util.Map<String, Object> disposeBioSample(String sampleItemId, String reason, String method, String notes);

    /**
     * Find samples by origin lab (exact match).
     *
     * @param originLab the origin lab name
     * @return list of bio samples from the specified origin lab
     */
    List<BioSample> getByOriginLab(String originLab);

    /**
     * Find samples by project ID (exact match).
     *
     * @param projectId the project ID/code
     * @return list of bio samples associated with the project
     */
    List<BioSample> getByProjectId(String projectId);
}
