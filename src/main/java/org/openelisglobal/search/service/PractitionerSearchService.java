package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.PractitionerBundleProvider;
import org.openelisglobal.fhir.search.searchparams.PractitionerSearchParams;
import org.openelisglobal.search.service.dao.PractitionerSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PractitionerSearchService {

    private final PractitionerSearchDao practitionerSearchDao;
    private final FhirTransformService fhirTransformService;

    public PractitionerSearchService(PractitionerSearchDao practitionerSearchDao,
            FhirTransformService fhirTransformService) {

        this.practitionerSearchDao = practitionerSearchDao;
        this.fhirTransformService = fhirTransformService;
    }

    /**
     * Returns a lazy DAO-backed bundle provider.
     *
     * Practitioner entities are loaded only when HAPI requests a result page.
     */
    public IBundleProvider searchPractitioners(PractitionerSearchParams params) {

        return new PractitionerBundleProvider(params, practitionerSearchDao, fhirTransformService);
    }
}
