package org.openelisglobal.patient.merge.service;

import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;

/**
 * Service for managing FHIR Patient link relationships during patient merge
 * operations. Implements FHIR R4 Patient.link functionality to establish
 * bidirectional relationships between merged patients and their primary
 * patient.
 *
 * @see <a href="https://www.hl7.org/fhir/patient.html#link">FHIR
 *      Patient.link</a>
 */
public interface FhirPatientLinkService {

    /**
     * Updates FHIR Patient resources with bidirectional link relationships after a
     * patient merge.
     *
     * <p>
     * This method performs the following operations:
     * <ul>
     * <li>Updates primary patient's FHIR resource with link type "replaces"
     * pointing to merged patient</li>
     * <li>Updates merged patient's FHIR resource with link type "replaced-by"
     * pointing to primary patient</li>
     * <li>Sets merged patient's active status to false</li>
     * </ul>
     *
     * <p>
     * Requirements:
     * <ul>
     * <li>FR-016: Primary patient MUST have link with type "replaces"</li>
     * <li>FR-017: Merged patient MUST have link with type "replaced-by" and active
     * = false</li>
     * </ul>
     *
     * @param primaryPatientId Internal database ID of the primary (surviving)
     *                         patient
     * @param mergedPatientId  Internal database ID of the merged (deactivated)
     *                         patient
     * @param primaryFhirUuid  FHIR UUID of the primary patient resource
     * @param mergedFhirUuid   FHIR UUID of the merged patient resource
     * @throws FhirLocalPersistingException If FHIR resource update fails or
     *                                      patients not found
     */
    void updatePatientLinks(String primaryPatientId, String mergedPatientId, String primaryFhirUuid,
            String mergedFhirUuid) throws FhirLocalPersistingException;

    /**
     * Checks if a patient has an associated FHIR resource (non-null fhirUuid).
     *
     * @param patientId Internal database ID of the patient
     * @return true if patient has a FHIR UUID, false otherwise
     */
    boolean hasFhirResource(String patientId);
}
