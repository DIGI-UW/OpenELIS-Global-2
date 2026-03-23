package org.openelisglobal.search.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.sample.dao.SearchResultsDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class DBSearchResultsServiceImpl implements SearchResultsService {

    @Autowired
    SearchResultsDAO searchResultsDAO;

    @Autowired
    PatientService patientService;

    @Override
    @Transactional
    public List<PatientSearchResults> getSearchResults(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) {
        List<PatientSearchResults> results = searchResultsDAO.getSearchResults(lastName, firstName, STNumber,
                subjectNumber, nationalID, externalID, patientID, guid, dateOfBirth, gender);

        // FR-015: Redirect merged patients to primary when searching by identifier
        // Only apply redirect for identifier-based searches, not for name/primary ID
        // searches
        boolean isIdentifierSearch = !GenericValidator.isBlankOrNull(nationalID)
                || !GenericValidator.isBlankOrNull(subjectNumber) || !GenericValidator.isBlankOrNull(STNumber)
                || !GenericValidator.isBlankOrNull(externalID);

        if (isIdentifierSearch) {
            results = redirectMergedPatientsToPrimary(results);
        }

        return results;
    }

    @Override
    @Transactional
    public List<PatientSearchResults> getSearchResultsExact(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) {
        List<PatientSearchResults> results = searchResultsDAO.getSearchResultsExact(lastName, firstName, STNumber,
                subjectNumber, nationalID, externalID, patientID, guid, dateOfBirth, gender);

        // FR-015: Redirect merged patients to primary when searching by identifier
        boolean isIdentifierSearch = !GenericValidator.isBlankOrNull(nationalID)
                || !GenericValidator.isBlankOrNull(subjectNumber) || !GenericValidator.isBlankOrNull(STNumber)
                || !GenericValidator.isBlankOrNull(externalID);

        if (isIdentifierSearch) {
            results = redirectMergedPatientsToPrimary(results);
        }

        return results;
    }

    /**
     * FR-015: Redirects merged patients to their primary patient in search results.
     * When a search returns a merged patient, replace it with the primary patient's
     * data. Also deduplicates results if both merged and primary patients were
     * returned.
     */
    private List<PatientSearchResults> redirectMergedPatientsToPrimary(List<PatientSearchResults> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }

        List<PatientSearchResults> processedResults = new ArrayList<>();
        Set<String> addedPatientIds = new HashSet<>();

        for (PatientSearchResults result : results) {
            String patientId = result.getPatientID();
            Patient patient = patientService.get(patientId);

            if (patient != null && Boolean.TRUE.equals(patient.getIsMerged())
                    && patient.getMergedIntoPatientId() != null) {
                // This patient was merged - get the primary patient instead
                String primaryPatientId = patient.getMergedIntoPatientId();

                // Avoid duplicates if primary was already in results
                if (!addedPatientIds.contains(primaryPatientId)) {
                    Patient primaryPatient = patientService.get(primaryPatientId);
                    if (primaryPatient != null) {
                        PatientSearchResults primaryResult = convertPatientToSearchResult(primaryPatient);
                        processedResults.add(primaryResult);
                        addedPatientIds.add(primaryPatientId);
                    }
                }
            } else {
                // Not merged - add as-is if not already added
                if (!addedPatientIds.contains(patientId)) {
                    processedResults.add(result);
                    addedPatientIds.add(patientId);
                }
            }
        }

        return processedResults;
    }

    /**
     * Converts a Patient entity to PatientSearchResults for consistent return
     * format.
     */
    private PatientSearchResults convertPatientToSearchResult(Patient patient) {
        PatientSearchResults result = new PatientSearchResults();
        result.setPatientID(patient.getId());
        if (patient.getPerson() != null) {
            result.setFirstName(patient.getPerson().getFirstName());
            result.setLastName(patient.getPerson().getLastName());
        }
        result.setGender(patient.getGender());
        result.setBirthdate(patient.getBirthDateForDisplay());
        result.setNationalId(patient.getNationalId());
        result.setExternalId(patient.getExternalId());
        // Note: ST number and subject number would need to be fetched from
        // patient_identity
        // but the redirect provides the essential patient data for selection
        return result;
    }
}
