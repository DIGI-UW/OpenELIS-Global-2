package org.openelisglobal.organization.service;

import java.io.IOException;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "startup configuration and admin (PRIV_SYSTEM_CONFIGURE) — called from ConfigurationInitializationService during boot and admin import")
public interface OrganizationImportService {

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    void importOrganizationList() throws FhirLocalPersistingException, FhirGeneralException, IOException;
}
