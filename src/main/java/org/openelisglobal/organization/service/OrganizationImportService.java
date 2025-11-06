package org.openelisglobal.organization.service;

import java.io.IOException;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;

public interface OrganizationImportService {

    void importOrganizationList() throws FhirLocalPersistingException, FhirGeneralException, IOException;
}
