package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.ServiceRequestBundleProvider;
import org.openelisglobal.fhir.search.searchparams.ServiceRequestSearchParams;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.dao.PractitionerSearchDao;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local ServiceRequest search. The FHIR status codes are widened to the
 * OpenELIS analysis statuses the outbound transform folds into them.
 */
@Service
@Transactional(readOnly = true)
public class ServiceRequestSearchService {

    private final ServiceRequestSearchDao serviceRequestSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final PractitionerSearchDao practitionerSearchDao;
    private final ObservationSearchDao observationSearchDao;
    private final FhirTransformService fhirTransformService;
    private final IStatusService statusService;

    public ServiceRequestSearchService(ServiceRequestSearchDao serviceRequestSearchDao,
            SampleHumanSearchDao sampleHumanSearchDao, PractitionerSearchDao practitionerSearchDao,
            ObservationSearchDao observationSearchDao, FhirTransformService fhirTransformService,
            IStatusService statusService) {
        this.serviceRequestSearchDao = serviceRequestSearchDao;
        this.sampleHumanSearchDao = sampleHumanSearchDao;
        this.practitionerSearchDao = practitionerSearchDao;
        this.observationSearchDao = observationSearchDao;
        this.fhirTransformService = fhirTransformService;
        this.statusService = statusService;
    }

    public IBundleProvider searchServiceRequests(ServiceRequestSearchParams params) {
        params.setStatusIdsByCode(statusIdsByFhirCode());
        return new ServiceRequestBundleProvider(params, serviceRequestSearchDao, sampleHumanSearchDao,
                practitionerSearchDao, observationSearchDao, fhirTransformService, statusService);
    }

    private Map<String, List<String>> statusIdsByFhirCode() {
        Map<String, List<String>> byCode = new LinkedHashMap<>();
        byCode.put("active",
                ids(AnalysisStatus.NotStarted, AnalysisStatus.TechnicalAcceptance, AnalysisStatus.BiologistRejected));
        byCode.put("completed", ids(AnalysisStatus.Finalized));
        byCode.put("revoked", ids(AnalysisStatus.TechnicalRejected, AnalysisStatus.Canceled));
        byCode.put("entered-in-error", ids(AnalysisStatus.SampleRejected));
        return byCode;
    }

    private List<String> ids(AnalysisStatus... statuses) {
        List<String> ids = new ArrayList<>();
        for (AnalysisStatus status : statuses) {
            String id = statusService.getStatusID(status);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}
