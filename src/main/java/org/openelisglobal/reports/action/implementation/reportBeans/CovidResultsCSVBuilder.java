package org.openelisglobal.reports.action.implementation.reportBeans;

import ca.uhn.hl7v2.hoh.util.repackage.Base64.Charsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Task;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
import org.openelisglobal.sample.valueholder.SampleAdditionalField.AdditionalFieldName;
import org.openelisglobal.spring.util.SpringContext;

public class CovidResultsCSVBuilder extends CovidResultsBuilderImpl {

    private List<String> columnNames = new ArrayList<>();
    private List<Map<String, String>> valueRows = new ArrayList<>();
    private List<Map<Integer, String>> descriptionFieldOrder = new ArrayList<>();
    private List<Map<Integer, String>> addressFieldOrder = new ArrayList<>();
    private List<Map<Integer, String>> emergencyContactFieldOrder = new ArrayList<>();
    private List<Map<Integer, String>> travelCompanionFieldOrder = new ArrayList<>();

    SampleService sampleService = SpringContext.getBean(SampleService.class);

    public CovidResultsCSVBuilder(DateRange dateRange) {
        super(dateRange);
    }

    @Override
    public void buildDataSource() {

        createBaseColumnNames();
        populateFieldOrder();

        List<Analysis> analysises = getCovidAnalysisWithinDate();
        for (Analysis analysis : analysises) {
            addValueRow(analysis);
        }
    }

    private void createBaseColumnNames() {
        columnNames.add(ORDER_NUMBER_PROPERTY_NAME);
        columnNames.add(SITE_PROPERTY_NAME);
        columnNames.add(SAMPLE_RECEIVED_DATE_PROPERTY_NAME);
        columnNames.add(DATE_PROPERTY_NAME);
        columnNames.add(RESULT_PROPERTY_NAME);

        columnNames.add(PATIENT_LAST_NAME_PROPERTY_NAME);
        columnNames.add(PATIENT_FIRST_NAME_PROPERTY_NAME);
        columnNames.add(PATIENT_DATE_OF_BIRTH_PROPERTY_NAME);
        columnNames.add(PATIENT_PHONE_NO_PROPERTY_NAME);

        columnNames.add(SAMPLE_STATUS_PROPERTY_NAME);
        columnNames.add(CONTACT_TRACING_INDEX_NAME);
        columnNames.add(CONTACT_TRACING_INDEX_RECORD_NUMBER);
    }

    private void populateFieldOrder() {
        Map<Integer, String> row = new HashMap<>();
        row.put(0, "lastName");
        row.put(1, "firstName");
        row.put(0, "lastName");
        row.put(1, "firstName");
        row.put(2, "middleInitial");
        row.put(3, "seatNumber");
        row.put(4, "dateOfBirth");
        row.put(5, "sex");
        row.put(6, "nationality");
        row.put(7, "passportNumber");
        row.put(8, "travellerType");

        row.put(9, "airlineName");
        row.put(10, "flightNumber");
        row.put(11, "arrivalDate");
        row.put(12, "title");
        row.put(13, "lengthOfStay");
        row.put(14, "countriesVisited");
        row.put(15, "portOfEmbarkation");

        row.put(16, "fever");
        row.put(17, "soreThroat");
        row.put(18, "jointPain");
        row.put(19, "cough");
        row.put(20, "breathingDifficulties");
        row.put(21, "rash");
        row.put(22, "hadCovidBefore");

        row.put(23, "visitPurpose");
        row.put(24, "mobilePhone");
        row.put(25, "fixedPhone");
        row.put(26, "email");
        row.put(27, "nationalID");

        descriptionFieldOrder.add(row);

        Map<Integer, String> row1 = new HashMap<>();
        row1.put(0, "country");
        row1.put(1, "zipPostalCode");
        row1.put(2, "city");
        row1.put(3, "numberAndStreet");
        row1.put(4, "stateProvince");
        row1.put(5, "hotelName");
        row1.put(6, "apartmentNumber");
        row1.put(7, "permanentAddress");
        row1.put(8, "temporaryAddress");
        addressFieldOrder.add(row1);

        Map<Integer, String> row2 = new HashMap<>();
        row2.put(0, "lastName");
        row2.put(1, "firstName");
        row2.put(2, "country");
        row2.put(3, "address");
        row2.put(4, "mobilePhone");
        row2.put(5, "emergencyContact");
        emergencyContactFieldOrder.add(row2);

        Map<Integer, String> row3 = new HashMap<>();
        row3.put(0, "lastName");
        row3.put(1, "firstName");
        row3.put(2, "middleInitial");
        row3.put(3, "seatNumber");
        row3.put(4, "dateOfBirth");
        row3.put(5, "sex");
        row3.put(6, "nationality");
        row3.put(7, "passportNumber");
        row3.put(8, "familyTravelCompanions");
        row3.put(9, "nonFamilyTravelCompanions");
        travelCompanionFieldOrder.add(row3);
    }

