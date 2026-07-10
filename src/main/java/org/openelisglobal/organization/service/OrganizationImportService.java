package org.openelisglobal.organization.service;

import java.io.IOException;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "Spring scheduler (facilitylist fixedRate FHIR sync, runs without an Authentication"
        + " in system context) and the admin Import endpoint (gated importOrganizationList)")
public interface OrganizationImportService {

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    void importOrganizationList() throws FhirLocalPersistingException, FhirGeneralException, IOException;

    /**
     * Scheduler entry point — declared on the interface because the JDK-proxied
     * bean's @Scheduled method must be interface-visible. Runs the import in system
     * context (SystemInitFlag); not exposed by any controller.
     */
    void scheduledImportOrganizationList();
}
