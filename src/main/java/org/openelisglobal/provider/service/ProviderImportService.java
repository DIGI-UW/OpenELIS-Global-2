package org.openelisglobal.provider.service;

import java.io.IOException;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "Spring scheduler (scheduledImportPractitionerList, runs without an Authentication"
        + " in system context) and the admin Import endpoint (ImportController, gated importPractitionerList)")
public interface ProviderImportService {

    @PreAuthorize("hasAuthority('PRIV_PROVIDER_MANAGE')")
    void importPractitionerList() throws FhirLocalPersistingException, FhirGeneralException, IOException;

    /**
     * Scheduler entry point — declared on the interface because the JDK-proxied
     * bean's @Scheduled method must be interface-visible. Runs the import in system
     * context (SystemInitFlag); not exposed by any controller.
     */
    void scheduledImportPractitionerList();
}