    private void addValueRow(Analysis analysis) {
        Map<String, String> valueRow = new HashMap<>();
        Optional<Result> result = getResultForAnalysis(analysis);
        Patient patient = getPatientForAnalysis(analysis);
        Sample sample = analysis.getSampleItem().getSample();

        SampleAdditionalField contactTracingIndexName = sampleService.getSampleAdditionalFieldForSample(sample.getId(),
                AdditionalFieldName.CONTACT_TRACING_INDEX_NAME);

        SampleAdditionalField contactTracingIndexRecordNumber = sampleService.getSampleAdditionalFieldForSample(
                sample.getId(), AdditionalFieldName.CONTACT_TRACING_INDEX_RECORD_NUMBER);

        valueRow.put(CONTACT_TRACING_INDEX_NAME, contactTracingIndexName.getFieldValue());
        valueRow.put(CONTACT_TRACING_INDEX_RECORD_NUMBER, contactTracingIndexRecordNumber.getFieldValue());

        valueRow.put(ORDER_NUMBER_PROPERTY_NAME, analysis.getSampleItem().getSample().getAccessionNumber());
        valueRow.put(SITE_PROPERTY_NAME, ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));
        // SampleService sampleService = SpringContext.getBean(SampleService.class);
        valueRow.put(SAMPLE_RECEIVED_DATE_PROPERTY_NAME, sampleService.getReceivedDateWithTwoYearDisplay(sample));
        valueRow.put(SAMPLE_STATUS_PROPERTY_NAME, sampleService.getSampleStatusForDisplay(sample));
        valueRow.put(DATE_PROPERTY_NAME, analysis.getStartedDateForDisplay());
        valueRow.put(RESULT_PROPERTY_NAME, getResultValue(result));

        valueRow.put(PATIENT_LAST_NAME_PROPERTY_NAME, patient.getPerson().getLastName());
        valueRow.put(PATIENT_FIRST_NAME_PROPERTY_NAME, patient.getPerson().getFirstName());
        valueRow.put(PATIENT_DATE_OF_BIRTH_PROPERTY_NAME, patient.getBirthDateForDisplay());
        valueRow.put(PATIENT_PHONE_NO_PROPERTY_NAME, patient.getPerson().getPrimaryPhone());

