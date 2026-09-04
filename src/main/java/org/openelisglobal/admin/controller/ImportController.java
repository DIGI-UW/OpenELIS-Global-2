package org.openelisglobal.admin.controller;

import java.io.IOException;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.organization.service.OrganizationImportService;
import org.openelisglobal.provider.service.ProviderImportService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/import")
// Admin-only FHIR import endpoints. The interceptor now fails open on unmapped
// /rest paths and these are not mapped, so this controller guard (restored from
// maintainer commit 0a3a335a1, lost in a rebase) is the real boundary — the
// service methods it calls are additionally gated (PRIV_ORGANIZATION_MANAGE /
// PRIV_PROVIDER_MANAGE).
@PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
public class ImportController {

    private enum ResourceType {
        ORGANIZATION, PROVIDER
    }

    private void importDataFromFhir(ResourceType resourceType)
            throws FhirLocalPersistingException, FhirGeneralException, IOException {
        switch (resourceType) {
        case ORGANIZATION:
            SpringContext.getBean(OrganizationImportService.class).importOrganizationList();
            break;
        case PROVIDER:
            SpringContext.getBean(ProviderImportService.class).importPractitionerList();
            break;
        default:
            // Handle invalid resource type
            throw new UnsupportedOperationException("Unsupported resource type");
        }
    }

    @GetMapping(value = "/all")
    public void importAll() throws FhirLocalPersistingException, FhirGeneralException, IOException {
        importDataFromFhir(ResourceType.ORGANIZATION);
        importDataFromFhir(ResourceType.PROVIDER);
    }

    @GetMapping(value = "/organization")
    public void importOrganizations() throws FhirLocalPersistingException, FhirGeneralException, IOException {
        importDataFromFhir(ResourceType.ORGANIZATION);
    }

    @GetMapping(value = "/provider")
    public void importProviders() throws FhirLocalPersistingException, FhirGeneralException, IOException {
        importDataFromFhir(ResourceType.PROVIDER);
    }
}
