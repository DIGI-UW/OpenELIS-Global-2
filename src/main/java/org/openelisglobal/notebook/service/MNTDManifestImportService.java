package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import org.openelisglobal.notebook.form.MNTDManifestImportForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for importing MNTD (Malaria and Neglected Tropical Disease)
 * Laboratory samples from manifest CSV files. Handles MNTD-specific data points
 * including Sample ID, Sample Source, Project Name, Collection Site, Collection
 * DateTime, and Collected By.
 */
public interface MNTDManifestImportService {

    /**
     * Result of manifest parsing operation.
     */
    record ParsedManifest(List<MNTDManifestRow> rows, List<ParseError> errors) {
    }

    /**
     * A single row from the MNTD manifest CSV with all MNTD-specific fields.
     */
    record MNTDManifestRow(int rowNumber, String sampleId, String sampleSource, String projectName, String sampleType,
            String collectionSite, String collectionDateTime, String collectedBy, int numOfSamples) {
    }

    /**
     * Error during manifest parsing or validation.
     */
    record ParseError(int rowNumber, String column, String message) {
    }

    /**
     * Result of sample creation from manifest.
     */
    record MNTDManifestImportResult(int totalRequested, int totalCreated, List<SampleItem> createdSamples,
            List<String> createdAccessionNumbers, List<ParseError> errors) {
    }

    /**
     * Parse an MNTD manifest CSV file.
     *
     * @param csvInput      the CSV input stream
     * @param columnMapping the MNTD column mapping configuration
     * @return parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, MNTDManifestImportForm columnMapping);

    /**
     * Validate sample types in a parsed manifest against TypeOfSample records.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSampleTypes(ParsedManifest manifest);

    /**
     * Create SampleItem records from an MNTD manifest for a notebook entry. Each
     * row with numOfSamples > 1 creates multiple SampleItem records. MNTD-specific
     * data is stored in sample metadata.
     *
     * @param entryId             the notebook entry to link samples to
     * @param manifest            the parsed and validated manifest
     * @param sysUserId           the user creating the samples
     * @param manifestDescription optional description/notes about this manifest
     *                            import
     * @return result containing created samples, accession numbers, and any errors
     */
    MNTDManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId,
            String manifestDescription);

    /**
     * Generate external ID for an MNTD sample based on sample ID and sequence
     * number. Format: {sampleId}-{sequenceNumber padded to 3 digits}
     *
     * @param sampleId       the sample identifier from manifest
     * @param sequenceNumber the sequence number within the group (1-based)
     * @return the formatted external ID
     */
    String generateExternalId(String sampleId, int sequenceNumber);

    /**
     * Get valid sample types for the MNTD laboratory. Returns sample types that are
     * both in the valid MNTD list AND exist in the database.
     *
     * @return List of maps containing sample type info (id, description)
     */
    java.util.List<java.util.Map<String, String>> getValidMntdSampleTypes();
}
