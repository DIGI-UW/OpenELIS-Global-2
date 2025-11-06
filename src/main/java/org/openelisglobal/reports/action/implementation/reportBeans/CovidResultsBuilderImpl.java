package org.openelisglobal.reports.action.implementation.reportBeans;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.json.JSONObject;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;

public abstract class CovidResultsBuilderImpl implements CovidResultsBuilder {

    public enum CovidReportType {
        JSON, CSV
    }

    private IStatusService statusService = SpringContext.getBean(IStatusService.class);
    protected AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
    protected TestService testService = SpringContext.getBean(TestService.class);
    protected FhirContext fhirContext = SpringContext.getBean(FhirContext.class);
    protected FhirConfig fhirConfig = SpringContext.getBean(FhirConfig.class);
    protected FhirUtil fhirUtil = SpringContext.getBean(FhirUtil.class);
    protected DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    protected SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);

    private static final String[] COVID_LOINC_CODES = { "94547-7", "94500-6" };

    protected static final String DATE_PROPERTY_NAME = "date of test";
    protected static final String RESULT_PROPERTY_NAME = "result";
    protected static final String ORDER_NUMBER_PROPERTY_NAME = "order number";
    protected static final String PATIENT_LAST_NAME_PROPERTY_NAME = "family name";
    protected static final String PATIENT_FIRST_NAME_PROPERTY_NAME = "given name";
    protected static final String PATIENT_DATE_OF_BIRTH_PROPERTY_NAME = "date of birth";
    protected static final String PATIENT_PHONE_NO_PROPERTY_NAME = "phone number";
    protected static final String SAMPLE_STATUS_PROPERTY_NAME = "sample status";
    protected static final String SAMPLE_RECEIVED_DATE_PROPERTY_NAME = "sample received date";
    protected static final String SITE_PROPERTY_NAME = "site code";
    protected static final String LOCATOR_FORM_PROPERTY_NAME = "locatorForm";
    protected static final String CONTACT_TRACING_INDEX_NAME = "contact tracing index name";
    protected static final String CONTACT_TRACING_INDEX_RECORD_NUMBER = "contact tracing dossier number";

    protected static final String EMPTY_VALUE = "";

    protected final List<String> ANALYSIS_STATUS_IDS;
    protected final List<String> SAMPLE_STATUS_IDS;

    protected DateRange dateRange;

    public CovidResultsBuilderImpl(DateRange dateRange) {
        ANALYSIS_STATUS_IDS = Arrays.asList(statusService.getStatusID(AnalysisStatus.Finalized),
                statusService.getStatusID(AnalysisStatus.TechnicalAcceptance));
        SAMPLE_STATUS_IDS = Arrays.asList(statusService.getStatusID(OrderStatus.Started),
                statusService.getStatusID(OrderStatus.Finished));
        this.dateRange = dateRange;
    }

    protected List<Analysis> getCovidAnalysisWithinDate() {

        List<Test> tests = testService.getActiveTestsByLoinc(COVID_LOINC_CODES);

        List<Analysis> analysises = analysisService.getAllAnalysisByTestsAndStatusAndCompletedDateRange(
                tests.stream().map(test -> Integer.parseInt(test.getId())).collect(Collectors.toList()),
                ANALYSIS_STATUS_IDS.stream().map(val -> Integer.parseInt(val)).collect(Collectors.toList()),
                SAMPLE_STATUS_IDS.stream().map(val -> Integer.parseInt(val)).collect(Collectors.toList()),
                this.dateRange.getLowDate(), this.dateRange.getHighDate());

        return analysises;

        // return analysises.stream().filter(analysis ->
        // analysis.getStartedDate().after(this.dateRange.getLowDate())
        // &&
        // analysis.getStartedDate().before(this.dateRange.getHighDate())).collect(Collectors.toList());
    }

    protected Optional<Task> getReferringTaskForAnalysis(Analysis analysis) {
        IGenericClient client = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
        String serviceRequestId = analysis.getSampleItem().getSample().getReferringId();
        if (GenericValidator.isBlankOrNull(serviceRequestId)) {
            return Optional.empty();
        }

        ServiceRequest serviceRequest = null;
        for (String remotePath : fhirConfig.getRemoteStorePaths()) {
            Bundle responseBundle = client.search().forResource(ServiceRequest.class)
                    .where(ServiceRequest.IDENTIFIER.exactly().systemAndIdentifier(remotePath, serviceRequestId))
                    .returnBundle(Bundle.class).execute();
            for (BundleEntryComponent bundleComponent : responseBundle.getEntry()) {
                if (bundleComponent.hasResource()
                        && ResourceType.ServiceRequest.equals(bundleComponent.getResource().getResourceType())) {
                    serviceRequest = (ServiceRequest) bundleComponent.getResource();
                }
            }
        }

        if (serviceRequest == null) {
            return Optional.empty();
        }
        Bundle responseBundle = client.search().forResource(Task.class)
                .where(Task.BASED_ON.hasId(serviceRequest.getIdElement().getIdPart())).returnBundle(Bundle.class)
                .execute();
        for (BundleEntryComponent bundleComponent : responseBundle.getEntry()) {
            if (bundleComponent.hasResource()
                    && ResourceType.Task.equals(bundleComponent.getResource().getResourceType())) {
                return Optional.of((Task) bundleComponent.getResource());
            }
        }
        throw new IllegalStateException("could not find task for analysis with serviceRequestId: " + serviceRequestId);
    }

    protected Optional<Result> getResultForAnalysis(Analysis analysis) {
        List<Result> results = analysisService.getResults(analysis);
        if (!results.isEmpty()) {
            return Optional.of(results.get(0));
        }
        return Optional.empty();
    }

    protected Patient getPatientForAnalysis(Analysis analysis) {
        return sampleHumanService.getPatientForSample(analysis.getSampleItem().getSample());
    }

    protected boolean patientIsMainPatient(Patient patient, JSONObject locatorForm) {
        String patientLastName = patient.getPerson().getLastName().trim();
        String patientFirstName = patient.getPerson().getFirstName().trim();
        Date patientBirthDate = patient.getBirthDate();

        String lfPatientLastName = locatorForm.getString("lastName").trim();
        String lfPatientFirstName = locatorForm.getString("firstName").trim();
        LocalDate lfPatientBirthDate = LocalDate.parse(locatorForm.getString("dateOfBirth"));

        if (!Date.from(lfPatientBirthDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                .equals(patientBirthDate)) {
            return false;
        } else if (!lfPatientFirstName.equals(patientFirstName)) {
            return false;
        } else if (!lfPatientLastName.equals(patientLastName)) {
            return false;
        }
        return true;
    }

    protected String getResultValue(Optional<Result> result) {
        if (result.isPresent()) {
            if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(result.get().getResultType())) {
                return dictionaryService.getDataForId(result.get().getValue()).getDictEntry();
            }
        }
        return EMPTY_VALUE;
    }
}
