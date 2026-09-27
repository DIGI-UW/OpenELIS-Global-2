package org.openelisglobal.search.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.search.bundleProviders.DiagnosticReportBundleProvider;
import org.openelisglobal.fhir.search.searchparams.DiagnosticReportSearchParams;
import org.openelisglobal.search.dao.DiagnosticReportSearchDao;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local DiagnosticReport search. Status codes follow the outbound transform:
 * final, preliminary, partial and registered map to finalized, technically
 * accepted, technically rejected and not-started analyses.
 */
@Service
@Transactional(readOnly = true)
public class DiagnosticReportSearchService {

    private final DiagnosticReportSearchDao diagnosticReportSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final ObservationSearchDao observationSearchDao;
    private final FhirTransformService fhirTransformService;
    private final IStatusService statusService;

    public DiagnosticReportSearchService(DiagnosticReportSearchDao diagnosticReportSearchDao,
            SampleHumanSearchDao sampleHumanSearchDao, ObservationSearchDao observationSearchDao,
            FhirTransformService fhirTransformService, IStatusService statusService) {
        this.diagnosticReportSearchDao = diagnosticReportSearchDao;
        this.sampleHumanSearchDao = sampleHumanSearchDao;
        this.observationSearchDao = observationSearchDao;
        this.fhirTransformService = fhirTransformService;
        this.statusService = statusService;
    }

    public IBundleProvider searchDiagnosticReports(DiagnosticReportSearchParams params) {
        Map<String, List<String>> byCode = new LinkedHashMap<>();
        byCode.put("final", ids(AnalysisStatus.Finalized));
        byCode.put("preliminary", ids(AnalysisStatus.TechnicalAcceptance));
        byCode.put("partial", ids(AnalysisStatus.TechnicalRejected));
        byCode.put("registered", ids(AnalysisStatus.NotStarted));
        params.setStatusIdsByCode(byCode);
        return new DiagnosticReportBundleProvider(params, diagnosticReportSearchDao, sampleHumanSearchDao,
                observationSearchDao, fhirTransformService);
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
