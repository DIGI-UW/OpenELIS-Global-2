package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.ServiceRequestSearchParams;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.dao.PractitionerSearchDao;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.openelisglobal.search.service.SampleHumanSearchDao;

/**
 * Pages OpenELIS analyses as FHIR ServiceRequest resources, adding the patient,
 * requester and specimen for {@code _include} and the results and reports for
 * {@code _revinclude=Observation:based-on} and
 * {@code _revinclude=DiagnosticReport:based-on}.
 */
public class ServiceRequestBundleProvider extends BaseFhirBundleProvider<Analysis, ServiceRequest> {

    private final ServiceRequestSearchParams searchParams;
    private final ServiceRequestSearchDao serviceRequestSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final PractitionerSearchDao practitionerSearchDao;
    private final ObservationSearchDao observationSearchDao;
    private final FhirTransformService fhirTransformService;
    private final IStatusService statusService;

    public ServiceRequestBundleProvider(ServiceRequestSearchParams searchParams,
            ServiceRequestSearchDao serviceRequestSearchDao, SampleHumanSearchDao sampleHumanSearchDao,
            PractitionerSearchDao practitionerSearchDao, ObservationSearchDao observationSearchDao,
            FhirTransformService fhirTransformService, IStatusService statusService) {
        this.searchParams = Objects.requireNonNull(searchParams, "ServiceRequestSearchParams must not be null");
        this.serviceRequestSearchDao = Objects.requireNonNull(serviceRequestSearchDao,
                "ServiceRequestSearchDao must not be null");
        this.sampleHumanSearchDao = Objects.requireNonNull(sampleHumanSearchDao,
                "SampleHumanSearchDao must not be null");
        this.practitionerSearchDao = Objects.requireNonNull(practitionerSearchDao,
                "PractitionerSearchDao must not be null");
        this.observationSearchDao = Objects.requireNonNull(observationSearchDao,
                "ObservationSearchDao must not be null");
        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
        this.statusService = Objects.requireNonNull(statusService, "IStatusService must not be null");
    }

    @Override
    protected List<Analysis> loadEntities(int offset, int pageSize) {
        return serviceRequestSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {
        return serviceRequestSearchDao.count(searchParams);
    }

    @Override
    protected ServiceRequest transformEntity(Analysis analysis) {
        return fhirTransformService.transformToServiceRequest(analysis.getId());
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
            ServiceRequest serviceRequest = transformEntity(analysis);
            if (serviceRequest != null) {
                ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(serviceRequest, BundleEntrySearchModeEnum.MATCH);
                resources.add(serviceRequest);
            }
        }
        if (analyses.isEmpty()) {
            return resources;
        }
        int matchCount = resources.size();

        boolean wantsPatient = searchParams.hasInclude(FhirConstants.SERVICE_REQUEST_PATIENT_INCLUDE,
                FhirConstants.SERVICE_REQUEST_SUBJECT_INCLUDE);
        boolean wantsRequester = searchParams.hasInclude(FhirConstants.SERVICE_REQUEST_REQUESTER_INCLUDE);
        if (wantsPatient || wantsRequester) {
            List<String> sampleIds = analyses.stream().map(Analysis::getSampleItem).filter(Objects::nonNull)
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
            if (wantsRequester) {
                for (Provider provider : practitionerSearchDao.findByIds(new ArrayList<>(providerIds))) {
                    resources.add(fhirTransformService.transformProviderToPractitioner(provider));
                }
            }
        }

        if (searchParams.hasInclude(FhirConstants.SERVICE_REQUEST_SPECIMEN_INCLUDE)) {
            analyses.stream().map(Analysis::getSampleItem).filter(Objects::nonNull)
                    .map(fhirTransformService::transformToSpecimen).forEach(resources::add);
        }

        boolean wantsObservations = searchParams.hasRevInclude(FhirConstants.OBSERVATION_BASED_ON_REV_INCLUDE);
        boolean wantsReports = searchParams.hasRevInclude(FhirConstants.DIAGNOSTIC_REPORT_BASED_ON_REV_INCLUDE);
        if (wantsObservations) {
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
        if (wantsReports) {
            for (Analysis analysis : analyses) {
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
