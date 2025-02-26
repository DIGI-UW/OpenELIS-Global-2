package org.openelisglobal.search.service;

import jakarta.transaction.Transactional;
import java.util.List;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.sample.dao.SearchResultsDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LuceneSearchResultsServiceImpl implements SearchResultsService {

    @Autowired
    @Qualifier("luceneSearchResultsDAOImpl")
    SearchResultsDAO searchResultsDAO;

    @Override
    @Transactional
    public List<PatientSearchResults> getSearchResults(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) {
        return searchResultsDAO.getSearchResults(lastName, firstName, STNumber, subjectNumber, nationalID, externalID,
                patientID, guid, dateOfBirth, gender);
    }

    @Override
    @Transactional
    public List<PatientSearchResults> getSearchResultsExact(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String externalID, String patientID, String guid,
            String dateOfBirth, String gender) {
        return searchResultsDAO.getSearchResultsExact(lastName, firstName, STNumber, subjectNumber, nationalID,
                externalID, patientID, guid, dateOfBirth, gender);
    }
}
