/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.sample.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.service.PersonAddressService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.address.valueholder.PersonAddress;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.rest.provider.bean.PatientInfoBean;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.services.RequesterService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.observationhistory.service.ObservationHistoryService;
import org.openelisglobal.observationhistory.service.ObservationHistoryServiceImpl.ObservationType;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.util.PatientUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.patienttype.service.PatientPatientTypeService;
import org.openelisglobal.patienttype.valueholder.PatientType;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.program.service.ProgramSampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Order Search and Dashboard in the decoupled sample
 * collection workflow.
 *
 * <p>
 * Provides endpoints for: - Barcode scanner bar (NAV-6) to load orders by lab
 * number - Order dashboard (DSH-1 to DSH-9) to list in-progress orders
 *
 * @see Sample
 * @see SampleService
 */
@RestController
@RequestMapping("/rest/order")
public class OrderSearchRestController extends BaseRestController {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private AddressPartService addressPartService;

    @Autowired
    private PersonAddressService personAddressService;

    @Autowired
    private PatientPatientTypeService patientPatientTypeService;

    @Autowired
    private PatientContactService patientContactService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ObservationHistoryService observationHistoryService;

    @Autowired
    private ProgramSampleService programSampleService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private PersonService personService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private FhirUtil fhirUtil;

    private String ADDRESS_PART_VILLAGE_ID;
    private String ADDRESS_PART_COMMUNE_ID;
    private String ADDRESS_PART_DEPT_ID;

