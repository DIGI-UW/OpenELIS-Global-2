package org.openelisglobal.organization.service;

import java.io.IOException;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OrganizationImportService {

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    void importOrganizationList() throws FhirLocalPersistingException, FhirGeneralException, IOException;
}
