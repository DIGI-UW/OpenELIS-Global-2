package org.openelisglobal.fhir;

import org.hl7.fhir.r4.model.ServiceRequest;

public final class FhirConstants {

    private FhirConstants() {
    }

    public static final String ID_PROPERTY = "fhirUuid";
    public static final String LAST_UPDATED_PROPERTY = "lastupdated";

    public static final String PERSON = "person";
    public static final String TELECOM = "telecom";
    public static final String ADDRESS = "address";

    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String CITY = "city";
    public static final String STATE = "state";
    public static final String POSTAL_CODE = "zipCode";
    public static final String COUNTRY = "country";
    public static final String EMAIL = "email";
    public static final String WORK_PHONE = "workPhone";
    public static final String HOME_PHONE = "homePhone";
    public static final String CELL_PHONE = "cellPhone";
    public static final String PRIMARY_PHONE = "primaryPhone";
    public static final String FAX = "fax";

    public static final String PATIENT = "patient";
    public static final String ANALYSIS = "analysis";

    /*
     * Hibernate property names—not SQL table names.
     */
    public static final String SAMPLE_ITEM = "sampleItem";
    public static final String SAMPLE = "sample";
    public static final String SAMPLE_ID = "sampleId";
    public static final String PROVIDER_ID = "providerId";
    public static final String PATIENT_ID = "patientId";

    public static final String SAMPLE_ITEM_ID_HANDLER = SAMPLE_ITEM + ".id";

    public static final String SAMPLE_ITEM_FHIR_UUID_HANDLER = SAMPLE_ITEM + "." + ID_PROPERTY;

    public static final String ANALYSIS_SAMPLE_ID_HANDLER = SAMPLE_ITEM + "." + SAMPLE + ".id";

    public static final String ANALYSIS_SAMPLE_FHIR_UUID_HANDLER = SAMPLE_ITEM + "." + SAMPLE + "." + ID_PROPERTY;

    public static final String IDENTIFIER_SEARCH_HANDLER = ID_PROPERTY;

    public static final String FIRST_NAME_SEARCH_HANDLER = PERSON + "." + FIRST_NAME;

    public static final String LAST_NAME_SEARCH_HANDLER = PERSON + "." + LAST_NAME;

    public static final String CITY_SEARCH_HANDLER = PERSON + "." + CITY;

    public static final String STATE_SEARCH_HANDLER = PERSON + "." + STATE;

    public static final String POSTALCODE_SEARCH_HANDLER = PERSON + "." + POSTAL_CODE;

    public static final String COUNTRY_SEARCH_HANDLER = PERSON + "." + COUNTRY;

    public static final String EMAIL_SEARCH_HANDLER = PERSON + "." + EMAIL;

    public static final String WORK_PHONE_SEARCH_HANDLER = PERSON + "." + WORK_PHONE;

    public static final String HOME_PHONE_SEARCH_HANDLER = PERSON + "." + HOME_PHONE;

    public static final String CELL_PHONE_SEARCH_HANDLER = PERSON + "." + CELL_PHONE;

    public static final String PRIMARY_PHONE_SEARCH_HANDLER = PERSON + "." + PRIMARY_PHONE;

    public static final String FAX_SEARCH_HANDLER = PERSON + "." + FAX;

    public static final String NAME_SEARCH_HANDLER = "name";
    public static final String TELECOM_SEARCH_HANDLER = "telecom";

    public static final String PATIENT_FIRST_NAME_SEARCH_HANDLER = PATIENT + "." + PERSON + "." + FIRST_NAME;

    public static final String PATIENT_LAST_NAME_SEARCH_HANDLER = PATIENT + "." + PERSON + "." + LAST_NAME;

    public static final String PATIENT_CITY_SEARCH_HANDLER = PATIENT + "." + PERSON + "." + CITY;

    public static final String PATIENT_STATE_SEARCH_HANDLER = PATIENT + "." + PERSON + "." + STATE;

