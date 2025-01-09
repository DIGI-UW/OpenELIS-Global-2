package org.openelisglobal.common.rest.provider;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.param.StringOrListParam;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.*;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.provider.query.PatientSearchResultsForm;
import org.openelisglobal.common.provider.query.workerObjects.PatientSearchLocalAndExternalWorker;
import org.openelisglobal.common.provider.query.workerObjects.PatientSearchLocalWorker;
import org.openelisglobal.common.provider.query.workerObjects.PatientSearchWorker;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.util.PatientSearchResultsPaging;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/")
public class PatientSearchRestController extends BaseRestController {

    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    SampleService sampleService;
    @Autowired
    PatientService patientService;
    @Autowired
    PersonService personService;
    @Autowired
    ObservationHistoryService observationHistoryService;
    @Autowired
    SampleHumanService sampleHumanService;
    @Autowired
    SearchResultsService searchResultsService;

    StringOrListParam targetSystemsParam;

    private static final Logger log = LoggerFactory.getLogger(PatientSearchRestController.class);

    @GetMapping(value = "patient-search-results", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PatientSearchResultsForm getPatientResults(HttpServletRequest request,
            @RequestParam(required = false) String lastName, @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String STNumber, @RequestParam(required = false) String subjectNumber,
            @RequestParam(required = false) String nationalID, @RequestParam(required = false) String guid,
            @RequestParam(required = false) String labNumber, @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String suppressExternalSearch)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        PatientSearchResultsPaging paging = new PatientSearchResultsPaging();
        PatientSearchResultsForm form = new PatientSearchResultsForm();

        String requestedPage = request.getParameter("page");
        if (GenericValidator.isBlankOrNull(requestedPage)) {
            List<PatientSearchResults> results = new ArrayList<>();
            if (!GenericValidator.isBlankOrNull(labNumber)) {
                Patient patient = getPatientForLabNumber(labNumber);
                if (patient == null || GenericValidator.isBlankOrNull(patient.getId())) {
                    form.setPatientSearchResults(results);
                    return form;
                } else {
                    PatientSearchResults searchResult = getSearchResultsForPatient(patient, null);
                    searchResult.setDataSourceName(MessageUtil.getMessage("patient.local.source"));
                    results.add(searchResult);
                }
            } else {
                PatientSearchWorker worker = getAppropriateWorker(request, "true".equals(suppressExternalSearch));
                if (worker != null) {
                    results = worker.getPatientSearchResults(lastName, firstName, STNumber, subjectNumber, nationalID,
                            null, guid, dateOfBirth, gender);
                } else {
                    form.setPatientSearchResults(results);
                    return form;
                }
            }

            if (ConfigurationProperties.getInstance().getPropertyValue(Property.ENABLE_CLIENT_REGISTRY)
                    .equals("true")) {
                String crSearchParam = request.getParameter("crSearch");
                if (crSearchParam != null && crSearchParam.contains("true")) {
                    List<PatientSearchResults> fhirResults = searchPatientInClientRegistry(lastName, firstName,
                            STNumber, subjectNumber, nationalID, null, guid, dateOfBirth, gender);
                    LogEvent.logWarn("PatientSearchRestController", "getPatientResults()",
                            "final results have been added");
                    results = fhirResults;
                }
            }
            paging.setDatabaseResults(request, form, results);
        } else {
            int requestedPageNumber = Integer.parseInt(requestedPage);
            paging.page(request, form, requestedPageNumber);
        }
        return form;
    }

    private Patient getPatientForLabNumber(String labNumber) {

        Sample sample = sampleService.getSampleByAccessionNumber(labNumber);

        if (sample != null && !GenericValidator.isBlankOrNull(sample.getId())) {
            return sampleHumanService.getPatientForSample(sample);
        }

        return new Patient();
    }

    private PatientSearchResults getSearchResultsForPatient(Patient patient, String referringSitePatientId) {
        personService.getData(patient.getPerson());
        return new PatientSearchResults(BigDecimal.valueOf(Long.parseLong(patient.getId())),
                patientService.getFirstName(patient), patientService.getLastName(patient),
                patientService.getGender(patient), patientService.getEnteredDOB(patient),
                patientService.getNationalId(patient), patient.getExternalId(), patientService.getSTNumber(patient),
                patientService.getSubjectNumber(patient), patientService.getGUID(patient),
                referringSitePatientId != null ? referringSitePatientId
                        : observationHistoryService.getMostRecentValueForPatient(ObservationType.REFERRERS_PATIENT_ID,
                                patientService.getPatientId(patient)));
    }

    private PatientSearchWorker getAppropriateWorker(HttpServletRequest request, boolean suppressExternalSearch) {

        if (ConfigurationProperties.getInstance().isCaseInsensitivePropertyValueEqual(Property.UseExternalPatientInfo,
                "false") || suppressExternalSearch) {
            return new PatientSearchLocalWorker();
        } else {
            return new PatientSearchLocalAndExternalWorker(getSysUserId(request));
        }
    }

