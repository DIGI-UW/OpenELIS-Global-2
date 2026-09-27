package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.SpecimenBundleProvider;
import org.openelisglobal.fhir.search.searchparams.SpecimenSearchParams;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.openelisglobal.search.dao.SpecimenSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SpecimenSearchService {

    private final SpecimenSearchDao specimenSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final ServiceRequestSearchDao serviceRequestSearchDao;
    private final ObservationSearchDao observationSearchDao;
    private final FhirTransformService fhirTransformService;
    private final IStatusService statusService;

    public SpecimenSearchService(SpecimenSearchDao specimenSearchDao, SampleHumanSearchDao sampleHumanSearchDao,
            ServiceRequestSearchDao serviceRequestSearchDao, ObservationSearchDao observationSearchDao,
            FhirTransformService fhirTransformService, IStatusService statusService) {
        this.specimenSearchDao = specimenSearchDao;
        this.sampleHumanSearchDao = sampleHumanSearchDao;
        this.serviceRequestSearchDao = serviceRequestSearchDao;
        this.observationSearchDao = observationSearchDao;
        this.fhirTransformService = fhirTransformService;
        this.statusService = statusService;
    }

    public IBundleProvider searchSpecimens(SpecimenSearchParams params) {
        params.setCanceledStatusId(statusService.getStatusID(SampleStatus.Canceled));
        params.setDisposedStatusId(statusService.getStatusID(SampleStatus.Disposed));
        return new SpecimenBundleProvider(params, specimenSearchDao, sampleHumanSearchDao, serviceRequestSearchDao,
                observationSearchDao, fhirTransformService);
    }
}