    public static final String ANALYSIS_PATIENT_HANDLER = ANALYSIS + "." + PATIENT;
    public static final String SERVICE_REQUEST_REQUESTER_REV_INCLUDE = "ServiceRequest:" + ServiceRequest.SP_REQUESTER;

    public static final String SERVICE_REQUEST_PATIENT_REV_INCLUDE = "ServiceRequest:" + ServiceRequest.SP_PATIENT;
    public static final String SERVICE_REQUEST_SUBJECT_REV_INCLUDE = "ServiceRequest:" + ServiceRequest.SP_SUBJECT;
    public static final String SPECIMEN_PATIENT_REV_INCLUDE = "Specimen:patient";
    public static final String SPECIMEN_SUBJECT_REV_INCLUDE = "Specimen:subject";
    public static final String OBSERVATION_PATIENT_REV_INCLUDE = "Observation:patient";
    public static final String OBSERVATION_SUBJECT_REV_INCLUDE = "Observation:subject";
    public static final String DIAGNOSTIC_REPORT_PATIENT_REV_INCLUDE = "DiagnosticReport:patient";
    public static final String DIAGNOSTIC_REPORT_SUBJECT_REV_INCLUDE = "DiagnosticReport:subject";
    public static final String ORGANIZATION_PARTOF_INCLUDE = "Organization:partof";
    public static final String SPECIMEN_PATIENT_INCLUDE = "Specimen:patient";
    public static final String SPECIMEN_SUBJECT_INCLUDE = "Specimen:subject";
    public static final String SERVICE_REQUEST_SPECIMEN_REV_INCLUDE = "ServiceRequest:specimen";
    public static final String OBSERVATION_SPECIMEN_REV_INCLUDE = "Observation:specimen";
    public static final String SERVICE_REQUEST_PATIENT_INCLUDE = "ServiceRequest:patient";
    public static final String SERVICE_REQUEST_SUBJECT_INCLUDE = "ServiceRequest:subject";
    public static final String SERVICE_REQUEST_REQUESTER_INCLUDE = "ServiceRequest:requester";
    public static final String SERVICE_REQUEST_SPECIMEN_INCLUDE = "ServiceRequest:specimen";
    public static final String OBSERVATION_BASED_ON_REV_INCLUDE = "Observation:based-on";
    public static final String DIAGNOSTIC_REPORT_BASED_ON_REV_INCLUDE = "DiagnosticReport:based-on";
    public static final String OBSERVATION_PATIENT_INCLUDE = "Observation:patient";
    public static final String OBSERVATION_SUBJECT_INCLUDE = "Observation:subject";
    public static final String OBSERVATION_BASED_ON_INCLUDE = "Observation:based-on";
    public static final String OBSERVATION_SPECIMEN_INCLUDE = "Observation:specimen";
    public static final String OBSERVATION_PERFORMER_INCLUDE = "Observation:performer";
    public static final String DIAGNOSTIC_REPORT_RESULT_REV_INCLUDE = "DiagnosticReport:result";
    public static final String DIAGNOSTIC_REPORT_PATIENT_INCLUDE = "DiagnosticReport:patient";
    public static final String DIAGNOSTIC_REPORT_SUBJECT_INCLUDE = "DiagnosticReport:subject";
    public static final String DIAGNOSTIC_REPORT_BASED_ON_INCLUDE = "DiagnosticReport:based-on";
    public static final String DIAGNOSTIC_REPORT_RESULT_INCLUDE = "DiagnosticReport:result";
    public static final String DIAGNOSTIC_REPORT_SPECIMEN_INCLUDE = "DiagnosticReport:specimen";
    public static final String LOCATION_PARTOF_INCLUDE = "Location:partof";
    public static final String OBSERVATION_PERFORMER_REV_INCLUDE = "Observation:performer";
    public static final String DIAGNOSTIC_REPORT_SPECIMEN_REV_INCLUDE = "DiagnosticReport:specimen";
}