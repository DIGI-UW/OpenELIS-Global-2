package org.openelisglobal.search.service;

import java.util.List;
import org.openelisglobal.common.provider.query.PatientSearchResults;

public interface SearchResultsService {

    List<PatientSearchResults> getSearchResults(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender);

    List<PatientSearchResults> getSearchResultsExact(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender);
}
