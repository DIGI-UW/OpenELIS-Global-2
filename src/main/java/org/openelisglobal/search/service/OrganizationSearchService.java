package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.OrganizationBundleProvider;
import org.openelisglobal.fhir.search.searchparams.OrganizationSearchParams;
import org.openelisglobal.search.dao.OrganizationSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationSearchService {

    private final OrganizationSearchDao organizationSearchDao;
    private final FhirTransformService fhirTransformService;

    public OrganizationSearchService(OrganizationSearchDao organizationSearchDao,
            FhirTransformService fhirTransformService) {
        this.organizationSearchDao = organizationSearchDao;
        this.fhirTransformService = fhirTransformService;
    }

    public IBundleProvider searchOrganizations(OrganizationSearchParams params) {
        return new OrganizationBundleProvider(params, organizationSearchDao, fhirTransformService);
    }
}
