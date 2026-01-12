/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.service;

import java.io.InputStream;
import java.util.List;
import org.openelisglobal.medlab.form.MedLabManifestImportForm;

/**
 * Service interface for MedLab manifest import operations.
 *
 * <p>
 * Handles the two-step sample collection workflow:
 * <ol>
 * <li>Step 1: Import Samples from Manifest - Parse CSV with 13 fields
 * <li>Step 2: Link Samples to Orders - Associate samples with orders via
 * OrderSampleLinkService
 * </ol>
 *
 * <p>
 * Manifest fields per spec FR-010 to FR-014:
 * <ul>
 * <li>sampleId → SampleItem.accessionNumber
 * <li>sampleTypeId → SampleItem.typeOfSample
 * <li>containerType → SampleItem.collectionContainer
 * <li>customLabel → SampleItem.externalId
 * <li>quantity → SampleItem.initialQuantity
 * <li>unitOfMeasure → SampleItem.unitOfMeasure
 * <li>collectionSource → Sample.source
 * <li>collector → SampleItem.collector
 * <li>collectionDate + collectionTime → SampleItem.collectionDate
 * <li>orderId → OrderSampleLink (optional, linked in Step 2)
 * <li>patientId → Sample.patientId (NULL for anonymous participants)
 * <li>notes → SampleItem.note
 * </ul>
 */
public interface MedLabManifestImportService {

    /**
     * Parse a MedLab manifest CSV file.
     *
     * @param csvInput      the CSV input stream
     * @param columnMapping the column mapping configuration
     * @return the parsed manifest with rows and errors
     */
    ParsedMedLabManifest parseManifestCsv(InputStream csvInput, MedLabManifestImportForm columnMapping);

    /**
     * Validate sample types in the parsed manifest.
     *
     * @param manifest the parsed manifest
     * @return list of validation errors
     */
    List<ParseError> validateSampleTypes(ParsedMedLabManifest manifest);

    /**
     * Validate container types in the parsed manifest.
     *
     * @param manifest the parsed manifest
     * @return list of validation errors
     */
    List<ParseError> validateContainerTypes(ParsedMedLabManifest manifest);

    /**
     * Validate that sample IDs (accession numbers) don't already exist in the
     * database.
     *
     * <p>
     * This prevents unique constraint violations during import. The
     * accession_number column has a unique constraint (accnum_uk), so attempting to
     * insert a duplicate would fail and cause a transaction rollback.
     *
     * @param manifest the parsed manifest
     * @return list of validation errors for duplicate sample IDs
     */
    List<ParseError> validateDuplicateSampleIds(ParsedMedLabManifest manifest);

    /**
     * Validate patient and order references in the parsed manifest.
     *
     * <p>
     * Checks each row for:
     * <ul>
     * <li>If patientId provided, verify patient exists in the system
     * <li>If orderId provided, verify order exists in the system
     * <li>If both provided, verify the order belongs to the specified patient
     * </ul>
     *
     * <p>
     * Returns warnings (not blocking errors) since samples can be created without
     * patient/order links per the two-step workflow.
     *
     * @param manifest the parsed manifest
     * @return validation result with errors and warnings
     */
    ValidationResult validatePatientAndOrderReferences(ParsedMedLabManifest manifest);

    /**
     * Create samples from the parsed manifest for a notebook entry.
     *
     * <p>
     * This is Step 1 of the workflow: samples are created but not yet linked to
     * orders. Step 2 (linking to orders) is done via OrderSampleLinkService.
     *
     * @param entryId       the notebook entry ID
     * @param manifest      the parsed manifest
     * @param createdBy     the user ID creating the samples
     * @param selectedTests optional list of test IDs to create Analysis records for
     * @param pageId        optional notebook page ID to link samples to the page
     * @return the import result with sample counts
     */
    ImportResult createSamplesForEntry(Integer entryId, ParsedMedLabManifest manifest, Integer createdBy,
            List<Integer> selectedTests, Integer pageId);

    /**
     * Parsed manifest row representing one sample.
     */
    record MedLabManifestRow(int rowNumber, String sampleId, String sampleType, String containerType,
            String customLabel, String quantity, String unitOfMeasure, String collectionSource, String collector,
            String collectionDate, String collectionTime, String orderId, String patientId, String notes) {
    }

    /**
     * Parsed manifest containing rows and errors.
     */
    record ParsedMedLabManifest(List<MedLabManifestRow> rows, List<ParseError> errors) {

        public boolean isValid() {
            return errors.isEmpty();
        }

        public int getTotalRows() {
            return rows.size();
        }
    }

    /**
     * Parse error for a specific row and column.
     */
    record ParseError(int rowNumber, String column, String message) {
    }

    /**
     * Import result with sample and analysis counts.
     */
    record ImportResult(boolean success, int samplesCreated, int analysesCreated, List<ParseError> errors) {

        public static ImportResult success(int samplesCreated, int analysesCreated) {
            return new ImportResult(true, samplesCreated, analysesCreated, List.of());
        }

        public static ImportResult failure(List<ParseError> errors) {
            return new ImportResult(false, 0, 0, errors);
        }
    }

    /**
     * Validation warning for non-blocking issues (e.g., patient/order not found).
     *
     * <p>
     * Unlike ParseError, warnings don't prevent import but inform the user of
     * potential issues with patient/order linking.
     */
    record ValidationWarning(int rowNumber, String column, String message, WarningType type) {

        public enum WarningType {
            PATIENT_NOT_FOUND, ORDER_NOT_FOUND, PATIENT_ORDER_MISMATCH, INFO
        }
    }

    /**
     * Validation result containing both blocking errors and non-blocking warnings.
     */
    record ValidationResult(List<ParseError> errors, List<ValidationWarning> warnings) {

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /**
         * Get row numbers that have blocking errors.
         */
        public List<Integer> getInvalidRowNumbers() {
            return errors.stream().map(ParseError::rowNumber).distinct().toList();
        }

        /**
         * Get row numbers that have warnings but no errors.
         */
        public List<Integer> getWarningRowNumbers() {
            List<Integer> errorRows = getInvalidRowNumbers();
            return warnings.stream().map(ValidationWarning::rowNumber).distinct().filter(r -> !errorRows.contains(r))
                    .toList();
        }

        public static ValidationResult empty() {
            return new ValidationResult(List.of(), List.of());
        }
    }
}
