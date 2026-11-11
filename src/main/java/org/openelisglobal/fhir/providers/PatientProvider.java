package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.PatientSearchParams;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.search.service.PatientSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Read and search Patient resources from OpenELIS. */
@Component
public class PatientProvider implements IResourceProvider {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientSearchService patientSearchService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return org.hl7.fhir.r4.model.Patient.class;
    }

    @Read
    public org.hl7.fhir.r4.model.Patient getPatientByUUID(@IdParam IdType theId) {
        String method = "Read";
        try {
            FhirProviderUtils.requireUuidId(theId, "Patient");
            List<Patient> patients = patientService.getAllMatching("fhirUuid", UUID.fromString(theId.getIdPart()));
            if (patients.isEmpty()) {
                throw new ResourceNotFoundException("Patient/" + theId.getIdPart());
            }
            return fhirTransformService.transformToFhirPatient(patients.get(0).getId());
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Patient", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading Patient: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Patient", e);
        }
    }

    @Search
    public IBundleProvider searchPatients(
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_NAME) StringAndListParam name,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_GIVEN) StringAndListParam given,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_FAMILY) StringAndListParam family,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_BIRTHDATE) DateRangeParam birthdate,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_GENDER) TokenAndListParam gender,
            @OptionalParam(name = "address-city") StringAndListParam city,

            @OptionalParam(name = "address-state") StringAndListParam state,

            @OptionalParam(name = "address-postalcode") StringAndListParam postalCode,

            @OptionalParam(name = "address-country") StringAndListParam country,

            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_TELECOM) TokenAndListParam telecom,

            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_EMAIL) TokenAndListParam email,

            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_PHONE) TokenAndListParam phone,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @Offset Integer offset, @Count Integer count,
            @IncludeParam(reverse = true, allow = { FhirConstants.SERVICE_REQUEST_PATIENT_REV_INCLUDE,
                    FhirConstants.SERVICE_REQUEST_SUBJECT_REV_INCLUDE, FhirConstants.SPECIMEN_PATIENT_REV_INCLUDE,
                    FhirConstants.SPECIMEN_SUBJECT_REV_INCLUDE, FhirConstants.OBSERVATION_PATIENT_REV_INCLUDE,
                    FhirConstants.OBSERVATION_SUBJECT_REV_INCLUDE, FhirConstants.DIAGNOSTIC_REPORT_PATIENT_REV_INCLUDE,
                    FhirConstants.DIAGNOSTIC_REPORT_SUBJECT_REV_INCLUDE }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        String methodName = "searchPatients";
        LogEvent.logDebug(this.getClass().getSimpleName(), methodName, "Searching for Patients");

        try {
            PatientSearchParams params = new PatientSearchParams(id, identifier, name, given, family, birthdate, gender,
                    city, state, postalCode, country, telecom, email, phone, lastUpdated, sort, revIncludes);
            return FhirProviderUtils.withPaging(patientSearchService.searchPatients(params), offset, count);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Invalid Patient search parameter: " + e.getMessage());
            throw new InvalidRequestException("Invalid Patient search parameter: " + e.getMessage(), e);
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Patient", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Patients: " + e.getMessage());
            throw new InternalErrorException("Error searching Patients", e);
        }
    }

}
