package org.openelisglobal.common.externalLinks;

import java.util.List;
import java.util.concurrent.Future;
import org.openelisglobal.common.provider.query.ExtendedPatientSearchResults;

public interface IExternalPatientSearch {

    Future<Integer> runExternalSearch();

    void setSearchCriteria(String lastName, String firstName, String STNumber, String subjectNumber, String nationalID,
            String guid);

    void setConnectionCredentials(String connectionString, String name, String password);

    List<ExtendedPatientSearchResults> getSearchResults();

    String getConnectionString();

    int getTimeout();
}