    @GetMapping("/patient-search")
    public @ResponseBody List<PatientSearchResults> getSearchResults(@RequestParam(required = false) String lastName,
            @RequestParam(required = false) String firstName, @RequestParam(required = false) String STNumber,
            @RequestParam(required = false) String subjectNumber, @RequestParam(required = false) String nationalID,
            @RequestParam(required = false) String externalID, @RequestParam(required = false) String patientID,
            @RequestParam(required = false) String guid, @RequestParam(required = false) String dateOfBirth,
            @RequestParam(required = false) String gender) {
        return searchResultsService.getSearchResults(lastName, firstName, STNumber, subjectNumber, nationalID,
                externalID, patientID, guid, dateOfBirth, gender);
    }

    private List<PatientSearchResults> searchPatientInClientRegistry(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String patientID, String guid, String dateOfBirth, String gender) {
        LogEvent.logWarn("PatientSearchRestController", "searchPatientInClientRegistry()",
                "searchPatientInClientRegistry method has been reached");
        if (isClientRegistryConfigInvalid()) {
            return new ArrayList<>();
        }

        IGenericClient clientRegistry = fhirUtil.getFhirClient(fhirConfig.getClientRegistryServerUrl(),
                fhirConfig.getClientRegistryUserName(), fhirConfig.getClientRegistryPassword());
        LogEvent.logWarn("PatientSearchRestController", "searchPatientInClientRegistry()",
                "ClientRegistry connected successfully");

        IQuery<IBaseBundle> query = buildPatientSearchQuery(clientRegistry, lastName, firstName, STNumber,
                subjectNumber, nationalID, patientID, guid, dateOfBirth, gender);

        Bundle bundle = query.returnBundle(Bundle.class).execute();
        List<org.hl7.fhir.r4.model.Patient> externalPatients = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof org.hl7.fhir.r4.model.Patient) {
                    externalPatients.add((org.hl7.fhir.r4.model.Patient) entry.getResource());
                    LogEvent.logWarn("PatientSearchRestController", "searchPatientInClientRegistry()",
                            "externalPatients have been added");
                }
            }
        }

        List<PatientSearchResults> finalResults = new ArrayList<>();
        for (org.hl7.fhir.r4.model.Patient externalPatient : externalPatients) {
            // convert fhir object to patient search result
            PatientSearchResults transformedPatientSearchResult = SpringContext.getBean(FhirTransformService.class)
                    .transformToOpenElisPatientSearchResults(externalPatient);

            // Check for null NationalId and generate if needed
            if (transformedPatientSearchResult.getNationalId() == null
                    || transformedPatientSearchResult.getNationalId().isEmpty()) {
                String nationalId = generateDynamicID(transformedPatientSearchResult);
                log.info("dynamic national id: {}", nationalId);
                transformedPatientSearchResult.setNationalId(nationalId);
            }

            // Skip this patient if it's already in the local database
            if (!isPatientDuplicate(transformedPatientSearchResult)) {
                transformedPatientSearchResult.setDataSourceName(MessageUtil.getMessage("patient.cr.source"));
                finalResults.add(transformedPatientSearchResult);
            } else {
                LogEvent.logInfo("PatientSearchRestController", "searchPatientInClientRegistry",
                        String.format("Skipped duplicate patient with NationalId: %s, Name: %s %s",
                                transformedPatientSearchResult.getNationalId(),
                                transformedPatientSearchResult.getFirstName(),
                                transformedPatientSearchResult.getLastName()));
            }
        }

        return finalResults;
    }

    // FIXME: get better fallback initials and gender
    private static String generateDynamicID(PatientSearchResults transformedPatientSearchResult) {
        String genderOfTransformedPatient = transformedPatientSearchResult.getGender() != null
                && !transformedPatientSearchResult.getGender().isEmpty()
                        ? transformedPatientSearchResult.getGender().toUpperCase()
                        : "UNK";

        String initials = getInitials(transformedPatientSearchResult);

        String formattedDob = "00000000";
        if (transformedPatientSearchResult.getBirthdate() != null
                && !transformedPatientSearchResult.getBirthdate().isEmpty()) {
            try {
                // Try to parse with "dd/MM/yyyy" format first
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate birthdate = LocalDate.parse(transformedPatientSearchResult.getBirthdate(), dateFormatter);
                formattedDob = birthdate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (DateTimeParseException e1) {
                try {
                    LocalDate birthdate = LocalDate.parse(transformedPatientSearchResult.getBirthdate(),
                            DateTimeFormatter.ISO_DATE);
                    formattedDob = birthdate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                } catch (DateTimeParseException e2) {
                    LogEvent.logError(e2);
                }
            }
        }

        return String.format("NID-%s-%s-%s", genderOfTransformedPatient, formattedDob, initials);
    }

    private static String getInitials(PatientSearchResults transformedPatientSearchResult) {
        String initials = "";
        if (transformedPatientSearchResult.getFirstName() != null
                && !transformedPatientSearchResult.getFirstName().isEmpty()) {
            initials = transformedPatientSearchResult.getFirstName().substring(0, 1).toUpperCase();
        }
        if (transformedPatientSearchResult.getLastName() != null
                && !transformedPatientSearchResult.getLastName().isEmpty()) {
            initials += transformedPatientSearchResult.getLastName().substring(0, 1).toUpperCase();
        }

        if (initials.isEmpty()) {
            initials = "NAN";
        }
        return initials;
    }

    private boolean isClientRegistryConfigInvalid() {
        return GenericValidator.isBlankOrNull(fhirConfig.getClientRegistryServerUrl())
                || GenericValidator.isBlankOrNull(fhirConfig.getClientRegistryUserName())
                || GenericValidator.isBlankOrNull(fhirConfig.getClientRegistryPassword());
    }

    public IQuery<IBaseBundle> buildPatientSearchQuery(IGenericClient clientRegistry, String lastName, String firstName,
            String STNumber, String subjectNumber, String nationalID, String patientID, String guid, String dateOfBirth,
            String gender) {
        IQuery<IBaseBundle> query = clientRegistry.search().forResource(org.hl7.fhir.r4.model.Patient.class);
        if (!GenericValidator.isBlankOrNull(lastName)) {
            query = query.where(org.hl7.fhir.r4.model.Patient.FAMILY.matches().value(lastName));
            LogEvent.logWarn("PatientSearchRestController", "buildPatientSearchQuery()", "lastname added to query");
        }

        if (!GenericValidator.isBlankOrNull(firstName)) {
            query = query.where(org.hl7.fhir.r4.model.Patient.GIVEN.matches().value(firstName));
            LogEvent.logWarn("PatientSearchRestController", "buildPatientSearchQuery()", "first name added to query");
        }

        // Map for identifier-based queries (STNumber, SubjectNumber, NationalId,
        // PatientID, GUID)
        Map<String, String> identifierMappings = new HashMap<>();

        // Add non-null identifiers to the map
        if (STNumber != null) {
            identifierMappings.put("stnumber", STNumber);
        }
        if (subjectNumber != null) {
            identifierMappings.put("subjectnumber", subjectNumber);
        }
        if (nationalID != null) {
            identifierMappings.put("nationalid", nationalID);
        }
        if (patientID != null) {
            identifierMappings.put("patientid", patientID);
        }
        if (guid != null) {
            identifierMappings.put("guid", guid);
        }

        // Loop through identifiers and add them to the query
        for (Map.Entry<String, String> entry : identifierMappings.entrySet()) {
            String value = entry.getValue();
            if (!GenericValidator.isBlankOrNull(value)) {
                query = query.where(org.hl7.fhir.r4.model.Patient.IDENTIFIER.exactly()
                        .systemAndCode("http://openelis-global.org/pat_" + entry.getKey(), value));
                LogEvent.logInfo("PatientSearchRestController", "buildPatientSearchQuery()",
                        "Added identifier (" + entry.getKey() + ") to query.");
            }
        }

        if (!GenericValidator.isBlankOrNull(dateOfBirth)) {
            query = query.where(org.hl7.fhir.r4.model.Patient.BIRTHDATE.exactly().day(dateOfBirth));
            LogEvent.logWarn("PatientSearchRestController", "buildPatientSearchQuery()", "dob added to query");
        }

        if (!GenericValidator.isBlankOrNull(gender)) {
            query = query.where(org.hl7.fhir.r4.model.Patient.GENDER.exactly().code(gender));
            LogEvent.logWarn("PatientSearchRestController", "buildPatientSearchQuery()", "gender added to query");
        }

        return query;
    }

    private boolean isPatientDuplicate(PatientSearchResults externalPatient) {
        List<PatientSearchResults> localResults = searchResultsService.getSearchResults(externalPatient.getLastName(),
                externalPatient.getFirstName(), null, null, externalPatient.getNationalId(), null, null, null, null,
                null);
        for (PatientSearchResults localPatient : localResults) {
            if (isMatchingPatient(localPatient, externalPatient)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMatchingPatient(PatientSearchResults local, PatientSearchResults external) {
        boolean nationalIdMatch = (local.getNationalId() != null && external.getNationalId() != null)
                && local.getNationalId().equalsIgnoreCase(external.getNationalId());

        boolean firstNameMatch = (local.getFirstName() != null && external.getFirstName() != null)
                && local.getFirstName().equalsIgnoreCase(external.getFirstName());

        boolean lastNameMatch = (local.getLastName() != null && external.getLastName() != null)
                && local.getLastName().equalsIgnoreCase(external.getLastName());

        return nationalIdMatch && firstNameMatch && lastNameMatch;
    }
}
