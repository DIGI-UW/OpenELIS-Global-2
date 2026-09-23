package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Observation;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.ObservationSearchParams;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.dao.PractitionerSearchDao;
import org.openelisglobal.search.service.SampleHumanSearchDao;

/**
 * Pages OpenELIS results as FHIR Observation resources, adding the patient,
 * order, specimen and performer for {@code _include} and the finalized report
 * for {@code _revinclude=DiagnosticReport:result}.
 */
public class ObservationBundleProvider extends BaseFhirBundleProvider<Result, Observation> {

    private final ObservationSearchParams searchParams;
    private final ObservationSearchDao observationSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final PractitionerSearchDao practitionerSearchDao;
    private final FhirTransformService fhirTransformService;
    private final IStatusService statusService;

    public ObservationBundleProvider(ObservationSearchParams searchParams, ObservationSearchDao observationSearchDao,
            SampleHumanSearchDao sampleHumanSearchDao, PractitionerSearchDao practitionerSearchDao,
            FhirTransformService fhirTransformService, IStatusService statusService) {
        this.searchParams = Objects.requireNonNull(searchParams, "ObservationSearchParams must not be null");
        this.observationSearchDao = Objects.requireNonNull(observationSearchDao,
                "ObservationSearchDao must not be null");
        this.sampleHumanSearchDao = Objects.requireNonNull(sampleHumanSearchDao,
                "SampleHumanSearchDao must not be null");
        this.practitionerSearchDao = Objects.requireNonNull(practitionerSearchDao,
                "PractitionerSearchDao must not be null");
        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
        this.statusService = Objects.requireNonNull(statusService, "IStatusService must not be null");
    }

    @Override
    protected List<Result> loadEntities(int offset, int pageSize) {
        return observationSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {
        return observationSearchDao.count(searchParams);
    }

    @Override
    protected Observation transformEntity(Result result) {
        try {
            return fhirTransformService.transformResultToObservation(result);
        } catch (FhirTransformationException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "transformEntity",
                    "skipping Observation for result " + result.getId() + ": " + e.getMessage());
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

        List<Result> results = loadEntities(offset, pageSize);
        List<IBaseResource> resources = new ArrayList<>();
        for (Result result : results) {
            Observation observation = transformEntity(result);
            if (observation != null) {
                ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(observation, BundleEntrySearchModeEnum.MATCH);
                resources.add(observation);
            }
        }
        if (results.isEmpty()) {
            return resources;
        }
        int matchCount = resources.size();

        Map<String, Analysis> analyses = new LinkedHashMap<>();
        for (Result result : results) {
            Analysis analysis = result.getAnalysis();
            if (analysis != null) {
                analyses.putIfAbsent(analysis.getId(), analysis);
            }
        }

        boolean wantsPatient = searchParams.hasInclude(FhirConstants.OBSERVATION_PATIENT_INCLUDE,
                FhirConstants.OBSERVATION_SUBJECT_INCLUDE);
        boolean wantsPerformer = searchParams.hasInclude(FhirConstants.OBSERVATION_PERFORMER_INCLUDE);
        if (wantsPatient || wantsPerformer) {
            List<String> sampleIds = analyses.values().stream().map(Analysis::getSampleItem).filter(Objects::nonNull)
                    .map(item -> item.getSample().getId()).distinct().toList();
            Set<String> patientIds = new LinkedHashSet<>();
            Set<String> providerIds = new LinkedHashSet<>();
            for (SampleHuman sampleHuman : sampleHumanSearchDao.findBySampleIds(sampleIds)) {
                if (sampleHuman.getPatientId() != null) {
                    patientIds.add(sampleHuman.getPatientId());
                }
                if (sampleHuman.getProviderId() != null) {
                    providerIds.add(sampleHuman.getProviderId());
                }
            }
            if (wantsPatient) {
                for (String patientId : patientIds) {
                    try {
                        resources.add(fhirTransformService.transformToFhirPatient(patientId));
                    } catch (FhirTransformationException e) {
                        LogEvent.logWarn(getClass().getSimpleName(), "getResources",
                                "skipping Patient " + patientId + ": " + e.getMessage());
                    }
                }
            }
            if (wantsPerformer) {
                for (Provider provider : practitionerSearchDao.findByIds(new ArrayList<>(providerIds))) {
                    resources.add(fhirTransformService.transformProviderToPractitioner(provider));
                }
            }
        }

        if (searchParams.hasInclude(FhirConstants.OBSERVATION_BASED_ON_INCLUDE)) {
            analyses.values().stream().map(analysis -> fhirTransformService.transformToServiceRequest(analysis.getId()))
                    .filter(Objects::nonNull).forEach(resources::add);
        }
        if (searchParams.hasInclude(FhirConstants.OBSERVATION_SPECIMEN_INCLUDE)) {
            analyses.values().stream().map(Analysis::getSampleItem).filter(Objects::nonNull).distinct()
                    .map(fhirTransformService::transformToSpecimen).forEach(resources::add);
        }
        if (searchParams.hasRevInclude(FhirConstants.DIAGNOSTIC_REPORT_RESULT_REV_INCLUDE)) {
            for (Analysis analysis : analyses.values()) {
                try {
                    resources.add(fhirTransformService.transformResultToDiagnosticReport(analysis));
                } catch (FhirTransformationException e) {
                    LogEvent.logWarn(getClass().getSimpleName(), "getResources",
                            "skipping DiagnosticReport for analysis " + analysis.getId() + ": " + e.getMessage());
                }
            }
        }

        resources.subList(matchCount, resources.size()).forEach(
                resource -> ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(resource, BundleEntrySearchModeEnum.INCLUDE));
        return resources;
    }
}
