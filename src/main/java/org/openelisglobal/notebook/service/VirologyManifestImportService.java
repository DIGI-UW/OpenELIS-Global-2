package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.VirologyManifestImportForm;

/**
 * Service interface for Virology & Vaccine Unit manifest CSV import. Supports
 * reception metadata with: - Required: sampleId, source, sampleType,
 * receptionDateTime, testType, projectStudyAssociation - Virus/Vaccine
 * Production: batchId, productionStage, cellLineUsed, passageNumber,
 * titerValues, qualityControlResults, formulationDetails - Optional:
 * collectionDateTime, storageConditionOnArrival, transportTemperature,
 * receivingPersonnelName, manifestVerificationStatus, notes - Auto-generated:
 * accessionNumber, barcodeQrCode
 */
public interface VirologyManifestImportService {

    /**
     * Represents a single row from the virology reception manifest CSV. Each row
     * creates one sample with its associated reception metadata.
     */
    record VirologyManifestRow(int rowNumber,
            // Required fields - Sample Arrival
            String sampleId, String source, String sampleType, String receptionDateTime, String testType,
            String projectStudyAssociation,
            // Virus/Vaccine Production fields
            String batchId, String productionStage, String cellLineUsed, String passageNumber, String titerValues,
            String qualityControlResults, String formulationDetails,
            // Optional fields
            String collectionDateTime, String storageConditionOnArrival, String transportTemperature,
            String receivingPersonnelName, String manifestVerificationStatus, String notes) {
    }

    record ParseError(int rowNumber, String column, String message) {
    }

    record ParsedManifest(List<VirologyManifestRow> rows, List<ParseError> errors) {
    }

    record VirologyManifestImportResult(int totalRequested, int totalCreated, List<ParseError> errors,
            List<String> createdAccessionNumbers) {
    }

    /**
     * Parse the manifest CSV input stream using the provided column mapping.
     *
     * @param csvInput      The CSV file input stream
     * @param columnMapping The column mapping configuration
     * @return Parsed manifest with rows and any parse errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, VirologyManifestImportForm columnMapping);

    /**
     * Validate the parsed manifest data. Checks sample types, date formats, test
     * types, and other business rules.
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
    VirologyManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Get valid sample types for the Virology & Vaccine Unit laboratory. Returns
     * sample types that are both in the valid virology list AND exist in the
     * database.
     *
     * @return List of maps containing sample type info (id, description)
     */
    List<Map<String, String>> getValidVirologySampleTypes();
}
