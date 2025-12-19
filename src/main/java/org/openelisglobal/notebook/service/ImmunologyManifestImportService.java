package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.ImmunologyManifestImportForm;

/**
 * Service interface for Immunology laboratory manifest CSV import. Supports
 * reception metadata with: - Required: uniqueParentSampleId, projectNameId,
 * deliveryManifestReference, collectionDateTime, receptionDateTime,
 * sourceOrigin - Sample Type: sampleType (validated against Immunology lab
 * types) - Optional: sampleVolume, storageConditionOnArrival,
 * transportTemperature, receivingPersonnelName, manifestVerificationStatus,
 * patientId, notes - Auto-generated: accessionNumber, barcodeQrCode
 */
public interface ImmunologyManifestImportService {

    /**
     * Represents a single row from the immunology reception manifest CSV. Each row
     * creates one sample with its associated reception metadata.
     */
    record ImmunologyManifestRow(int rowNumber,
            // Required fields
            String uniqueParentSampleId, String projectNameId, String deliveryManifestReference,
            String collectionDateTime, String receptionDateTime, String sourceOrigin,
            // Sample type (validated against Immunology lab types)
            String sampleType,
            // Optional fields
            String sampleVolume, String storageConditionOnArrival, String transportTemperature,
            String receivingPersonnelName, String manifestVerificationStatus, String patientId, String notes) {
    }

    record ParseError(int rowNumber, String column, String message) {
    }

    record ParsedManifest(List<ImmunologyManifestRow> rows, List<ParseError> errors) {
    }

    record ImmunologyManifestImportResult(int totalRequested, int totalCreated, List<ParseError> errors,
            List<String> createdAccessionNumbers) {
    }

    /**
     * Parse the manifest CSV input stream using the provided column mapping.
     *
     * @param csvInput      The CSV file input stream
     * @param columnMapping The column mapping configuration
     * @return Parsed manifest with rows and any parse errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, ImmunologyManifestImportForm columnMapping);

    /**
     * Validate the parsed manifest data. Checks sample types, date formats, and
     * other business rules.
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
    ImmunologyManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Get valid sample types for the Immunology laboratory. Returns sample types
     * that are both in the valid immunology list AND exist in the database.
     *
     * @return List of maps containing sample type info (id, description)
     */
    List<Map<String, String>> getValidImmunologySampleTypes();
}