        // gnr: use srStrings to test by commenting out getTaskForAnalysis
        // String tString =
        // "{\"resourceType\":\"Task\",\"id\":\"04c4fb6d-8738-4926-a7fe-699f4e28f2d7\",\"meta\":{\"versionId\":\"1\",\"lastUpdated\":\"2020-10-12T05:16:23.651+00:00\",\"source\":\"#Q22w7sIOqJxRwBlj\"},\"identifier\":[{\"id\":\"04c4fb6d-8738-4926-a7fe-699f4e28f2d7\",\"system\":\"https://host.openelis.org/locator-form\"},{\"system\":\"https://testing.openelisci.org:8448/hapi-fhir-jpaserver/fhir/\",\"value\":\"04c4fb6d-8738-4926-a7fe-699f4e28f2d7\"}],\"basedOn\":[{\"reference\":\"ServiceRequest/01ba623d-e465-41a3-bb83-ddcef1edf297\",\"type\":\"ServiceRequest\"}],\"status\":\"requested\",\"description\":\"{\\\"lastName\\\":\\\"Bhurosah\\\",\\\"firstName\\\":\\\"Suresh\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"\\\",\\\"dateOfBirth\\\":\\\"1987-10-20\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"MU\\\",\\\"passportNumber\\\":\\\"78u\\\",\\\"travellerType\\\":\\\"RESIDENT\\\",\\\"airlineName\\\":\\\"Air
        // Mauritius\\\",\\\"flightNumber\\\":\\\"MK0241\\\",\\\"arrivalDate\\\":\\\"2020-10-13\\\",\\\"title\\\":\\\"MR\\\",\\\"lengthOfStay\\\":null,\\\"countriesVisited\\\":[\\\"AE\\\"],\\\"portOfEmbarkation\\\":\\\"\\\",\\\"fever\\\":null,\\\"soreThroat\\\":null,\\\"jointPain\\\":null,\\\"cough\\\":null,\\\"breathingDifficulties\\\":null,\\\"rash\\\":null,\\\"hadCovidBefore\\\":null,\\\"visitPurpose\\\":\\\"BUSINESS\\\",\\\"mobilePhone\\\":\\\"+23057789383\\\",\\\"fixedPhone\\\":\\\"+2304600133\\\",\\\"email\\\":\\\"pmohabeer@govmu.org\\\",\\\"nationalID\\\":\\\"09087666657456G\\\",\\\"permanentAddress\\\":{\\\"hotelName\\\":null,\\\"numberAndStreet\\\":\\\"Rosehill\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":\\\"\\\"},\\\"temporaryAddress\\\":{\\\"hotelName\\\":\\\"Civic\\\",\\\"numberAndStreet\\\":\\\"camp\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":null},\\\"emergencyContact\\\":{\\\"lastName\\\":\\\"Mohabeer\\\",\\\"firstName\\\":\\\"priya\\\",\\\"address\\\":\\\"Royal
        // Road Mare
        // Tabac\\\",\\\"country\\\":\\\"MU\\\",\\\"mobilePhone\\\":\\\"+23057582994\\\"},\\\"familyTravelCompanions\\\":[],\\\"nonFamilyTravelCompanions\\\":[],\\\"acceptedTerms\\\":true}\"}";

