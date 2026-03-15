package org.openelisglobal.dataexchange.externalorders.service;

import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.dataexchange.externalorders.dto.ValidationReport;

/**
 * Service for validating external order requests. Validates patient, tests, and
 * panels before storage.
 */
public interface ExternalOrderValidationService {

    /**
     * Validate an external order request.
     * 
     * @param request the external order request to validate
     * @return ValidationReport containing validation results for patient, tests,
     *         and panels
     */
    ValidationReport validateOrder(ExternalOrderRequest request);

    /**
     * Validate a patient GUID.
     * 
     * @param patientGuid the patient GUID to validate
     * @return true if patient exists, false otherwise
     */
    boolean validatePatient(String patientGuid);

    /**
     * Create a filtered request containing only valid tests and panels.
     * 
     * @param original the original request
     * @param report   the validation report
     * @return a new request with only valid items, or null if nothing is valid
     */
    ExternalOrderRequest filterValidItems(ExternalOrderRequest original, ValidationReport report);
}
