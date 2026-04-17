package org.openelisglobal.provider.service;

import java.io.IOException;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;

@CrossDomainService(callers = "FHIR provider import pipeline — internal infrastructure")
public interface ProviderImportService {

    void importPractitionerList() throws FhirLocalPersistingException, FhirGeneralException, IOException;
}