        // empty arrays
        // String srStringEmpty
        // ="{\"resourceType\":\"ServiceRequest\",\"status\":\"requested\",\"description\":\"{\\\"lastName\\\":\\\"Bhurosah\\\",\\\"firstName\\\":\\\"Suresh\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"\\\",\\\"dateOfBirth\\\":\\\"1987-10-20\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"MU\\\",\\\"passportNumber\\\":\\\"78u\\\",\\\"travellerType\\\":\\\"RESIDENT\\\",\\\"airlineName\\\":\\\"Air
        // Mauritius\\\",\\\"flightNumber\\\":\\\"MK0241\\\",\\\"arrivalDate\\\":\\\"2020-10-13\\\",\\\"title\\\":\\\"MR\\\",\\\"lengthOfStay\\\":null,\\\"countriesVisited\\\":[\\\"AE\\\"],\\\"portOfEmbarkation\\\":\\\"\\\",\\\"fever\\\":null,\\\"soreThroat\\\":null,\\\"jointPain\\\":null,\\\"cough\\\":null,\\\"breathingDifficulties\\\":null,\\\"rash\\\":null,\\\"hadCovidBefore\\\":null,\\\"visitPurpose\\\":\\\"BUSINESS\\\",\\\"mobilePhone\\\":\\\"+23057789383\\\",\\\"fixedPhone\\\":\\\"+2304600133\\\",\\\"email\\\":\\\"pmohabeer@govmu.org\\\",\\\"nationalID\\\":\\\"09087666657456G\\\",\\\"permanentAddress\\\":{\\\"hotelName\\\":null,\\\"numberAndStreet\\\":\\\"Rosehill\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":\\\"\\\"},\\\"temporaryAddress\\\":{\\\"hotelName\\\":\\\"Civic\\\",\\\"numberAndStreet\\\":\\\"camp\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":null},\\\"emergencyContact\\\":{\\\"lastName\\\":\\\"Mohabeer\\\",\\\"firstName\\\":\\\"priya\\\",\\\"address\\\":\\\"Royal
        // Road Mare
        // Tabac\\\",\\\"country\\\":\\\"MU\\\",\\\"mobilePhone\\\":\\\"+23057582994\\\"},\\\"familyTravelCompanions\\\":[],\\\"nonFamilyTravelCompanions\\\":[],\\\"acceptedTerms\\\":true}\"}";
        // family 1, nonF empty String srString
        // ="{\"resourceType\":\"ServiceRequest\",\"status\":\"requested\",\"description\":\"{\\\"lastName\\\":\\\"Bhurosah\\\",\\\"firstName\\\":\\\"Suresh\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"\\\",\\\"dateOfBirth\\\":\\\"1987-10-20\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"MU\\\",\\\"passportNumber\\\":\\\"78u\\\",\\\"travellerType\\\":\\\"RESIDENT\\\",\\\"airlineName\\\":\\\"Air
        // Mauritius\\\",\\\"flightNumber\\\":\\\"MK0241\\\",\\\"arrivalDate\\\":\\\"2020-10-13\\\",\\\"title\\\":\\\"MR\\\",\\\"lengthOfStay\\\":null,\\\"countriesVisited\\\":[\\\"AE\\\"],\\\"portOfEmbarkation\\\":\\\"\\\",\\\"fever\\\":null,\\\"soreThroat\\\":null,\\\"jointPain\\\":null,\\\"cough\\\":null,\\\"breathingDifficulties\\\":null,\\\"rash\\\":null,\\\"hadCovidBefore\\\":null,\\\"visitPurpose\\\":\\\"BUSINESS\\\",\\\"mobilePhone\\\":\\\"+23057789383\\\",\\\"fixedPhone\\\":\\\"+2304600133\\\",\\\"email\\\":\\\"pmohabeer@govmu.org\\\",\\\"nationalID\\\":\\\"09087666657456G\\\",\\\"permanentAddress\\\":{\\\"hotelName\\\":null,\\\"numberAndStreet\\\":\\\"Rosehill\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":\\\"\\\"},\\\"temporaryAddress\\\":{\\\"hotelName\\\":\\\"Civic\\\",\\\"numberAndStreet\\\":\\\"camp\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":null},\\\"emergencyContact\\\":{\\\"lastName\\\":\\\"Mohabeer\\\",\\\"firstName\\\":\\\"priya\\\",\\\"address\\\":\\\"Royal
        // Road Mare
        // Tabac\\\",\\\"country\\\":\\\"MU\\\",\\\"mobilePhone\\\":\\\"+23057582994\\\"},\\\"familyTravelCompanions\\\":[{\\\"lastName\\\":\\\"familyTest\\\",\\\"firstName\\\":\\\"Junior\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"4B\\\",\\\"dateOfBirth\\\":\\\"2005-05-15\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"LU\\\",\\\"passportNumber\\\":\\\"7676767676767\\\"}],\\\"nonFamilyTravelCompanions\\\":[],\\\"acceptedTerms\\\":true}\"}";

