package org.openelisglobal.sample.dao;

import java.util.List;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.provider.query.PatientSearchResults;

public interface SearchResultsDAO {

    String FIRST_NAME_PARAM = "firstName";
    String LAST_NAME_PARAM = "lastName";
    String NATIONAL_ID_PARAM = "nationalID";
    String EXTERNAL_ID_PARAM = "externalID";
    String ST_NUMBER_PARAM = "stNumber";
    String SUBJECT_NUMBER_PARAM = "subjectNumber";
    String ID_PARAM = "id";
    String GUID = "guid";
    String DATE_OF_BIRTH = "dateOfBirth";
    String DATE_OF_BIRTH_FORMATED = "dateOfBirthFormatted";
    String GENDER = "gender";

    String ID_TYPE_FOR_ST = "stNumberId";
    String ID_TYPE_FOR_SUBJECT_NUMBER = "subjectNumberId";
    String ID_TYPE_FOR_GUID = "guidId";

    public List<PatientSearchResults> getSearchResults(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) throws LIMSRuntimeException;

    List<PatientSearchResults> getSearchResultsByGUID(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) throws LIMSRuntimeException;

    List<PatientSearchResults> getSearchResultsExact(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) throws LIMSRuntimeException;
}