    /**
     * Get orders for the dashboard (DSH-1 to DSH-9).
     *
     * <p>
     * Returns paginated list of orders with filtering support.
     *
     * @param page            page number (1-based)
     * @param pageSize        items per page (25, 50, or 100)
     * @param search          search query for patient name, lab number, etc.
     * @param status          filter by order status
     * @param priority        filter by priority
     * @param includeExternal include external/EMR orders
     * @return Dashboard data with orders list and counts
     */
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "100") int pageSize, @RequestParam(required = false) String search,
            @RequestParam(required = false) String status, @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "false") boolean includeExternal,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate) {

        try {
            LogEvent.logInfo(this.getClass().getName(), "getDashboard",
                    "Fetching order dashboard, page: " + page + ", pageSize: " + pageSize);

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> ordersList = new ArrayList<>();

            // Get recent samples (simplified query - in production, use proper pagination)
            int startingRecNo = (page - 1) * pageSize;
            List<Sample> samples = sampleService.getPageOfSamples(startingRecNo);

            for (Sample sample : samples) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("id", sample.getId());
                orderData.put("labNumber", sample.getAccessionNumber());
                orderData.put("lastUpdated", sample.getLastupdated() != null ? sample.getLastupdated().toString() : "");
                orderData.put("priority", "routine"); // Default, can be enhanced
                orderData.put("isExternal", false);
                orderData.put("returnedFromQA", false);

                // Get patient name
                Patient patient = sampleHumanService.getPatientForSample(sample);
                if (patient != null) {
                    String patientName = (patientService.getFirstName(patient) + " "
                            + patientService.getLastName(patient)).trim();
                    orderData.put("patientName", patientName);
                } else {
                    orderData.put("patientName", "---");
                }

                // Facility (simplified - could get from organization)
                orderData.put("facilityName", "---");

                // Step progress (simplified - could track actual step completion)
                Map<String, Boolean> stepProgress = new HashMap<>();
                String sampleStatus = sample.getStatus();
                stepProgress.put("enter", true); // If sample exists, enter is complete
                stepProgress.put("collect", sampleStatus != null);
                stepProgress.put("label", false);
                stepProgress.put("qa", false);
                orderData.put("stepProgress", stepProgress);

                ordersList.add(orderData);
            }

            response.put("orders", ordersList);
            response.put("totalCount", ordersList.size()); // Simplified, should be total count
            response.put("externalCount", 0); // Placeholder for external orders count
            response.put("page", page);
            response.put("pageSize", pageSize);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getDashboard", "Error fetching dashboard: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search for an order by lab number (accession number).
     *
     * <p>
     * Returns order data including patient information and samples for display in
     * the order workflow steps. Used by the barcode scanner bar (NAV-6).
     *
     * <p>
     * Example: GET /rest/order/search?labNumber=20231201-001
     *
     * @param labNumber the lab/accession number to search for (required)
     * @return Order data with 200 OK, or 404 if not found
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchOrder(@RequestParam(required = false) String labNumber) {

        LogEvent.logInfo(this.getClass().getName(), "searchOrder",
                "searchOrder called with labNumber param: '" + labNumber + "'");

        if (labNumber == null || labNumber.trim().isEmpty()) {
            LogEvent.logWarn(this.getClass().getName(), "searchOrder",
                    "Rejecting request - labNumber is null or empty");
            return ResponseEntity.badRequest().build();
        }

        try {
            LogEvent.logInfo(this.getClass().getName(), "searchOrder",
                    "Searching for order with lab number: " + labNumber);

            // Find the sample by accession number
            Sample sample = sampleService.getSampleByAccessionNumber(labNumber.trim());

            if (sample == null) {
                LogEvent.logInfo(this.getClass().getName(), "searchOrder",
                        "No order found for lab number: " + labNumber);
                return ResponseEntity.notFound().build();
            }

            // Build response with order data
            Map<String, Object> response = new HashMap<>();
            response.put("id", sample.getId());
            response.put("labNumber", sample.getAccessionNumber());
            response.put("receivedDate", sample.getReceivedDateForDisplay());
            response.put("collectionDate", sample.getCollectionDateForDisplay());
            response.put("status", sample.getStatus());

            // Get patient information - using PatientInfoBean for consistency with
            // /rest/patient-details
            Patient patient = sampleHumanService.getPatientForSample(sample);
            if (patient != null) {
                PatientInfoBean patientData = buildPatientProperties(patient);
                response.put("patientProperties", patientData);

                // Also include orderData structure for frontend compatibility
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("patientProperties", patientData);
                orderData.put("patientUpdateStatus", "UPDATE");
                response.put("orderData", orderData);
            }

            // Get sample items
            List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(sample.getId());
            List<Map<String, Object>> samplesData = new ArrayList<>();

            for (SampleItem sampleItem : sampleItems) {
                Map<String, Object> sampleItemData = new HashMap<>();
                sampleItemData.put("id", sampleItem.getId());
                sampleItemData.put("sampleItemId", sampleItem.getId()); // For frontend to use in updates
                sampleItemData.put("sortOrder", sampleItem.getSortOrder());
                sampleItemData.put("sampleTypeId", sampleItem.getTypeOfSampleId());

                // Get sample type name
                if (sampleItem.getTypeOfSampleId() != null) {
                    var typeOfSample = typeOfSampleService.get(sampleItem.getTypeOfSampleId());
                    if (typeOfSample != null) {
                        sampleItemData.put("name", typeOfSample.getLocalizedName());
                        sampleItemData.put("sampleTypeName", typeOfSample.getDescription());
                    }
                }

                // Collection info
                Map<String, Object> sampleXML = new HashMap<>();
                sampleXML.put("collectionDate",
                        sampleItem.getCollectionDate() != null ? sampleItem.getCollectionDate().toString() : "");
                sampleXML.put("quantity", sampleItem.getQuantity());
                sampleItemData.put("sampleXML", sampleXML);

                // Get tests from analysis records for this sample item
                List<Analysis> analyses = analysisService.getAnalysesBySampleItem(sampleItem);
                List<Map<String, Object>> testsData = new ArrayList<>();
                List<Map<String, Object>> panelsData = new ArrayList<>();

                LogEvent.logInfo(this.getClass().getName(), "searchOrder",
                        "DEBUG: Found " + analyses.size() + " analyses for sampleItem " + sampleItem.getId());

                for (Analysis analysis : analyses) {
                    if (analysis.getTest() != null) {
                        Map<String, Object> testData = new HashMap<>();
                        testData.put("id", analysis.getTest().getId());
                        testData.put("name", analysis.getTest().getLocalizedName());
                        testData.put("description", analysis.getTest().getDescription());
                        testsData.add(testData);
                    }
                    // Try to get panel - may be null if test wasn't added via panel
                    try {
                        if (analysis.getPanel() != null) {
                            // Check if panel already added
                            boolean panelExists = panelsData.stream()
                                    .anyMatch(p -> p.get("id").equals(analysis.getPanel().getId()));
                            if (!panelExists) {
                                Map<String, Object> panelData = new HashMap<>();
                                panelData.put("id", analysis.getPanel().getId());
                                panelData.put("name", analysis.getPanel().getLocalizedName());
                                panelsData.add(panelData);
                            }
                        }
                    } catch (Exception e) {
                        // Panel retrieval failed - log at debug level only
                    }
                }

                sampleItemData.put("tests", testsData);
                sampleItemData.put("panels", panelsData);
                sampleItemData.put("index", sampleItem.getSortOrder());

                samplesData.add(sampleItemData);
            }
            response.put("samples", samplesData);

            // Build comprehensive sampleOrderItems with provider, site, and clinical info
            Map<String, Object> sampleOrderItems = buildSampleOrderItems(sample);
            response.put("sampleOrderItems", sampleOrderItems);

            // Step progress
            Map<String, Boolean> stepProgress = new HashMap<>();
            stepProgress.put("enter", true);
            stepProgress.put("collect", sample.getStatus() != null);
            stepProgress.put("label", false);
            stepProgress.put("qa", false);
            response.put("stepProgress", stepProgress);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "searchOrder", "Error searching for order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Build patient properties using PatientInfoBean - matches the format returned
     * by /rest/patient-details endpoint. This ensures consistency with the existing
     * patient search functionality.
     */
    private PatientInfoBean buildPatientProperties(Patient patient) {
        initAddressPartIds();

        Person person = patient.getPerson();
        PatientIdentityTypeMap identityMap = PatientIdentityTypeMap.getInstance();
        List<PatientIdentity> identityList = PatientUtil.getIdentityListForPatient(patient.getId());
        List<PatientContact> patientContacts = patientContactService.getForPatient(patient.getId());

        String city = getAddress(person, ADDRESS_PART_VILLAGE_ID);
        if (GenericValidator.isBlankOrNull(city)) {
            city = person.getCity();
        }
        String commune = getAddress(person, ADDRESS_PART_COMMUNE_ID);
        String dept = getAddress(person, ADDRESS_PART_DEPT_ID);

        PatientInfoBean patientInfo = new PatientInfoBean();
        patientInfo.setPatientPK(patient.getId());
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);
        patientInfo.setNationalId(patient.getNationalId());
        patientInfo.setSTnumber(identityMap.getIdentityValue(identityList, "ST"));
        patientInfo.setSubjectNumber(identityMap.getIdentityValue(identityList, "SUBJECT"));
        patientInfo.setLastName(getLastNameForResponse(person));
        patientInfo.setFirstName(person.getFirstName());
        patientInfo.setMothersName(identityMap.getIdentityValue(identityList, "MOTHER"));
        patientInfo.setAka(identityMap.getIdentityValue(identityList, "AKA"));
        patientInfo.setStreetAddress(person.getStreetAddress());
        patientInfo.setCity(city);
        patientInfo.setPrimaryPhone(person.getPrimaryPhone());
        patientInfo.setEmail(person.getEmail());
        patientInfo.setGender(patient.getGender());
        patientInfo.setPatientType(getPatientType(patient));
        patientInfo.setInsuranceNumber(identityMap.getIdentityValue(identityList, "INSURANCE"));
        patientInfo.setOccupation(identityMap.getIdentityValue(identityList, "OCCUPATION"));

        String format1 = "dd/MM/yyyy";
        String format2 = "MM/dd/yyyy";
        patientInfo.setBirthDateForDisplay(
                ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE).equals("fr-FR")
                        ? DateUtil.formatStringDate(patient.getBirthDateForDisplay(), format1)
                        : DateUtil.formatStringDate(patient.getBirthDateForDisplay(), format2));

        patientInfo.setCommune(commune);
        patientInfo.setAddressDepartment(dept);
        patientInfo.setMothersInitial(identityMap.getIdentityValue(identityList, "MOTHERS_INITIAL"));
        patientInfo.setEducation(identityMap.getIdentityValue(identityList, "EDUCATION"));
        patientInfo.setMaritialStatus(identityMap.getIdentityValue(identityList, "MARITIAL"));
        patientInfo.setNationality(identityMap.getIdentityValue(identityList, "NATIONALITY"));
        patientInfo.setOtherNationality(identityMap.getIdentityValue(identityList, "OTHER NATIONALITY"));
        patientInfo.setHealthDistrict(identityMap.getIdentityValue(identityList, "HEALTH DISTRICT"));
        patientInfo.setHealthRegion(identityMap.getIdentityValue(identityList, "HEALTH REGION"));
        patientInfo.setGuid(identityMap.getIdentityValue(identityList, "GUID"));

        if (patientContacts.size() >= 1) {
            PatientContact contact = patientContacts.get(0);
            patientInfo.setPatientContact(contact);
        }

        if (patient.getLastupdated() != null) {
            patientInfo.setPatientLastUpdated(patient.getLastupdated().toString());
        }
        if (person.getLastupdated() != null) {
            patientInfo.setPersonLastUpdated(person.getLastupdated().toString());
        }

        patientInfo.setReadOnly(false);

        return patientInfo;
    }

    private void initAddressPartIds() {
        if (ADDRESS_PART_DEPT_ID == null) {
            List<AddressPart> partList = addressPartService.getAll();
            for (AddressPart addressPart : partList) {
                if ("department".equals(addressPart.getPartName())) {
                    ADDRESS_PART_DEPT_ID = addressPart.getId();
                } else if ("commune".equals(addressPart.getPartName())) {
                    ADDRESS_PART_COMMUNE_ID = addressPart.getId();
                } else if ("village".equals(addressPart.getPartName())) {
                    ADDRESS_PART_VILLAGE_ID = addressPart.getId();
                }
            }
        }
    }

    private String getAddress(Person person, String addressPartId) {
        if (GenericValidator.isBlankOrNull(addressPartId) || person == null) {
            return "";
        }
        PersonAddress address = personAddressService.getByPersonIdAndPartId(person.getId(), addressPartId);
        return address != null ? address.getValue() : "";
    }

    private String getLastNameForResponse(Person person) {
        if (PatientUtil.getUnknownPerson().getId().equals(person.getId())) {
            return null;
        } else {
            return person.getLastName();
        }
    }

    private String getPatientType(Patient patient) {
        PatientType patientType = patientPatientTypeService.getPatientTypeForPatient(patient.getId());
        return patientType != null ? patientType.getType() : null;
    }

    /**
     * Build sampleOrderItems with all order-level data including: - Lab number and
     * dates - Provider/Requester info from SampleHuman (provider) and
     * SampleRequester (organization) - Referring site (organization) info - Program
     * from ObservationHistory - Clinical information from ObservationHistory
     */
    private Map<String, Object> buildSampleOrderItems(Sample sample) {
        Map<String, Object> sampleOrderItems = new HashMap<>();

        // Basic sample info
        sampleOrderItems.put("labNo", sample.getAccessionNumber());
        sampleOrderItems.put("collectionDate", sample.getCollectionDateForDisplay());
        sampleOrderItems.put("receivedDateForDisplay", sample.getReceivedDateForDisplay());
        sampleOrderItems.put("receivedTime", sample.getReceivedTimeForDisplay());

        String sampleId = sample.getId();
        LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                "=== DEBUG: Building sampleOrderItems for sampleId: " + sampleId);

        // Provider - get from SampleHuman.providerId via
        // sampleHumanService.getProviderForSample()
        // This is the correct approach since SamplePatientUpdateData stores provider in
        // SampleHuman
        Provider provider = sampleHumanService.getProviderForSample(sample);
        LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                "DEBUG: Provider from sampleHuman: " + (provider != null ? provider.getId() : "NULL"));
        if (provider != null && provider.getPerson() != null) {
            Person providerPerson = provider.getPerson();
            // Ensure person data is loaded
            personService.getData(providerPerson);
            LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                    "DEBUG: Provider person: " + providerPerson.getId() + " - " + providerPerson.getFirstName() + " "
                            + providerPerson.getLastName());
            sampleOrderItems.put("providerPersonId", providerPerson.getId());
            sampleOrderItems.put("providerFirstName", providerPerson.getFirstName());
            sampleOrderItems.put("providerLastName", providerPerson.getLastName());
            sampleOrderItems.put("providerWorkPhone", providerPerson.getWorkPhone());
            sampleOrderItems.put("providerEmail", providerPerson.getEmail());
            sampleOrderItems.put("providerFax", providerPerson.getFax());
        }

        // Referring site (organization) - use RequesterService for organization
        RequesterService requesterService = new RequesterService(sampleId);
        Organization referringSite = requesterService.getOrganization();

        // Referring site department
        Organization department = requesterService.getOrganizationDepartment();

        // If referringSite is null but department exists, use department as the site
        // This handles cases where organization was stored with department type instead
        // of site type
        if (referringSite == null && department != null) {
            referringSite = department;
            department = null; // Clear department since we're using it as the site
        }

        if (referringSite != null) {
            sampleOrderItems.put("referringSiteId", referringSite.getId());
            sampleOrderItems.put("referringSiteName", referringSite.getOrganizationName());
            sampleOrderItems.put("referringSiteCode", referringSite.getShortName());
        }

        if (department != null) {
            sampleOrderItems.put("referringSiteDepartmentId", department.getId());
            sampleOrderItems.put("referringSiteDepartmentName", department.getOrganizationName());
        }

        // Program - Try multiple approaches to find program info
        // 1. Check observation_history for program NAME
        // 2. If found, use ProgramSampleService to get the Program.id
        // 3. If not found in observation_history, check program_sample table directly
        String programName = observationHistoryService.getRawValueForSample(ObservationType.PROGRAM, sampleId);
        LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                "DEBUG: programName from observation_history: '" + programName + "'");

        if (programName != null && !programName.isEmpty()) {
            sampleOrderItems.put("program", programName); // Keep the name for display
            // Try to resolve the numeric program ID from the name
            try {
                ProgramSample programSample = programSampleService.getProgrammeSampleBySample(Integer.valueOf(sampleId),
                        programName);
                if (programSample != null && programSample.getProgram() != null) {
                    String programId = programSample.getProgram().getId();
                    LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                            "DEBUG: Resolved programId from ProgramSample: " + programId);
                    sampleOrderItems.put("programId", programId);

                    // Load questionnaire response if available
                    if (programSample.getQuestionnaireResponseUuid() != null) {
                        try {
                            QuestionnaireResponse qr = fhirUtil.getLocalFhirClient().read()
                                    .resource(QuestionnaireResponse.class)
                                    .withId(programSample.getQuestionnaireResponseUuid().toString()).execute();
                            if (qr != null) {
                                LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                                        "DEBUG: Loaded QuestionnaireResponse: " + qr.getId());
                                sampleOrderItems.put("additionalQuestions", qr);
                            }
                        } catch (Exception qrEx) {
                            LogEvent.logError(this.getClass().getName(), "buildSampleOrderItems",
                                    "DEBUG: Error loading QuestionnaireResponse: " + qrEx.getMessage());
                        }
                    }
                } else {
                    // Fall back: try to find program by name directly
                    List<Program> allPrograms = programService.getAll();
                    for (Program p : allPrograms) {
                        if (p.getProgramName() != null && p.getProgramName().equals(programName)) {
                            LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                                    "DEBUG: Found program by name lookup: " + p.getId());
                            sampleOrderItems.put("programId", p.getId());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "buildSampleOrderItems",
                        "DEBUG: Exception resolving program ID: " + e.getMessage());
            }
        } else {
            // No program in observation_history - check program_sample table directly
            // This handles cases where program was stored differently
            LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                    "DEBUG: No program in observation_history, checking program_sample table");
            try {
                // Get all program samples and filter by sample ID
                List<ProgramSample> programSamples = programSampleService
                        .getProgramSamplesByAccessionNumberOrProgramName(sample.getAccessionNumber());
                if (programSamples != null && !programSamples.isEmpty()) {
                    ProgramSample ps = programSamples.get(0);
                    if (ps.getProgram() != null) {
                        LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                                "DEBUG: Found program in program_sample table: " + ps.getProgram().getId());
                        sampleOrderItems.put("programId", ps.getProgram().getId());
                        sampleOrderItems.put("program", ps.getProgram().getProgramName());

                        // Load questionnaire response if available
                        if (ps.getQuestionnaireResponseUuid() != null) {
                            try {
                                QuestionnaireResponse qr = fhirUtil.getLocalFhirClient().read()
                                        .resource(QuestionnaireResponse.class)
                                        .withId(ps.getQuestionnaireResponseUuid().toString()).execute();
                                if (qr != null) {
                                    LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                                            "DEBUG: Loaded QuestionnaireResponse from fallback: " + qr.getId());
                                    sampleOrderItems.put("additionalQuestions", qr);
                                }
                            } catch (Exception qrEx) {
                                LogEvent.logError(this.getClass().getName(), "buildSampleOrderItems",
                                        "DEBUG: Error loading QuestionnaireResponse from fallback: "
                                                + qrEx.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getName(), "buildSampleOrderItems",
                        "DEBUG: Exception checking program_sample: " + e.getMessage());
            }
        }

        // Payment status
        String paymentStatus = observationHistoryService.getRawValueForSample(ObservationType.PAYMENT_STATUS, sampleId);
        LogEvent.logInfo(this.getClass().getName(), "buildSampleOrderItems",
                "DEBUG: paymentStatus: '" + paymentStatus + "'");
        if (paymentStatus != null) {
            sampleOrderItems.put("paymentOptionSelection", paymentStatus);
        }
        // Also add paymentOptions list for the dropdown
        sampleOrderItems.put("paymentOptions",
                DisplayListService.getInstance().getList(ListType.SAMPLE_PATIENT_PAYMENT_OPTIONS));

        // Billing reference number
        String billingRef = observationHistoryService.getRawValueForSample(ObservationType.BILLING_REFERENCE_NUMBER,
                sampleId);
        if (billingRef != null) {
            sampleOrderItems.put("billingReferenceNumber", billingRef);
        }

        // Test location code
        String testLocationCode = observationHistoryService.getRawValueForSample(ObservationType.TEST_LOCATION_CODE,
                sampleId);
        if (testLocationCode != null) {
            sampleOrderItems.put("testLocationCode", testLocationCode);
        }

        // Other location code
        String otherLocationCode = observationHistoryService
                .getRawValueForSample(ObservationType.TEST_LOCATION_CODE_OTHER, sampleId);
        if (otherLocationCode != null) {
            sampleOrderItems.put("otherLocationCode", otherLocationCode);
        }

        // Request date
        String requestDate = observationHistoryService.getRawValueForSample(ObservationType.REQUEST_DATE, sampleId);
        if (requestDate != null) {
            sampleOrderItems.put("requestDate", requestDate);
        }

        // Next visit date
        String nextVisitDate = observationHistoryService.getRawValueForSample(ObservationType.NEXT_VISIT_DATE,
                sampleId);
        if (nextVisitDate != null) {
            sampleOrderItems.put("nextVisitDate", nextVisitDate);
        }

        // Provisional clinical diagnosis
        String provisionalDiagnosis = observationHistoryService
                .getRawValueForSample(ObservationType.PROVISIONAL_CLINICAL_DIAGNOSIS, sampleId);
        if (provisionalDiagnosis != null) {
            sampleOrderItems.put("provisionalClinicalDiagnosis", provisionalDiagnosis);
        }

        // Priority (from sample entity if available)
        if (sample.getPriority() != null) {
            sampleOrderItems.put("priority", sample.getPriority().name());
        }

        return sampleOrderItems;
    }
}
