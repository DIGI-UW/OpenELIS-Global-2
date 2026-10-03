package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.DiagnosticReportSearchParams;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.dao.DiagnosticReportSearchDao;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.service.SampleHumanSearchDao;

/**
 * Pages OpenELIS analyses as FHIR DiagnosticReport resources, adding the
 * patient, order, results and specimen for {@code _include}.
 */
public class DiagnosticReportBundleProvider extends BaseFhirBundleProvider<Analysis, DiagnosticReport> {

    private final DiagnosticReportSearchParams searchParams;
    private final DiagnosticReportSearchDao diagnosticReportSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final ObservationSearchDao observationSearchDao;
    private final FhirTransformService fhirTransformService;

    public DiagnosticReportBundleProvider(DiagnosticReportSearchParams searchParams,
            DiagnosticReportSearchDao diagnosticReportSearchDao, SampleHumanSearchDao sampleHumanSearchDao,
            ObservationSearchDao observationSearchDao, FhirTransformService fhirTransformService) {
        this.searchParams = Objects.requireNonNull(searchParams, "DiagnosticReportSearchParams must not be null");
        this.diagnosticReportSearchDao = Objects.requireNonNull(diagnosticReportSearchDao,
                "DiagnosticReportSearchDao must not be null");
        this.sampleHumanSearchDao = Objects.requireNonNull(sampleHumanSearchDao,
                "SampleHumanSearchDao must not be null");
        this.observationSearchDao = Objects.requireNonNull(observationSearchDao,
                "ObservationSearchDao must not be null");
        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
    }

    @Override
    protected List<Analysis> loadEntities(int offset, int pageSize) {
        return diagnosticReportSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {
        return diagnosticReportSearchDao.count(searchParams);
    }

    @Override
    protected DiagnosticReport transformEntity(Analysis analysis) {
        try {
            return fhirTransformService.transformResultToDiagnosticReport(analysis);
        } catch (FhirTransformationException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "transformEntity",
                    "skipping DiagnosticReport for analysis " + analysis.getId() + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<IBaseResource> getResources(int fromIndex, int toIndex) {

        int offset = effectiveOffset(fromIndex);
        int pageSize = effectivePageSize(fromIndex, toIndex);
        if (pageSize <= 0) {
            return List.of();
        }

        List<Analysis> analyses = loadEntities(offset, pageSize);
        List<IBaseResource> resources = new ArrayList<>();
        for (Analysis analysis : analyses) {
            DiagnosticReport report = transformEntity(analysis);
            if (report != null) {
                ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(report, BundleEntrySearchModeEnum.MATCH);
                resources.add(report);
            }
        }
        if (analyses.isEmpty()) {
            return resources;
        }
        int matchCount = resources.size();

        if (searchParams.hasInclude(FhirConstants.DIAGNOSTIC_REPORT_PATIENT_INCLUDE,
                FhirConstants.DIAGNOSTIC_REPORT_SUBJECT_INCLUDE)) {
            List<String> sampleIds = analyses.stream().map(Analysis::getSampleItem).filter(Objects::nonNull)
                    .map(item -> item.getSample().getId()).distinct().toList();
            Set<String> patientIds = new LinkedHashSet<>();
            for (SampleHuman sampleHuman : sampleHumanSearchDao.findBySampleIds(sampleIds)) {
                if (sampleHuman.getPatientId() != null) {
                    patientIds.add(sampleHuman.getPatientId());
                }
            }
            for (String patientId : patientIds) {
                try {
                    resources.add(fhirTransformService.transformToFhirPatient(patientId));
                } catch (FhirTransformationException e) {
                    LogEvent.logWarn(getClass().getSimpleName(), "getResources",
                            "skipping Patient " + patientId + ": " + e.getMessage());
                }
            }
        }
        if (searchParams.hasInclude(FhirConstants.DIAGNOSTIC_REPORT_BASED_ON_INCLUDE)) {
            analyses.stream().map(analysis -> fhirTransformService.transformToServiceRequest(analysis.getId()))
                    .filter(Objects::nonNull).forEach(resources::add);
        }
        if (searchParams.hasInclude(FhirConstants.DIAGNOSTIC_REPORT_RESULT_INCLUDE)) {
            List<String> analysisIds = analyses.stream().map(Analysis::getId).toList();
            for (Result result : observationSearchDao.findByAnalysisIds(analysisIds)) {
                try {
                    resources.add(fhirTransformService.transformResultToObservation(result));
                } catch (FhirTransformationException e) {
                    LogEvent.logWarn(getClass().getSimpleName(), "getResources",
                            "skipping Observation for result " + result.getId() + ": " + e.getMessage());
                }
            }
        }
        if (searchParams.hasInclude(FhirConstants.DIAGNOSTIC_REPORT_SPECIMEN_INCLUDE)) {
            analyses.stream().map(Analysis::getSampleItem).filter(Objects::nonNull).distinct()
                    .map(fhirTransformService::transformToSpecimen).forEach(resources::add);
        }

        resources.subList(matchCount, resources.size()).forEach(
                resource -> ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(resource, BundleEntrySearchModeEnum.INCLUDE));
        return resources;
    }
}
