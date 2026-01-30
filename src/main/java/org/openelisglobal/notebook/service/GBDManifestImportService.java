package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.GBDManifestImportForm;

/**
 * Service interface for Genomic Bioanalytical Database (GBD) manifest CSV
 * import.
 *
 * Supports genomic sample metadata with: - Required: sampleId, sampleType,
 * source, collectionDate, receptionDateTime - Optional reception metadata:
 * projectStudyAssociation, volumeConcentration, A260/280, A260/230, RIN -
 * Optional processing metadata: extractionMethodKit, pcrProtocol,
 * libraryPrepProtocol, sequencingPlatform, runId, operator, processingDateTime,
 * notes
 *
 * Sample Types include DNA, RNA, cDNA, and various genomic materials. All
 * sample metadata is stored in JSONB format on the sample.data field for
 * flexibility.
 */
public interface GBDManifestImportService {

    /**
     * Represents a single row from the GBD manifest CSV. Each row creates one
     * sample with its associated reception and processing metadata.
     */
    record GBDManifestRow(int rowNumber,
            // Required fields
            String sampleId, String sampleType, String source, String collectionDate, String receptionDateTime,
            // Optional reception metadata
            String projectStudyAssociation, String volumeConcentration, String a260_280, String a260_230, String rin,
            // Optional processing metadata
            String extractionMethodKit, String pcrProtocol, String libraryPrepProtocol, String sequencingPlatform,
            String runId, String operator, String processingDateTime, String notes) {
    }

    record ParseError(int rowNumber, String column, String message) {
    }

    record ParsedManifest(List<GBDManifestRow> rows, List<ParseError> errors) {
    }

    record GBDManifestImportResult(int totalRequested, int totalCreated, List<ParseError> errors,
            List<String> createdSampleIds) {
    }

    /**
     * Parse the manifest CSV input stream using the provided column mapping.
     *
     * @param csvInput      The CSV file input stream
     * @param columnMapping The column mapping configuration
     * @return Parsed manifest with rows and any parse errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, GBDManifestImportForm columnMapping);

    /**
     * Validate the parsed manifest data. Checks sample types, date formats, numeric
     * values, and other business rules specific to genomic laboratory.
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
    GBDManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Get valid sample types for the GBD laboratory. Returns sample types that are
     * both in the valid GBD list AND exist in the database.
     *
     * @return List of maps containing sample type info (id, description)
     */
    List<Map<String, String>> getValidGBDSampleTypes();
}
