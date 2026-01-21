package org.openelisglobal.biorepository.dao;

import java.util.List;
import org.openelisglobal.biorepository.valueholder.BioSample;
import org.openelisglobal.biorepository.valueholder.BioSample.BiosafetyLevel;
import org.openelisglobal.common.dao.BaseDAO;

/**
 * DAO interface for BioSample entity operations.
 *
 * BioSample is now an extension record for SampleItem, containing only
 * biorepository-specific metadata. Core sample data (barcode, type, quantity,
 * dates) is stored in Sample and SampleItem entities.
 */
public interface BioSampleDAO extends BaseDAO<BioSample, Integer> {

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
     * Get all BioSamples with relationships eagerly loaded. Fetches shipment,
     * sampleItem, sampleItem.typeOfSample, and sampleItem.sample in a single query.
     *
     * @param limit maximum number of results
     * @return list of bio samples with relationships loaded
     */
    List<BioSample> getAllWithRelationships(int limit);

    /**
     * Get BioSamples by shipment ID with relationships eagerly loaded.
     *
     * @param shipmentId the shipment ID
     * @return list of bio samples with relationships loaded
     */
    List<BioSample> getByShipmentIdWithRelationships(Integer shipmentId);

    /**
     * Get BioSamples by list of sample item IDs with relationships eagerly loaded.
     *
     * @param sampleItemIds list of sample item IDs
     * @return list of bio samples with relationships loaded
     */
    List<BioSample> getBySampleItemIds(List<Integer> sampleItemIds);

    /**
     * Get samples expiring within a specified number of days.
     *
     * @param expiryDate the cutoff date (today + daysWindow)
     * @return list of bio samples expiring before the cutoff
     */
    List<BioSample> getExpiringSamplesBefore(java.sql.Date expiryDate);

    /**
     * Get samples that have already expired.
     *
     * @param today today's date
     * @return list of expired bio samples
     */
    List<BioSample> getExpiredSamples(java.sql.Date today);

    /**
     * Get all samples with retention expiry date set (for disposal dashboard).
     *
     * @return list of bio samples with retention data
     */
    List<BioSample> getSamplesWithRetentionData();

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
