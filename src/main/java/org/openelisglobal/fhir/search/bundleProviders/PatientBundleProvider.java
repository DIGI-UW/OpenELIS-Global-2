package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.PatientSearchParams;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.search.dao.ObservationSearchDao;
import org.openelisglobal.search.dao.PatientSearchDao;
import org.openelisglobal.search.dao.ServiceRequestSearchDao;
import org.openelisglobal.search.dao.SpecimenSearchDao;
import org.openelisglobal.search.service.SampleHumanSearchDao;

/**
 * Pages OpenELIS patients as FHIR Patient resources and, on request, the
 * orders, specimens, results and reports linked to them through sample_human.
 */
public class PatientBundleProvider extends BaseFhirBundleProvider<Patient, org.hl7.fhir.r4.model.Patient> {

    private final PatientSearchParams searchParams;
    private final PatientSearchDao patientSearchDao;
    private final SampleHumanSearchDao sampleHumanSearchDao;
    private final ServiceRequestSearchDao serviceRequestSearchDao;
    private final SpecimenSearchDao specimenSearchDao;
    private final ObservationSearchDao observationSearchDao;
    private final FhirTransformService fhirTransformService;

    public PatientBundleProvider(PatientSearchParams searchParams, PatientSearchDao patientSearchDao,
            SampleHumanSearchDao sampleHumanSearchDao, ServiceRequestSearchDao serviceRequestSearchDao,
            SpecimenSearchDao specimenSearchDao, ObservationSearchDao observationSearchDao,
            FhirTransformService fhirTransformService) {

        this.searchParams = Objects.requireNonNull(searchParams, "PatientSearchParams must not be null");
        this.patientSearchDao = Objects.requireNonNull(patientSearchDao, "PatientSearchDao must not be null");
        this.sampleHumanSearchDao = Objects.requireNonNull(sampleHumanSearchDao,
                "SampleHumanSearchDao must not be null");
        this.serviceRequestSearchDao = Objects.requireNonNull(serviceRequestSearchDao,
                "ServiceRequestSearchDao must not be null");
        this.specimenSearchDao = Objects.requireNonNull(specimenSearchDao, "SpecimenSearchDao must not be null");
        this.observationSearchDao = Objects.requireNonNull(observationSearchDao,
                "ObservationSearchDao must not be null");
        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
    }

    @Override
    protected List<Patient> loadEntities(int offset, int pageSize) {
        return patientSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {
        return patientSearchDao.count(searchParams);
    }

    @Override
    protected org.hl7.fhir.r4.model.Patient transformEntity(Patient patient) {
        try {
            return fhirTransformService.transformToFhirPatient(patient.getId());
        } catch (FhirTransformationException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "transformEntity",
                    "skipping Patient " + patient.getId() + ": " + e.getMessage());
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

        List<Patient> patients = loadEntities(offset, pageSize);
        List<IBaseResource> resources = new ArrayList<>();
        patients.stream().map(this::transformEntity).filter(Objects::nonNull).forEach(resources::add);
        resources.forEach(
                resource -> ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(resource, BundleEntrySearchModeEnum.MATCH));

        if (patients.isEmpty() || searchParams.getRevIncludes().isEmpty()) {
            return resources;
        }
        int matchCount = resources.size();

        List<SampleHuman> sampleHumans = sampleHumanSearchDao.findByPatients(patients);
        if (sampleHumans.isEmpty()) {
            return resources;
        }
        List<String> sampleIds = sampleHumans.stream().map(SampleHuman::getSampleId).filter(Objects::nonNull).distinct()
                .toList();

        boolean wantsServiceRequests = searchParams.hasRevInclude(FhirConstants.SERVICE_REQUEST_PATIENT_REV_INCLUDE,
                FhirConstants.SERVICE_REQUEST_SUBJECT_REV_INCLUDE);
        boolean wantsObservations = searchParams.hasRevInclude(FhirConstants.OBSERVATION_PATIENT_REV_INCLUDE,
                FhirConstants.OBSERVATION_SUBJECT_REV_INCLUDE);
        boolean wantsReports = searchParams.hasRevInclude(FhirConstants.DIAGNOSTIC_REPORT_PATIENT_REV_INCLUDE,
                FhirConstants.DIAGNOSTIC_REPORT_SUBJECT_REV_INCLUDE);

        if (searchParams.hasRevInclude(FhirConstants.SPECIMEN_PATIENT_REV_INCLUDE,
                FhirConstants.SPECIMEN_SUBJECT_REV_INCLUDE)) {
            for (SampleItem sampleItem : specimenSearchDao.findBySampleIds(sampleIds)) {
                resources.add(fhirTransformService.transformToSpecimen(sampleItem));
            }
        }

        if (wantsServiceRequests || wantsObservations || wantsReports) {
            List<Analysis> analyses = serviceRequestSearchDao.findBySampleHumans(sampleHumans);
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
