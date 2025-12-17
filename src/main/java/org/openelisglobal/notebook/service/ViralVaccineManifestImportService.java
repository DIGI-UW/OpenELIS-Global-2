package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import org.openelisglobal.notebook.form.ViralVaccineManifestImportForm;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Service for importing Viral & Vaccine Unit Laboratory samples from manifest
 * CSV files. Handles Viral & Vaccine-specific data points including sample
 * identity, metadata, storage, and testing intent fields.
 */
public interface ViralVaccineManifestImportService {

    /**
     * Result of manifest parsing operation.
     */
    class ParsedManifest {
        private final List<ViralVaccineManifestRow> rows;
        private final List<ParseError> errors;

        public ParsedManifest(List<ViralVaccineManifestRow> rows, List<ParseError> errors) {
            this.rows = rows;
            this.errors = errors;
        }

        public List<ViralVaccineManifestRow> rows() {
            return rows;
        }

        public List<ParseError> errors() {
            return errors;
        }
    }

    /**
     * A single row from the Viral & Vaccine manifest CSV with all workflow-specific
     * fields.
     */
    class ViralVaccineManifestRow {
        private final int rowNumber;
        private final String sampleId;
        private final String sampleName;
        private final String sampleType;
        private final String batchId;
        private final String sourceOrganism;
        private final String passageNumber;
        private final String collectionDate;
        private final String collectedBy;
        private final String originLab;
        private final String storageLocation;
        private final String storageTemperature;
        private final String volume;
        private final String intendedUse;
        private final String projectName;
        private final int numOfSamples;

        public ViralVaccineManifestRow(int rowNumber, String sampleId, String sampleName, String sampleType,
                String batchId, String sourceOrganism, String passageNumber, String collectionDate, String collectedBy,
                String originLab, String storageLocation, String storageTemperature, String volume, String intendedUse,
                String projectName, int numOfSamples) {
            this.rowNumber = rowNumber;
            this.sampleId = sampleId;
            this.sampleName = sampleName;
            this.sampleType = sampleType;
            this.batchId = batchId;
            this.sourceOrganism = sourceOrganism;
            this.passageNumber = passageNumber;
            this.collectionDate = collectionDate;
            this.collectedBy = collectedBy;
            this.originLab = originLab;
            this.storageLocation = storageLocation;
            this.storageTemperature = storageTemperature;
            this.volume = volume;
            this.intendedUse = intendedUse;
            this.projectName = projectName;
            this.numOfSamples = numOfSamples;
        }

        public int rowNumber() {
            return rowNumber;
        }

        public String sampleId() {
            return sampleId;
        }

        public String sampleName() {
            return sampleName;
        }

        public String sampleType() {
            return sampleType;
        }

        public String batchId() {
            return batchId;
        }

        public String sourceOrganism() {
            return sourceOrganism;
        }

        public String passageNumber() {
            return passageNumber;
        }

        public String collectionDate() {
            return collectionDate;
        }

        public String collectedBy() {
            return collectedBy;
        }

        public String originLab() {
            return originLab;
        }

        public String storageLocation() {
            return storageLocation;
        }

        public String storageTemperature() {
            return storageTemperature;
        }

        public String volume() {
            return volume;
        }

        public String intendedUse() {
            return intendedUse;
        }

        public String projectName() {
            return projectName;
        }

        public int numOfSamples() {
            return numOfSamples;
        }
    }

    /**
     * Error during manifest parsing or validation.
     */
    class ParseError {
        private final int rowNumber;
        private final String column;
        private final String message;

        public ParseError(int rowNumber, String column, String message) {
            this.rowNumber = rowNumber;
            this.column = column;
            this.message = message;
        }

        public int rowNumber() {
            return rowNumber;
        }

        public String column() {
            return column;
        }

        public String message() {
            return message;
        }
    }

    /**
     * Result of sample creation from manifest.
     */
    class ViralVaccineManifestImportResult {
        private final int totalRequested;
        private final int totalCreated;
        private final List<SampleItem> createdSamples;
        private final List<String> createdAccessionNumbers;
        private final List<ParseError> errors;

        public ViralVaccineManifestImportResult(int totalRequested, int totalCreated, List<SampleItem> createdSamples,
                List<String> createdAccessionNumbers, List<ParseError> errors) {
            this.totalRequested = totalRequested;
            this.totalCreated = totalCreated;
            this.createdSamples = createdSamples;
            this.createdAccessionNumbers = createdAccessionNumbers;
            this.errors = errors;
        }

        public int totalRequested() {
            return totalRequested;
        }

        public int totalCreated() {
            return totalCreated;
        }

        public List<SampleItem> createdSamples() {
            return createdSamples;
        }

        public List<String> createdAccessionNumbers() {
            return createdAccessionNumbers;
        }

        public List<ParseError> errors() {
            return errors;
        }
    }

    /**
     * Parse a Viral & Vaccine manifest CSV file.
     *
     * @param csvInput      the CSV input stream
     * @param columnMapping the Viral & Vaccine column mapping configuration
     * @return parsed manifest with rows and any parsing errors
     */
    ParsedManifest parseManifestCsv(InputStream csvInput, ViralVaccineManifestImportForm columnMapping);

    /**
     * Validate sample types in a parsed manifest against TypeOfSample records.
     *
     * @param manifest the parsed manifest to validate
     * @return list of validation errors (empty if all valid)
     */
    List<ParseError> validateSampleTypes(ParsedManifest manifest);

    /**
     * Create SampleItem records from a Viral & Vaccine manifest for a notebook
     * entry. Each row with numOfSamples > 1 creates multiple SampleItem records.
     * Viral & Vaccine-specific data is stored in sample metadata.
     *
     * @param entryId   the notebook entry to link samples to
     * @param manifest  the parsed and validated manifest
     * @param sysUserId the user creating the samples
     * @return result containing created samples, accession numbers, and any errors
     */
    ViralVaccineManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest, String sysUserId);

    /**
     * Generate external ID for a Viral & Vaccine sample based on sample ID and
     * sequence number. Format: {sampleId}-{sequenceNumber padded to 3 digits}
     *
     * @param sampleId       the sample identifier from manifest
     * @param sequenceNumber the sequence number within the group (1-based)
     * @return the formatted external ID
     */
    String generateExternalId(String sampleId, int sequenceNumber);
}