        // fam 1 nonf 1
        // String srString
        // ="{\"resourceType\":\"ServiceRequest\",\"status\":\"requested\",\"description\":\"{\\\"lastName\\\":\\\"Bhurosah\\\",\\\"firstName\\\":\\\"Suresh\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"\\\",\\\"dateOfBirth\\\":\\\"1987-10-20\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"MU\\\",\\\"passportNumber\\\":\\\"78u\\\",\\\"travellerType\\\":\\\"RESIDENT\\\",\\\"airlineName\\\":\\\"Air
        // Mauritius\\\",\\\"flightNumber\\\":\\\"MK0241\\\",\\\"arrivalDate\\\":\\\"2020-10-13\\\",\\\"title\\\":\\\"MR\\\",\\\"lengthOfStay\\\":null,\\\"countriesVisited\\\":[\\\"AE\\\"],\\\"portOfEmbarkation\\\":\\\"\\\",\\\"fever\\\":null,\\\"soreThroat\\\":null,\\\"jointPain\\\":null,\\\"cough\\\":null,\\\"breathingDifficulties\\\":null,\\\"rash\\\":null,\\\"hadCovidBefore\\\":null,\\\"visitPurpose\\\":\\\"BUSINESS\\\",\\\"mobilePhone\\\":\\\"+23057789383\\\",\\\"fixedPhone\\\":\\\"+2304600133\\\",\\\"email\\\":\\\"pmohabeer@govmu.org\\\",\\\"nationalID\\\":\\\"09087666657456G\\\",\\\"permanentAddress\\\":{\\\"hotelName\\\":null,\\\"numberAndStreet\\\":\\\"Rosehill\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":\\\"\\\"},\\\"temporaryAddress\\\":{\\\"hotelName\\\":\\\"Civic\\\",\\\"numberAndStreet\\\":\\\"camp\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":null},\\\"emergencyContact\\\":{\\\"lastName\\\":\\\"Mohabeer\\\",\\\"firstName\\\":\\\"priya\\\",\\\"address\\\":\\\"Royal
        // Road Mare
        // Tabac\\\",\\\"country\\\":\\\"MU\\\",\\\"mobilePhone\\\":\\\"+23057582994\\\"},\\\"familyTravelCompanions\\\":[{\\\"lastName\\\":\\\"familyTest\\\",\\\"firstName\\\":\\\"Junior\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"4B\\\",\\\"dateOfBirth\\\":\\\"2005-05-15\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"LU\\\",\\\"passportNumber\\\":\\\"7676767676767\\\"}]
        // ,\\\"nonFamilyTravelCompanions\\\":[{\\\"lastName\\\":\\\"familyTest\\\",\\\"firstName\\\":\\\"Junior\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"4B\\\",\\\"dateOfBirth\\\":\\\"2005-05-15\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"LU\\\",\\\"passportNumber\\\":\\\"7676767676767\\\"}],\\\"acceptedTerms\\\":true}\"}";

