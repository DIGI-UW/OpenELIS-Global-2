package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HAPI FHIR resource provider for the Patient resource. Handles Read, Search,
 * Update, and Delete operations against the local OpenELIS database with
 * synchronization to the FHIR store.
 *
 * <p>
 * Note: {@code @Create} is not yet supported because no public
 * {@code transformToPatient(org.hl7.fhir.r4.model.Patient)} reverse transform
 * exists. This will be added in a follow-up PR once the transform is made
 * public.
 *
 * <p>
 * Auto-discovered by {@link org.openelisglobal.fhir.servlets.FhirRestfulServer}
 * as a Spring {@code @Component} implementing {@link IResourceProvider}.
 */
@Component
public class PatientProvider implements IResourceProvider {

    @Autowired
    private FhirUtil util;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirPersistanceService fhirPersistenceService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private PersonService personService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return org.hl7.fhir.r4.model.Patient.class;
    }

    @Read
    public org.hl7.fhir.r4.model.Patient getPatientByUUID(@IdParam IdType theId) {
        String method = "Read";
        try {
            if (theId == null || !theId.hasIdPart()) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Missing Patient ID for Read");
                throw new InvalidRequestException("Patient ID must be provided for Read");
            }
            Patient patient = getPatientByFhirId(theId.getIdPart());
            if (patient == null) {
                throw new ResourceNotFoundException("Patient/" + theId.getIdPart());
            }
            return fhirTransformService.transformToFhirPatient(patient.getId());
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading Patient: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Patient", e);
        }
    }

    @Search
    public Bundle searchPatientBundle(
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_GIVEN) StringAndListParam given,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_FAMILY) StringAndListParam family,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_NAME) StringAndListParam name,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_BIRTHDATE) DateRangeParam birthdate,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_GENDER) TokenAndListParam gender,
            @OptionalParam(name = org.hl7.fhir.r4.model.Patient.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,
            @IncludeParam(reverse = true, allow = { "Encounter:" + Encounter.SP_PATIENT,
                    "ServiceRequest:" + ServiceRequest.SP_SUBJECT, }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        String methodName = "searchPatientBundle";
        LogEvent.logDebug(this.getClass().getSimpleName(), methodName, "Searching for Patients (returning Bundle)");

        try {

            Bundle bundle = util.forwardSearchToFhirStore(request);

            return bundle;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Patients: " + e.getMessage());
            throw new InternalErrorException("Error searching Patients", e);
        }
    }

    @Update
    public MethodOutcome update(@IdParam IdType theId, @ResourceParam org.hl7.fhir.r4.model.Patient fhirPatient,
            HttpServletRequest request) {

        String method = "update";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR UPDATE request for Patient ID: " + (theId != null ? theId.getIdPart() : "null"));

        try {

            FhirProviderUtils.validateIdParam(theId, "Patient", this.getClass().getSimpleName(), method);

            fhirPatient.setId(theId);

            Patient existingPatient = getPatientByFhirId(theId.getIdPart());
            if (existingPatient == null) {
                throw new ResourceNotFoundException("Patient/" + theId.getIdPart());
            }

            Person existingPerson = personService.get(existingPatient.getPerson().getId());

            fhirTransformService.addHumanNameToPerson(fhirPatient.getNameFirstRep(), existingPerson);
            fhirTransformService.addTelecomToPerson(fhirPatient.getTelecom(), existingPerson);
            existingPerson.setSysUserId(FhirProviderUtils.getSysUserId(request));
            personService.save(existingPerson);

            existingPatient.setSysUserId(FhirProviderUtils.getSysUserId(request));
            patientService.save(existingPatient);

            org.hl7.fhir.r4.model.Patient resultFhirPatient = fhirTransformService
                    .transformToFhirPatient(existingPatient.getId());
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, resultFhirPatient,
                    this.getClass().getSimpleName(), method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Patient with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildUpdateOutcome(resultFhirPatient);

        } catch (ResourceNotFoundException | UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while updating Patient: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while updating Patient", e);
        }
    }

    @Delete
    public MethodOutcome delete(@IdParam IdType theId, HttpServletRequest request) {

        String method = "delete";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR DELETE request for Patient ID: " + (theId != null ? theId.getIdPart() : "null"));

        try {

            FhirProviderUtils.validateIdParam(theId, "Patient", this.getClass().getSimpleName(), method);

            Patient patient = getPatientByFhirId(theId.getIdPart());

            if (patient == null) {
                throw new ResourceNotFoundException("Patient/" + theId.getIdPart());
            }

            patient.setSysUserId(FhirProviderUtils.getSysUserId(request));
            patientService.save(patient);

            org.hl7.fhir.r4.model.Patient fhirPatientToSync = fhirTransformService
                    .transformToFhirPatient(patient.getId());
            fhirPatientToSync.setActive(false);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, fhirPatientToSync,
                    this.getClass().getSimpleName(), method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully deleted Patient with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildDeleteOutcome(theId, "Patient");

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while deleting Patient: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while deleting Patient", e);
        }
    }

    /**
     * Looks up a local Patient entity by its FHIR UUID. Uses the generic
     * {@code getAllMatching} from
     * {@link org.openelisglobal.common.service.BaseObjectService}.
     *
     * @param fhirUuid the FHIR UUID string
     * @return the matching Patient, or {@code null} if not found
     */
    private Patient getPatientByFhirId(String fhirUuid) {
        List<Patient> matches = patientService.getAllMatching("fhirUuid", UUID.fromString(fhirUuid));
        return matches.isEmpty() ? null : matches.get(0);
    }
}
