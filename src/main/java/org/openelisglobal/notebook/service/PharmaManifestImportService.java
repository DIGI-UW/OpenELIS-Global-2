package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.PharmaManifestImportForm;
import org.openelisglobal.notebook.service.PharmaManifestImportService.ParseError;

/**
 * Service interface for Pharmaceuticals manifest CSV import. Supports the
 * updated dataPoints schema with: - Required: sampleName, lotBatchNumber,
 * dateOfManufacture, expiryRetestDate, storageCondition, ownerRequester -
 * Optional: alphanumericCode, chemicalIupacName, gradeSpecification,
 * chainOfCustodyDetails, patientId, clinicalTrialNumber, consentStatus -
 * Auto-generated: uniqueSampleId, barcodeQrCode
 */
public interface PharmaManifestImportService {

    /**
     * Represents a single row from the pharmaceutical manifest CSV. Each row
     * creates one sample with its associated metadata.
     */
    record PharmaManifestRow(int rowNumber,
            // Required fields
            String sampleName, String lotBatchNumber, String dateOfManufacture, String expiryRetestDate,
            String storageCondition, String ownerRequester,
            // Sample type (validated against Pharmaceutical lab types)
            String sampleType,
            // Optional fields
            String alphanumericCode, String chemicalIupacName, String gradeSpecification, String chainOfCustodyDetails,
            String patientId, String clinicalTrialNumber, String consentStatus) {
    }

    record ParseError(int rowNumber, String column, String message) {
    }

    record ParsedManifest(List<PharmaManifestRow> rows, List<ParseError> errors) {
    }

    record PharmaManifestImportResult(int totalRequested, int totalCreated, List<ParseError> errors,
            List<String> createdAccessionNumbers) {
    }

    ParsedManifest parseManifestCsv(InputStream csvInput, PharmaManifestImportForm columnMapping);

    List<ParseError> validateManifest(ParsedManifest manifest);

    PharmaManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Get valid sample types for the Pharmaceutical laboratory. Returns sample
     * types that are both in the valid pharma list AND exist in the database.
     *
     * @return List of maps containing sample type info (id, description)
     */
    List<Map<String, String>> getValidPharmaSampleTypes();
}
