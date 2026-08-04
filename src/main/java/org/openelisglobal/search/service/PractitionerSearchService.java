package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.PractitionerBundleProvider;
import org.openelisglobal.fhir.search.searchparams.PractitionerSearchParams;
import org.openelisglobal.search.dao.PractitionerSearchDao;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PractitionerSearchService {

    private final PractitionerSearchDao practitionerSearchDao;

    private final SampleHumanSearchDao sampleHumanSearchDao;

    private final ServiceRequestSearchDao serviceRequestSearchDao;

    private final FhirTransformService fhirTransformService;

    public PractitionerSearchService(PractitionerSearchDao practitionerSearchDao,
            SampleHumanSearchDao sampleHumanSearchDao, ServiceRequestSearchDao serviceRequestSearchDao,
            FhirTransformService fhirTransformService) {

        this.practitionerSearchDao = practitionerSearchDao;
        this.sampleHumanSearchDao = sampleHumanSearchDao;
        this.serviceRequestSearchDao = serviceRequestSearchDao;
        this.fhirTransformService = fhirTransformService;
    }

    public IBundleProvider searchPractitioners(PractitionerSearchParams params) {

        return new PractitionerBundleProvider(params, practitionerSearchDao, sampleHumanSearchDao,
                serviceRequestSearchDao, fhirTransformService);
    }
}