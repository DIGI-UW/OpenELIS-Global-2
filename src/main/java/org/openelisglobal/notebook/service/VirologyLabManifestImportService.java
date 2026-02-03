package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.VirologyLabManifestImportForm;

/**
 * Service interface for VirologyLab (Virology Laboratory) manifest CSV import operations.
 *
 * Handles parsing, validation, and import of virology sample metadata from CSV manifests.
 * Supports the complete VirologyLab workflow including sample reception, processing,
 * and quality control tracking.
 */
public interface VirologyLabManifestImportService {

    /**
     * Get valid sample types for VirologyLab laboratory.
     * @return List of sample type maps with id and description
     */
    List<Map<String, String>> getValidVirologyLabSampleTypes();

    /**
     * Parse a CSV manifest file into structured data.
     * @param inputStream CSV file input stream
     * @param form Column mapping configuration
     * @return Parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream inputStream, VirologyLabManifestImportForm form);

    /**
     * Validate parsed manifest data.
     * @param parsedManifest The parsed manifest to validate
     * @return List of validation errors
     */
    List<ParseError> validateManifest(ParsedManifest parsedManifest);

    /**
     * Create samples from validated manifest data.
     * @param entryId Notebook entry ID to link samples to
     * @param parsedManifest Validated manifest data
     * @param sysUserId System user ID for audit trail
     * @return Import result with created sample IDs and any errors
     */
    VirologyLabManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest parsedManifest, String sysUserId);

    /**
     * Represents a single row from the parsed CSV manifest.
     */
    record ManifestRow(
        int rowNumber,
        String sampleId,
        String sampleType,
        String source,
        String collectionDate,
        String receptionDateTime,
        String projectStudyAssociation,
        String volumeConcentration,
        String a260_280,
        String a260_230,
        String rin,
        String extractionMethodKit,
        String pcrProtocol,
        String libraryPrepProtocol,
        String sequencingPlatform,
        String runId,
        String operator,
        String processingDateTime,
        String notes
    ) {}

    /**
     * Represents a parsing or validation error.
     */
    record ParseError(
        int rowNumber,
        String column,
        String message
    ) {}

    /**
     * Represents the complete parsed manifest with data and errors.
     */
    record ParsedManifest(
        List<ManifestRow> rows,
        List<ParseError> errors
    ) {}

    /**
     * Represents the result of a manifest import operation.
     */
    record VirologyLabManifestImportResult(
        int totalRequested,
        int totalCreated,
        List<String> createdSampleIds,
        List<ParseError> errors
    ) {}
}