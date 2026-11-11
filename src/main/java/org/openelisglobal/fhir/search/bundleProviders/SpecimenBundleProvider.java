package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Specimen;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.SpecimenSearchParams;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.openelisglobal.search.dao.SpecimenSearchDao;
import org.openelisglobal.search.service.SampleHumanSearchDao;

/**
 * Pages OpenELIS sample items as FHIR Specimen resources, adding the patient
 * for {@code _include=Specimen:patient|subject} and the orders and results that
 * used the specimen for {@code _revinclude=ServiceRequest:specimen} and
 * {@code _revinclude=Observation:specimen}.
 */
public class SpecimenBundleProvider extends BaseFhirBundleProvider<SampleItem, Specimen> {

    private final SpecimenSearchParams searchParams;
    private final SpecimenSearchDao specimenSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final ServiceRequestSearchDao serviceRequestSearchDao;
    private final ObservationSearchDao observationSearchDao;
    private final FhirTransformService fhirTransformService;

    public SpecimenBundleProvider(SpecimenSearchParams searchParams, SpecimenSearchDao specimenSearchDao,
            SampleHumanSearchDao sampleHumanSearchDao, ServiceRequestSearchDao serviceRequestSearchDao,
            ObservationSearchDao observationSearchDao, FhirTransformService fhirTransformService) {
        this.searchParams = Objects.requireNonNull(searchParams, "SpecimenSearchParams must not be null");
        this.specimenSearchDao = Objects.requireNonNull(specimenSearchDao, "SpecimenSearchDao must not be null");
        this.sampleHumanSearchDao = Objects.requireNonNull(sampleHumanSearchDao,
                "SampleHumanSearchDao must not be null");
        this.serviceRequestSearchDao = Objects.requireNonNull(serviceRequestSearchDao,
                "ServiceRequestSearchDao must not be null");
        this.observationSearchDao = Objects.requireNonNull(observationSearchDao,
                "ObservationSearchDao must not be null");
        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
    }

    @Override
    protected List<SampleItem> loadEntities(int offset, int pageSize) {
        return specimenSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {
        return specimenSearchDao.count(searchParams);
    }

    @Override
    protected Specimen transformEntity(SampleItem sampleItem) {
        return fhirTransformService.transformToSpecimen(sampleItem);
    }

    @Override
    public List<IBaseResource> getResources(int fromIndex, int toIndex) {

        int offset = effectiveOffset(fromIndex);
        int pageSize = effectivePageSize(fromIndex, toIndex);
        if (pageSize <= 0) {
            return List.of();
        }

        List<SampleItem> sampleItems = loadEntities(offset, pageSize);
        List<IBaseResource> resources = new ArrayList<>();
        for (SampleItem sampleItem : sampleItems) {
            Specimen specimen = transformEntity(sampleItem);
            ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(specimen, BundleEntrySearchModeEnum.MATCH);
            resources.add(specimen);
        }
        if (sampleItems.isEmpty()) {
            return resources;
        }
        int matchCount = resources.size();

        if (searchParams.hasInclude(FhirConstants.SPECIMEN_PATIENT_INCLUDE, FhirConstants.SPECIMEN_SUBJECT_INCLUDE)) {
            List<String> sampleIds = sampleItems.stream().map(item -> item.getSample().getId()).distinct().toList();
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

        boolean wantsServiceRequests = searchParams.hasRevInclude(FhirConstants.SERVICE_REQUEST_SPECIMEN_REV_INCLUDE);
        boolean wantsObservations = searchParams.hasRevInclude(FhirConstants.OBSERVATION_SPECIMEN_REV_INCLUDE);
        boolean wantsReports = searchParams.hasRevInclude(FhirConstants.DIAGNOSTIC_REPORT_SPECIMEN_REV_INCLUDE);
        if (wantsServiceRequests || wantsObservations || wantsReports) {
            List<String> sampleItemIds = sampleItems.stream().map(SampleItem::getId).toList();
            List<Analysis> analyses = serviceRequestSearchDao.findBySampleItemIds(sampleItemIds);
            if (wantsServiceRequests) {
                analyses.stream().map(analysis -> fhirTransformService.transformToServiceRequest(analysis.getId()))
                        .filter(Objects::nonNull).forEach(resources::add);
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
        }

        resources.subList(matchCount, resources.size()).forEach(
                resource -> ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(resource, BundleEntrySearchModeEnum.INCLUDE));
        return resources;
    }
}
