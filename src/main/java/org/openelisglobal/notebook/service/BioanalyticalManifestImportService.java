package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.BioanalyticalManifestImportForm;

/**
 * Service interface for Bioanalytical & Bioequivalence Laboratory manifest CSV
 * import.
 *
 * Supports sample reception metadata including: - Required: uniqueSampleId,
 * sampleType, sourceOrigin, requestedTests, dateTimeOfReceipt,
 * receivingPersonnel - Optional: projectStudyAssociation,
 * storageConditionPrior, sampleVolume, transportTemperature,
 * manifestVerificationStatus, notes
 *
 * Sample Types include biological matrices (plasma, serum, urine) and
 * pharmaceutical products (API, tablets, capsules, suspensions, excipients,
 * reference standards).
 */
public interface BioanalyticalManifestImportService {

    /**
     * Represents a single row from the bioanalytical reception manifest CSV. Each
     * row creates one sample with its associated reception metadata.
     */
    record BioanalyticalManifestRow(int rowNumber,
            // Required fields
            String uniqueSampleId, String sampleType, String sourceOrigin, String requestedTests,
            String dateTimeOfReceipt, String receivingPersonnel,
            // Optional fields
            String projectStudyAssociation, String storageConditionPrior, String sampleVolume,
            String transportTemperature, String manifestVerificationStatus, String subjectId,
            String timepoint, String notes) {
    }

    record ParseError(int rowNumber, String column, String message) {
    }

    record ParsedManifest(List<BioanalyticalManifestRow> rows, List<ParseError> errors) {
    }

    record BioanalyticalManifestImportResult(int totalRequested, int totalCreated, List<ParseError> errors,
            List<String> createdSampleIds) {
    }

    /**
     * Parse the manifest CSV input stream using the provided column mapping.
     *
     * @param csvInput      The CSV file input stream
     * @param columnMapping The column mapping configuration
     * @return Parsed manifest with rows and any parse errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, BioanalyticalManifestImportForm columnMapping);

    /**
     * Validate the parsed manifest data. Checks sample types, requested tests, date
     * formats, and other business rules specific to bioanalytical lab.
     *
     * @param manifest The parsed manifest to validate
     * @return List of validation errors (empty if valid)
     */
    List<ParseError> validateManifest(ParsedManifest manifest);

    /**
     * Create samples from the parsed and validated manifest.
     *
     * @param entryId   The notebook entry ID to link samples to
     * @param manifest  The parsed manifest data
     * @param sysUserId The current user ID
     * @return Import result with counts and any errors
     */
    BioanalyticalManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Get valid sample types for the Bioanalytical laboratory. Returns sample types
     * that are both in the valid bioanalytical list AND exist in the database.
     *
     * @return List of maps containing sample type info (id, description)
     */
    List<Map<String, String>> getValidBioanalyticalSampleTypes();

    /**
     * Get valid analytical tests for the Bioanalytical laboratory.
     *
     * @return List of maps containing test info (id, description)
     */
    List<Map<String, String>> getValidBioanalyticalTests();

    /**
     * Get valid source origins for bioanalytical samples (Medical Lab, External
     * Client, Internal Researcher, etc.).
     *
     * @return List of maps containing source origin info
     */
    List<Map<String, String>> getValidSourceOrigins();
}
