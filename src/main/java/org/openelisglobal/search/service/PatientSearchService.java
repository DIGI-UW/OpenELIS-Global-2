package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.PatientBundleProvider;
import org.openelisglobal.fhir.search.searchparams.PatientSearchParams;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.dao.PatientSearchDao;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.openelisglobal.search.dao.SpecimenSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PatientSearchService {

    private final PatientSearchDao patientSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final ServiceRequestSearchDao serviceRequestSearchDao;
    private final SpecimenSearchDao specimenSearchDao;
    private final ObservationSearchDao observationSearchDao;
    private final FhirTransformService fhirTransformService;

    public PatientSearchService(PatientSearchDao patientSearchDao, SampleHumanSearchDao sampleHumanSearchDao,
            ServiceRequestSearchDao serviceRequestSearchDao, SpecimenSearchDao specimenSearchDao,
            ObservationSearchDao observationSearchDao, FhirTransformService fhirTransformService) {
        this.patientSearchDao = patientSearchDao;
        this.sampleHumanSearchDao = sampleHumanSearchDao;
        this.serviceRequestSearchDao = serviceRequestSearchDao;
        this.specimenSearchDao = specimenSearchDao;
        this.observationSearchDao = observationSearchDao;
        this.fhirTransformService = fhirTransformService;
    }

    public IBundleProvider searchPatients(PatientSearchParams params) {
        return new PatientBundleProvider(params, patientSearchDao, sampleHumanSearchDao, serviceRequestSearchDao,
                specimenSearchDao, observationSearchDao, fhirTransformService);
    }
}