        // fam 1 nonf 2
        // String srString12
        // ="{\"resourceType\":\"ServiceRequest\",\"status\":\"requested\",\"description\":\"{\\\"lastName\\\":\\\"Bhurosah\\\",\\\"firstName\\\":\\\"Suresh\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"\\\",\\\"dateOfBirth\\\":\\\"1987-10-20\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"MU\\\",\\\"passportNumber\\\":\\\"78u\\\",\\\"travellerType\\\":\\\"RESIDENT\\\",\\\"airlineName\\\":\\\"Air
        // Mauritius\\\",\\\"flightNumber\\\":\\\"MK0241\\\",\\\"arrivalDate\\\":\\\"2020-10-13\\\",\\\"title\\\":\\\"MR\\\",\\\"lengthOfStay\\\":null,\\\"countriesVisited\\\":[\\\"AE\\\"],\\\"portOfEmbarkation\\\":\\\"\\\",\\\"fever\\\":null,\\\"soreThroat\\\":null,\\\"jointPain\\\":null,\\\"cough\\\":null,\\\"breathingDifficulties\\\":null,\\\"rash\\\":null,\\\"hadCovidBefore\\\":null,\\\"visitPurpose\\\":\\\"BUSINESS\\\",\\\"mobilePhone\\\":\\\"+23057789383\\\",\\\"fixedPhone\\\":\\\"+2304600133\\\",\\\"email\\\":\\\"pmohabeer@govmu.org\\\",\\\"nationalID\\\":\\\"09087666657456G\\\",\\\"permanentAddress\\\":{\\\"hotelName\\\":null,\\\"numberAndStreet\\\":\\\"Rosehill\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":\\\"\\\"},\\\"temporaryAddress\\\":{\\\"hotelName\\\":\\\"Civic\\\",\\\"numberAndStreet\\\":\\\"camp\\\",\\\"apartmentNumber\\\":\\\"\\\",\\\"city\\\":\\\"\\\",\\\"stateProvince\\\":\\\"\\\",\\\"country\\\":\\\"MU\\\",\\\"zipPostalCode\\\":null},\\\"emergencyContact\\\":{\\\"lastName\\\":\\\"Mohabeer\\\",\\\"firstName\\\":\\\"priya\\\",\\\"address\\\":\\\"Royal
        // Road Mare
        // Tabac\\\",\\\"country\\\":\\\"MU\\\",\\\"mobilePhone\\\":\\\"+23057582994\\\"},\\\"familyTravelCompanions\\\":[{\\\"lastName\\\":\\\"familyTest\\\",\\\"firstName\\\":\\\"Junior\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"4B\\\",\\\"dateOfBirth\\\":\\\"2005-05-15\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"LU\\\",\\\"passportNumber\\\":\\\"7676767676767\\\"}],\\\"nonFamilyTravelCompanions\\\":[{\\\"lastName\\\":\\\"nonFamilyTest\\\",\\\"firstName\\\":\\\"Junior1\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"4B\\\",\\\"dateOfBirth\\\":\\\"2005-05-15\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"LU\\\",\\\"passportNumber\\\":\\\"7676767676767\\\"},
        // {\\\"lastName\\\":\\\"nonFamilyTest\\\",\\\"firstName\\\":\\\"Junior2\\\",\\\"middleInitial\\\":\\\"\\\",\\\"seatNumber\\\":\\\"4B\\\",\\\"dateOfBirth\\\":\\\"2005-05-15\\\",\\\"sex\\\":\\\"MALE\\\",\\\"nationality\\\":\\\"LU\\\",\\\"passportNumber\\\":\\\"7676767676767\\\"}],\\\"acceptedTerms\\\":true}\"}";

        // FhirContext fhirContext = FhirContext.forR4();
        // IParser parser = fhirContext.newJsonParser();

        // Task ttask = parser.parseResource(Task.class, tString);
        // Optional<Task> task = Optional.ofNullable(ttask);
        //
        //
        // String srString = "";
        // if
        // (analysis.getSampleItem().getSample().getAccessionNumber().equalsIgnoreCase("10200000000422724"))
        // {
        // srString = srStringEmpty;
        // } else {
        // srString = srString12;
        // }
        //
        // JSONObject json = new JSONObject(srString);
        // System.out.println("description:" + json.getString("description"));
        //
        // JSONObject jDescription = new JSONObject(json.getString("description"));
        // System.out.println("description:" + jDescription.toString());
        //
        // JSONArray jFamilyTravelCompanions = new
        // JSONArray(jDescription.get("familyTravelCompanions").toString());
        // System.out.println("familyTravelCompanions:" +
        // jFamilyTravelCompanions.toString());
        // JSONArray jNonFamilyTravelCompanions = new
        // JSONArray(jDescription.get("nonFamilyTravelCompanions").toString());
        // System.out.println("jNonFamilyTravelCompanions:" +
        // jNonFamilyTravelCompanions.toString());

