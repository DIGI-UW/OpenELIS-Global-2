package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import org.openelisglobal.notebook.form.ManifestImportForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for importing samples from manifest CSV files. Handles CSV parsing,
 * sample type validation, and bulk SampleItem creation.
 */
public interface ManifestImportService {

    /**
     * Result of manifest parsing operation.
     */
    record ParsedManifest(List<ManifestRow> rows, List<ParseError> errors) {
    }

    /**
     * A single row from the manifest CSV.
     */
    record ManifestRow(int rowNumber, String groupId, String sampleType, String collectionDate, String volume,
            int numOfSamples, String notes) {
    }

    /**
     * Error during manifest parsing or validation.
     */
    record ParseError(int rowNumber, String column, String message) {
    }

    /**
     * Result of sample creation from manifest.
     */
    record ManifestImportResult(int totalRequested, int totalCreated, List<SampleItem> createdSamples,
            List<ParseError> errors) {
    }

    /**
     * Parse a manifest CSV file.
     *
     * @param csvInput      the CSV input stream
     * @param columnMapping the column mapping configuration
     * @return parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, ManifestImportForm columnMapping);

    /**
     * Validate sample types in a parsed manifest against TypeOfSample records.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSampleTypes(ParsedManifest manifest);

    /**
     * Create SampleItem records from a manifest for a notebook entry. Each row with
     * num_of_samples > 1 creates multiple SampleItem records with sequential
     * external_id values (e.g., GRP-001-001, GRP-001-002, etc.).
     *
     * @param entryId   the notebook entry to link samples to
     * @param manifest  the parsed and validated manifest
     * @param sysUserId the user creating the samples
     * @return result containing created samples and any errors
     */
    ManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Generate external ID for a sample based on group ID and sequence number.
     * Format: {groupId}-{sequenceNumber padded to 3 digits} Example: GRP-001-001,
     * GRP-001-002, etc.
     *
     * @param groupId        the group identifier from manifest
     * @param sequenceNumber the sequence number within the group (1-based)
     * @return the formatted external ID
     */
    String generateExternalId(String groupId, int sequenceNumber);
}
