package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import org.openelisglobal.notebook.form.TBManifestImportForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for importing TB (Tuberculosis) Laboratory samples from manifest CSV
 * files. Handles TB-specific data points per spec FR-014 including specimen
 * information, patient metadata, clinical context, requested tests, and receipt
 * details.
 */
public interface TBManifestImportService {

    /**
     * Result of manifest parsing operation.
     */
    record ParsedManifest(List<TBManifestRow> rows, List<ParseError> errors) {
    }

    /**
     * A single row from the TB manifest CSV with spec-compliant TB fields per
     * FR-014.
     */
    record TBManifestRow(int rowNumber,
            // Sample Identity
            String sampleId,
            // Specimen Information
            String specimenType, String specimenQuality,
            // Request Paper Details
            String documentNumber, String referringFacility,
            // Patient Metadata
            String patientName, String patientAge, String patientSex, String patientId, String studyId,
            String patientAddress, String patientPhone, String physicianPhone, String consentStatus,
            // Clinical Context
            String treatmentHistory,
            // Requested Tests
            String culture, String smearMicroscopy, String genexpert, String identification, String dstFirstLine,
            String dstSecondLine, String intendedMethod,
            // Receipt Details
            String receivedSite, String receivedDate, String receivedTime,
            // Common
            int numOfSamples) {
    }

    /**
     * Error during manifest parsing or validation.
     */
    record ParseError(int rowNumber, String column, String message) {
    }

    /**
     * Result of sample creation from manifest.
     */
    record TBManifestImportResult(int totalRequested, int totalCreated, List<SampleItem> createdSamples,
            List<String> createdAccessionNumbers, List<ParseError> errors) {
    }

    /**
     * Parse a TB manifest CSV file.
     *
     * @param csvInput      the CSV input stream
     * @param columnMapping the TB column mapping configuration
     * @return parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, TBManifestImportForm columnMapping);

    /**
     * Validate specimen types in a parsed manifest against TypeOfSample records.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSpecimenTypes(ParsedManifest manifest);

    /**
     * Validate specimen types in a parsed manifest against sample types allowed for
     * the organizations linked to the notebook entry. Falls back to global
     * validation if no organizations are linked.
     *
     * @param entryId  the notebook entry ID to get linked organizations
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSpecimenTypesForEntry(Integer entryId, ParsedManifest manifest);

    /**
     * Create SampleItem records from a TB manifest for a notebook entry. Each row
     * with numOfSamples > 1 creates multiple SampleItem records. TB-specific data
     * is stored in sample metadata.
     *
     * @param entryId   the notebook entry to link samples to
     * @param manifest  the parsed and validated manifest
     * @param sysUserId the user creating the samples
     * @return result containing created samples, accession numbers, and any errors
     */
    TBManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Generate external ID for a TB sample based on sample ID and sequence number.
     * Format: TB-{sampleId}-{sequenceNumber padded to 3 digits}
     *
     * @param sampleId       the sample identifier from manifest
     * @param sequenceNumber the sequence number within the group (1-based)
     * @return the formatted external ID
     */
    String generateExternalId(String sampleId, int sequenceNumber);
}
