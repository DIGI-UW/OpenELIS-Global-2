package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.ObservationBundleProvider;
import org.openelisglobal.fhir.search.searchparams.ObservationSearchParams;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.dao.PractitionerSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local Observation search. Status codes follow the outbound transform: final
 * is a finalized analysis, unknown a not-started one, and preliminary
 * everything else.
 */
@Service
@Transactional(readOnly = true)
public class ObservationSearchService {

    private final ObservationSearchDao observationSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final PractitionerSearchDao practitionerSearchDao;
    private final FhirTransformService fhirTransformService;
    private final IStatusService statusService;

    public ObservationSearchService(ObservationSearchDao observationSearchDao,
            SampleHumanSearchDao sampleHumanSearchDao, PractitionerSearchDao practitionerSearchDao,
            FhirTransformService fhirTransformService, IStatusService statusService) {
        this.observationSearchDao = observationSearchDao;
        this.sampleHumanSearchDao = sampleHumanSearchDao;
        this.practitionerSearchDao = practitionerSearchDao;
        this.fhirTransformService = fhirTransformService;
        this.statusService = statusService;
    }

    public IBundleProvider searchObservations(ObservationSearchParams params) {
        Map<String, List<String>> byCode = new LinkedHashMap<>();
        byCode.put("final", ids(AnalysisStatus.Finalized));
        byCode.put("unknown", ids(AnalysisStatus.NotStarted));
        params.setStatusIdsByCode(byCode);
        return new ObservationBundleProvider(params, observationSearchDao, sampleHumanSearchDao, practitionerSearchDao,
                fhirTransformService, statusService);
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
