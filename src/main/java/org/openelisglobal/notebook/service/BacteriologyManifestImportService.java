package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.BacteriologyManifestImportForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for importing Bacteriology Laboratory samples from manifest CSV
 * files. Handles Bacteriology-specific data points including: - Project Name,
 * Study ID - Participant ID, Barcode - Collection Site, Sample Type, Collection
 * Date & Time - Sample Received Date, Sample Arrival Time, Received By -
 * Storage Container Type, Storage Temperature on Arrival - Consent Status, CRF
 * Status - Sample Origin (Human/Animal/Environmental/Food), Source
 * Location/Facility
 */
public interface BacteriologyManifestImportService {

    /**
     * Result of manifest parsing operation.
     */
    record ParsedManifest(List<BacteriologyManifestRow> rows, List<ParseError> errors) {
    }

    /**
     * A single row from the Bacteriology manifest CSV with all
     * Bacteriology-specific fields.
     */
    record BacteriologyManifestRow(int rowNumber, String projectName, String studyId, String participantId,
            String barcode, String collectionSite, String sampleType, String collectionDateTime,
            String sampleReceivedDate, String sampleArrivalTime, String receivedBy, String storageContainerType,
            String storageTemperatureOnArrival, String consentStatus, String crfStatus, String sampleOrigin,
            String sourceLocationFacility) {
    }

    /**
     * Error during manifest parsing or validation.
     */
    record ParseError(int rowNumber, String column, String message) {
    }

    /**
     * Result of sample creation from manifest.
     */
    record BacteriologyManifestImportResult(int totalRequested, int totalCreated, List<SampleItem> createdSamples,
            List<String> createdAccessionNumbers, List<ParseError> errors) {
    }

    /**
     * Parse a Bacteriology manifest CSV file.
     *
     * @param csvInput      the CSV input stream
     * @param columnMapping the Bacteriology column mapping configuration
     * @return parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, BacteriologyManifestImportForm columnMapping);

    /**
     * Validate sample types in a parsed manifest against TypeOfSample records.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSampleTypes(ParsedManifest manifest);

    /**
     * Create SampleItem records from a Bacteriology manifest for a notebook entry.
     * Bacteriology-specific data is stored in sample metadata.
     *
     * @param entryId   the notebook entry to link samples to
     * @param manifest  the parsed and validated manifest
     * @param sysUserId the user creating the samples
     * @return result containing created samples, accession numbers, and any errors
     */
    BacteriologyManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Generate external ID for a Bacteriology sample based on barcode and sequence
     * number. Format: {barcode}-{sequenceNumber padded to 3 digits}
     *
     * @param barcode        the barcode from manifest
     * @param sequenceNumber the sequence number within the batch (1-based)
     * @return the formatted external ID
     */
    String generateExternalId(String barcode, int sequenceNumber);

    /**
     * Get valid sample types for the Bacteriology laboratory. Returns sample types
     * that are both in the valid Bacteriology list AND exist in the database.
     *
     * @return List of maps containing sample type info (id, description)
     */
    List<Map<String, String>> getValidBacteriologySampleTypes();
}