        try {
            Optional<Task> task = getReferringTaskForAnalysis(analysis);

            if (task.isPresent() && !GenericValidator.isBlankOrNull(task.get().getDescription())) {
                // if (true) {
                try {
                    JSONObject jDescription = new JSONObject(task.get().getDescription());
                    JSONArray jFamilyTravelCompanions = new JSONArray(
                            jDescription.get("familyTravelCompanions").toString());
                    JSONArray jNonFamilyTravelCompanions = new JSONArray(
                            jDescription.get("nonFamilyTravelCompanions").toString());

                    convertJSONToCSV(new JSONObject(task.get().getDescription()), LOCATOR_FORM_PROPERTY_NAME, valueRow,
                            jFamilyTravelCompanions, jNonFamilyTravelCompanions);
                } catch (JSONException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "addValueRow",
                            "could not make json from task description");
                    LogEvent.logError(e);
                }
            }

            valueRows.add(valueRow);

        } catch (IllegalStateException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "addValueRow",
                    "could not retrieve FhirTask from analysis with id: " + analysis.getId());
            LogEvent.logError(this.getClass().getSimpleName(), "addValueRow", e.getMessage());
        }
    }

    private void convertJSONToCSV(JSONObject jsonObject, String keyPrefix, Map<String, String> valueRow,
            Map<Integer, String> keyMap) {

        for (Integer i = 0; i <= keyMap.size(); i++) {
            String keyMatch = keyMap.get(i);
            for (String key : jsonObject.keySet()) {
                if (!key.equalsIgnoreCase(keyMatch)) { // skip
                } else {

                    if (jsonObject.get(key) == null) {
                        String columnName = getCreateColumnName(keyPrefix, ".", key);
                        valueRow.put(columnName, EMPTY_VALUE);
                    } else {
                        String columnName = getCreateColumnName(keyPrefix, ".", key);
                        valueRow.put(columnName, jsonObject.get(key).toString());
                    }
                }
            }
        }
    }

    private void convertJSONToCSV(JSONObject jsonObject, String keyPrefix, Map<String, String> valueRow,
            JSONArray jFamilyTravelCompanions, JSONArray jNonFamilyTravelCompanions) {

        Map<Integer, String> keyMap = descriptionFieldOrder.get(0);

        for (Integer i = 0; i <= keyMap.size(); i++) {
            String keyMatch = keyMap.get(i);
            for (String key : jsonObject.keySet()) {
                if (!key.equalsIgnoreCase(keyMatch)) { // skip
                } else {

                    if (jsonObject.get(key) == null) {
                        String columnName = getCreateColumnName(keyPrefix, ".", key);
                        valueRow.put(columnName, EMPTY_VALUE);
                    } else if (!(key.equalsIgnoreCase("permanentAddress") || key.equalsIgnoreCase("temporaryAddress")
                            || key.equalsIgnoreCase("emergencyContact")
                            || key.equalsIgnoreCase("familyTravelCompanions")
                            || key.equalsIgnoreCase("nonFamilyTravelCompanions"))) {
                        String columnName = getCreateColumnName(keyPrefix, ".", key);
                        valueRow.put(columnName, jsonObject.get(key).toString());
                    }
                }
            }
        }

        keyMap = addressFieldOrder.get(0);
        for (Integer i = 0; i <= keyMap.size(); i++) {
            String keyMatch = keyMap.get(i);
            for (String key : jsonObject.keySet()) {
                if (!key.equalsIgnoreCase(keyMatch)) { // skip
                } else {

                    if (jsonObject.get(key) instanceof JSONObject
                            && (key.equalsIgnoreCase("permanentAddress") || key.equalsIgnoreCase("temporaryAddress"))) {
                        convertJSONToCSV(jsonObject.getJSONObject(key), keyPrefix + "." + key, valueRow, keyMap);
                    }
                }
            }
        }

        keyMap = emergencyContactFieldOrder.get(0);
        for (Integer i = 0; i <= keyMap.size(); i++) {
            String keyMatch = keyMap.get(i);
            for (String key : jsonObject.keySet()) {
                if (!key.equalsIgnoreCase(keyMatch)) { // skip
                } else {

                    if (jsonObject.get(key) instanceof JSONObject && (key.equalsIgnoreCase("emergencyContact"))) {
                        convertJSONToCSV(jsonObject.getJSONObject(key), keyPrefix + "." + key, valueRow, keyMap);
                    }
                }
            }
        }

        keyMap = travelCompanionFieldOrder.get(0);
        for (Integer i = 0; i <= keyMap.size(); i++) {
            String keyMatch = keyMap.get(i);
            for (String key : jsonObject.keySet()) {
                if (!key.equalsIgnoreCase(keyMatch)) { // skip
                } else {

                    if (jsonObject.get(key) instanceof JSONArray && key.equalsIgnoreCase("familyTravelCompanions")) {
                        convertJSONToCSV(jFamilyTravelCompanions, keyPrefix + "." + key, valueRow, keyMap);
                    }
                    if (jsonObject.get(key) instanceof JSONArray && key.equalsIgnoreCase("nonFamilyTravelCompanions")) {
                        convertJSONToCSV(jNonFamilyTravelCompanions, keyPrefix + "." + key, valueRow, keyMap);
                    }
                }
            }
        }
    }

    private void convertJSONToCSV(JSONArray jsonArray, String keyPrefix, Map<String, String> valueRow,
            Map<Integer, String> keyMap) {

        Iterator<Object> iterator = jsonArray.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Object object = iterator.next();
            ++i;
            if (object == null) {
                String columnName = getCreateColumnName(keyPrefix, "_", Integer.toString(i));
                valueRow.put(columnName, EMPTY_VALUE);
            } else if (object instanceof JSONObject) {
                convertJSONToCSV((JSONObject) object, keyPrefix + "_" + i, valueRow, keyMap);
            } else {
                String columnName = getCreateColumnName(keyPrefix, "_", Integer.toString(i));
                valueRow.put(columnName, object.toString());
            }
        }
    }

    private String getCreateColumnName(String keyPrefix, String infix, String suffix) {
        String newColumnName = (keyPrefix + infix + suffix);
        if (!columnNames.contains(newColumnName)) {
            columnNames.add(newColumnName);
        }
        return newColumnName;
    }

    private byte[] getColumnNamesRowBytes() {
        return String.join(",", columnNames).getBytes(Charsets.UTF_8);
    }

    @Override
    public byte[] getDataSourceAsByteArray() {
        byte[] columnNamesRowBytes = getColumnNamesRowBytes();
        List<byte[]> resultRowsBytes = getResultRowsAsBytes();

        int totalArraySize = columnNamesRowBytes.length;
        for (byte[] resultRowBytes : resultRowsBytes) {
            totalArraySize += resultRowBytes.length;
        }
        byte[] allByteArray = new byte[totalArraySize];
        ByteBuffer buff = ByteBuffer.wrap(allByteArray);
        buff.put(columnNamesRowBytes);
        for (byte[] resultRowBytes : resultRowsBytes) {
            buff.put(resultRowBytes);
        }

        return buff.array();
    }

    private List<byte[]> getResultRowsAsBytes() {
        List<byte[]> resultRowsBytes = new ArrayList<>();
        for (Map<String, String> valueRow : valueRows) {
            List<String> orderedValues = new ArrayList<>();
            for (String columnName : columnNames) {
                if (valueRow.containsKey(columnName)) {
                    orderedValues.add(valueRow.get(columnName));
                } else {
                    orderedValues.add(EMPTY_VALUE);
                }
            }
            resultRowsBytes.add(("\n" + String.join(",", orderedValues)).getBytes(Charsets.UTF_8));
        }
        return resultRowsBytes;
    }
}
