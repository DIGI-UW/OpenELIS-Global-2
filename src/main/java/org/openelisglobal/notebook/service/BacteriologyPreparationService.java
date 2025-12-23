package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Bacteriology Laboratory preparation operations. Handles
 * culture media preparation, biochemical media, and antibiotic IQC tracking.
 *
 * Preparation types supported: - Culture Media: Blood Agar, MacConkey,
 * Chocolate Agar, Mueller-Hinton, etc. - Biochemical Media: TSI, Citrate, Urea,
 * Indole, etc. - Antibiotic IQC: Quality control for antibiotic susceptibility
 * testing
 */
public interface BacteriologyPreparationService {

    // ==========================================
    // CULTURE MEDIA PREPARATION
    // ==========================================

    /**
     * Get all culture media preparations for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of culture media preparations with details
     */
    List<Map<String, Object>> getCultureMediaPreparations(Integer entryId);

    /**
     * Save a new culture media preparation.
     *
     * @param entryId     the notebook entry ID
     * @param preparation preparation data including: - mediaType: Type of culture
     *                    media (e.g., BLOOD_AGAR, MACCONKEY) - lotNumber:
     *                    Manufacturer lot number - expiryDate: Expiration date -
     *                    preparationDate: Date prepared - preparedBy: Staff who
     *                    prepared - sterilizationMethod: How sterilized (AUTOCLAVE,
     *                    FILTER, etc.) - qcStatus: Quality control status (PASSED,
     *                    FAILED, PENDING) - qcOrganism: QC organism used for
     *                    validation - storageCondition: Storage
     *                    temperature/conditions - notes: Additional notes
     * @param userId      the user creating the preparation
     * @return the saved preparation with generated ID
     */
    Map<String, Object> saveCultureMediaPreparation(Integer entryId, Map<String, Object> preparation, String userId);

    /**
     * Update an existing culture media preparation.
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param preparation   updated preparation data
     * @param userId        the user updating the preparation
     * @return the updated preparation, or null if not found
     */
    Map<String, Object> updateCultureMediaPreparation(Integer entryId, Integer preparationId,
            Map<String, Object> preparation, String userId);

    /**
     * Delete a culture media preparation.
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param userId        the user deleting the preparation
     * @return true if deleted, false if not found
     */
    boolean deleteCultureMediaPreparation(Integer entryId, Integer preparationId, String userId);

    // ==========================================
    // BIOCHEMICAL MEDIA PREPARATION
    // ==========================================

    /**
     * Get all biochemical media preparations for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of biochemical media preparations with details
     */
    List<Map<String, Object>> getBiochemicalMediaPreparations(Integer entryId);

    /**
     * Save a new biochemical media preparation.
     *
     * @param entryId     the notebook entry ID
     * @param preparation preparation data including: - testType: Type of
     *                    biochemical test (TSI, CITRATE, UREA, etc.) - lotNumber:
     *                    Manufacturer lot number - expiryDate: Expiration date -
     *                    preparationDate: Date prepared - preparedBy: Staff who
     *                    prepared - qcStatus: Quality control status -
     *                    positiveControl: Positive control organism -
     *                    negativeControl: Negative control organism -
     *                    expectedResult: Expected result for QC - actualResult:
     *                    Actual observed result - notes: Additional notes
     * @param userId      the user creating the preparation
     * @return the saved preparation with generated ID
     */
    Map<String, Object> saveBiochemicalMediaPreparation(Integer entryId, Map<String, Object> preparation,
            String userId);

    /**
     * Update an existing biochemical media preparation.
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param preparation   updated preparation data
     * @param userId        the user updating the preparation
     * @return the updated preparation, or null if not found
     */
    Map<String, Object> updateBiochemicalMediaPreparation(Integer entryId, Integer preparationId,
            Map<String, Object> preparation, String userId);

    /**
     * Delete a biochemical media preparation.
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param userId        the user deleting the preparation
     * @return true if deleted, false if not found
     */
    boolean deleteBiochemicalMediaPreparation(Integer entryId, Integer preparationId, String userId);

    // ==========================================
    // ANTIBIOTIC IQC PREPARATION
    // ==========================================

    /**
     * Get all antibiotic IQC preparations for a notebook entry.
     *
     * @param entryId the notebook entry ID
     * @return list of antibiotic IQC preparations with details
     */
    List<Map<String, Object>> getAntibioticIqcPreparations(Integer entryId);

    /**
     * Save a new antibiotic IQC preparation.
     *
     * @param entryId     the notebook entry ID
     * @param preparation preparation data including: - antibioticName: Name of
     *                    antibiotic - antibioticClass: Class (PENICILLIN,
     *                    CEPHALOSPORIN, etc.) - concentration: Concentration in
     *                    µg/mL - lotNumber: Manufacturer lot number - expiryDate:
     *                    Expiration date - qcOrganism: QC strain used (e.g., E.
     *                    coli ATCC 25922) - expectedZone: Expected zone diameter
     *                    range (mm) - observedZone: Observed zone diameter (mm) -
     *                    qcStatus: QC status (PASSED, FAILED, PENDING) - testDate:
     *                    Date of test - testedBy: Staff who performed test - notes:
     *                    Additional notes
     * @param userId      the user creating the preparation
     * @return the saved preparation with generated ID
     */
    Map<String, Object> saveAntibioticIqcPreparation(Integer entryId, Map<String, Object> preparation, String userId);

    /**
     * Update an existing antibiotic IQC preparation.
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param preparation   updated preparation data
     * @param userId        the user updating the preparation
     * @return the updated preparation, or null if not found
     */
    Map<String, Object> updateAntibioticIqcPreparation(Integer entryId, Integer preparationId,
            Map<String, Object> preparation, String userId);

    /**
     * Delete an antibiotic IQC preparation.
     *
     * @param entryId       the notebook entry ID
     * @param preparationId the preparation ID
     * @param userId        the user deleting the preparation
     * @return true if deleted, false if not found
     */
    boolean deleteAntibioticIqcPreparation(Integer entryId, Integer preparationId, String userId);

    // ==========================================
    // REFERENCE DATA
    // ==========================================

    /**
     * Get available culture media types.
     *
     * @return list of culture media types with id and text
     */
    List<Map<String, String>> getCultureMediaTypes();

    /**
     * Get available biochemical test types.
     *
     * @return list of biochemical test types with id and text
     */
    List<Map<String, String>> getBiochemicalTestTypes();

    /**
     * Get available antibiotic types for IQC.
     *
     * @return list of antibiotic types with id and text
     */
    List<Map<String, String>> getAntibioticTypes();
}
