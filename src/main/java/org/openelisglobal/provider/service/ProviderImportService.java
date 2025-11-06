package org.openelisglobal.provider.service;

import java.io.IOException;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;

public interface ProviderImportService {

    void importPractitionerList() throws FhirLocalPersistingException, FhirGeneralException, IOException;
}
