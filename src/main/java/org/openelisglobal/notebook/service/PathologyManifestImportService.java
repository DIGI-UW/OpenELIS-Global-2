package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.form.PathologyManifestImportForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for importing Pathology Laboratory samples from manifest CSV files.
 * Handles pathology-specific data points including:
 *
 * Patient Identification: - First Name (MANDATORY - primary name field for
 * order acceptance) - Surname/Last Name (OPTIONAL) - National ID (OPTIONAL)
 *
 * Clinical metadata: Patient ID, requesting clinician, clinical details,
 * specimen type/site, collection date/time Research metadata: Study ID, PI
 * name, participant/animal ID, ethical approval reference All samples:
 * Receiving date/time, receiving staff name, source facility
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
     *
     * Patient Identification: - firstName: MANDATORY - primary name field for order
     * acceptance - surname: OPTIONAL - not required for order acceptance -
     * nationalId: OPTIONAL - not required for order acceptance
     *
     * Sample Attributes (ALL MANDATORY): - specimenType: Type of specimen (Tissue,
     * Cytology, etc.) - specimenSite: Patient Site / Anatomical Source -
     * collectionDateTime: Date and time of collection - collectionMethod: How the
     * sample was collected (Biopsy, FNAC, etc.) - collector: Personnel who
     * collected the sample - processingCondition: Condition/state of sample (Fresh,
     * Fixed, etc.) - laboratoryMaterial: Material type (Blocks, Slides, etc.)
     */
    record PathologyManifestRow(int rowNumber, String firstName, String surname, String nationalId,
            String sampleCategory, String receivedDateTime, String receivedBy, String sourceFacility,
            String specimenType, String specimenSite, String collectionDateTime, String collectionMethod,
            String collector, String processingCondition, String laboratoryMaterial, String patientId,
            String requestingClinician, String clinicalDetails, String studyId, String piName,
            String participantAnimalId, String ethicalApprovalRef, String remarks) {
    }

    /**
     * Check if a manifest row is a clinical sample based on sample category.
     */
    static boolean isClinical(PathologyManifestRow row) {
        return row.sampleCategory() != null && row.sampleCategory().toLowerCase().contains("clinical");
    }

    /**
     * Check if a manifest row is a research sample based on sample category.
     */
    static boolean isResearch(PathologyManifestRow row) {
        return row.sampleCategory() != null && row.sampleCategory().toLowerCase().contains("research");
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
     * @param csvInput       the CSV input stream
     * @param columnMapping  the Pathology column mapping configuration
     * @param sampleCategory the sample category ("Clinical diagnostic" or
     *                       "Research") determined by the import type, not from CSV
     * @return parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, PathologyManifestImportForm columnMapping,
            String sampleCategory);

    /**
     * Validate sample types in a parsed manifest against TypeOfSample records.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSampleTypes(ParsedManifest manifest);

    /**
     * Validate clinical and research samples have appropriate metadata fields.
     * Validates that: - First Name is present (MANDATORY for all samples) -
     * Clinical samples have patient ID, requesting clinician, clinical details -
     * Research samples have study ID, PI name, participant/animal ID, ethical
     * approval ref - All samples have receiving date/time and receiving staff name
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors for missing required metadata
     */
    List<ParseError> validateCategoryMetadata(ParsedManifest manifest);

    /**
     * Create SampleItem records from a Pathology manifest for a notebook entry.
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
     * Generate external ID for a Pathology sample based on first name, specimen
     * type, and sequence number. Format:
     * {firstName}-{specimenTypeAbbrev}-{sequenceNumber padded to 3 digits}
     *
     * @param firstName      the patient/participant first name
     * @param specimenType   the specimen type
     * @param sequenceNumber the sequence number (1-based)
     * @return the formatted external ID
     */
    String generateExternalId(String firstName, String specimenType, int sequenceNumber);

    /**
     * Get valid sample types for the Pathology laboratory. Returns sample types
     * that are configured for pathology workflows.
     *
     * @return List of maps containing sample type info (id, description)
     */
    List<Map<String, String>> getValidPathologySampleTypes();
}
