package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import org.openelisglobal.notebook.form.PathologyManifestImportForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for importing Pathology Laboratory samples from manifest CSV files.
 * Handles pathology-specific data points including clinical metadata (patient
 * ID, requesting clinician, clinical details) and research metadata (study ID,
 * PI name, participant/animal ID, ethical approval reference).
 */
public interface PathologyManifestImportService {

    /**
     * Result of manifest parsing operation.
     */
    record ParsedManifest(List<PathologyManifestRow> rows, List<ParseError> errors) {
    }

    /**
     * A single row from the Pathology manifest CSV with all pathology-specific
     * fields.
     */
    record PathologyManifestRow(int rowNumber, String groupId, String sampleType, int numOfSamples,
            String collectionDate, String sampleCategory, String sourceFacility, String specimenSite,
            // Clinical metadata
            String patientId, String requestingClinician, String clinicalDetails,
            // Research metadata
            String studyId, String piName, String participantAnimalId, String ethicalApprovalRef,
            // Notes
            String notes) {

        /**
         * Check if this is a clinical sample based on sample category.
         */
        public boolean isClinical() {
            return sampleCategory != null && sampleCategory.toLowerCase().contains("clinical");
        }

        /**
         * Check if this is a research sample based on sample category.
         */
        public boolean isResearch() {
            return sampleCategory != null && sampleCategory.toLowerCase().contains("research");
        }
    }

    /**
     * Error during manifest parsing or validation.
     */
    record ParseError(int rowNumber, String column, String message) {
    }

    /**
     * Result of sample creation from manifest.
     */
    record PathologyManifestImportResult(int totalRequested, int totalCreated, List<SampleItem> createdSamples,
            List<String> createdAccessionNumbers, List<ParseError> errors) {
    }

    /**
     * Parse a Pathology manifest CSV file.
     *
     * @param csvInput      the CSV input stream
     * @param columnMapping the Pathology column mapping configuration
     * @return parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, PathologyManifestImportForm columnMapping);

    /**
     * Validate sample types in a parsed manifest against TypeOfSample records.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSampleTypes(ParsedManifest manifest);

    /**
     * Validate clinical and research samples have appropriate metadata fields.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation warnings for missing metadata
     */
    List<ParseError> validateCategoryMetadata(ParsedManifest manifest);

    /**
     * Create SampleItem records from a Pathology manifest for a notebook entry.
     * Each row with numOfSamples > 1 creates multiple SampleItem records.
     * Pathology-specific data is stored in sample metadata based on sample
     * category.
     *
     * @param entryId   the notebook entry to link samples to
     * @param manifest  the parsed and validated manifest
     * @param sysUserId the user creating the samples
     * @return result containing created samples, accession numbers, and any errors
     */
    PathologyManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Generate external ID for a Pathology sample based on group ID and sequence
     * number. Format: {groupId}-{sequenceNumber padded to 3 digits}
     *
     * @param groupId        the group identifier from manifest
     * @param sequenceNumber the sequence number within the group (1-based)
     * @return the formatted external ID
     */
    String generateExternalId(String groupId, int sequenceNumber);
}
