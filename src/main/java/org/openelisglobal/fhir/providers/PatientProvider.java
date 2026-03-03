package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Create;
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
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.StaleObjectStateException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.validator.ValidatePatientInfo;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;

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
    private PatientIdentityService patientIdentityService;

    @Autowired
    private PatientContactService patientContactService;

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

    @Create
    public MethodOutcome createPatient(@ResourceParam org.hl7.fhir.r4.model.Patient fhirPatient,
            HttpServletRequest request) {
        String method = "Create";

        MethodOutcome outcome = new MethodOutcome();
        OperationOutcome operationOutcome = new OperationOutcome();

        try {

            if (fhirPatient == null) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Patient resource is null");
                throw new InvalidRequestException("Patient resource cannot be null");

            } else if (fhirPatient.getIdElement().getIdPart() == null) {
                fhirPatient.setId(UUID.randomUUID().toString());
            }

            PatientManagementInfo patientInfo = fhirTransformService.createOePatientManagementInfo(fhirPatient);

            if (StringUtils.isBlank(patientInfo.getPatientPK())) {
                patientInfo.setPatientUpdateStatus(PatientUpdateStatus.ADD);
            }

            Errors errors = new BindException(patientInfo, "patientInfo");
            ValidatePatientInfo.validatePatientInfo(errors, patientInfo);

            if (errors.hasErrors()) {
                for (ObjectError error : errors.getAllErrors()) {
                    operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR)
                            .setCode(OperationOutcome.IssueType.INVALID).setDiagnostics(error.getDefaultMessage());
                }

                outcome.setOperationOutcome(operationOutcome);
                outcome.setCreated(false);
                return outcome;
            }

            Patient patient = new Patient();
            preparePatientData(patientInfo, patient, request);
            fhirTransformService.addTelecomToPerson(fhirPatient.getTelecom(), patient.getPerson());

            patientInfo.getPatientContact().setPerson(patient.getPerson());

            patientService.persistPatientData(patientInfo, patient, FhirProviderUtils.getSysUserId(request));

            fhirTransformService.transformPersistPatient(patientInfo, true);
            Patient savedPatient = getPatientByFhirId(patient.getFhirUuidAsString());
            if (savedPatient == null) {
                throw new InternalErrorException("Saved patient is null");
            }

            org.hl7.fhir.r4.model.Patient savedFhirPatient = fhirTransformService
                    .transformToFhirPatient(savedPatient.getId());

            IdType id = new IdType("Patient", patient.getId());
            outcome.setId(id);
            outcome.setCreated(true);
            outcome.setResource(savedFhirPatient);

            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                    .setCode(OperationOutcome.IssueType.INFORMATIONAL).setDiagnostics("Patient created successfully");

            outcome.setOperationOutcome(operationOutcome);

        } catch (Exception e) {

            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setCode(OperationOutcome.IssueType.EXCEPTION).setDiagnostics(e.getMessage());

            outcome.setOperationOutcome(operationOutcome);
            outcome.setCreated(false);
        }

        return outcome;
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
    public MethodOutcome updatePatient(@IdParam IdType theId, @ResourceParam org.hl7.fhir.r4.model.Patient fhirPatient,
            HttpServletRequest request) {

        MethodOutcome outcome = new MethodOutcome();
        OperationOutcome operationOutcome = new OperationOutcome();

        try {

            if (theId == null || !theId.hasIdPart()) {
                throw new InvalidRequestException("Patient ID must be provided for update");
            }

            String fhirUuid = theId.getIdPart();

            Patient existingPatient = getPatientByFhirId(fhirUuid);

            if (existingPatient == null) {
                throw new ResourceNotFoundException("Patient/" + fhirUuid);
            }

            PatientManagementInfo patientInfo = fhirTransformService.createOePatientManagementInfo(fhirPatient);

            patientInfo.setPatientPK(existingPatient.getId());
            patientInfo.setPatientUpdateStatus(PatientUpdateStatus.UPDATE);

            Errors errors = new BindException(patientInfo, "patientInfo");
            ValidatePatientInfo.validatePatientInfo(errors, patientInfo);

            if (errors.hasErrors()) {
                for (ObjectError error : errors.getAllErrors()) {
                    operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR)
                            .setCode(OperationOutcome.IssueType.INVALID).setDiagnostics(error.getDefaultMessage());
                }
                outcome.setOperationOutcome(operationOutcome);
                return outcome;
            }

            Patient workingPatient = new Patient();

            preparePatientData(patientInfo, workingPatient, request);
            fhirTransformService.addTelecomToPerson(fhirPatient.getTelecom(), workingPatient.getPerson());
            PatientContact contact = patientContactService.getForPatient(workingPatient.getId()).get(0);
            contact.setPerson(workingPatient.getPerson());
            patientInfo.setPatientContact(contact);

            patientService.persistPatientData(patientInfo, workingPatient, FhirProviderUtils.getSysUserId(request));

            fhirTransformService.transformPersistPatient(patientInfo, false);

            Patient savedPatient = patientService.get(existingPatient.getId());

            org.hl7.fhir.r4.model.Patient savedFhirPatient = fhirTransformService
                    .transformToFhirPatient(savedPatient.getId());

            outcome.setId(new IdType("Patient", savedPatient.getFhirUuidAsString()));
            outcome.setCreated(false);
            outcome.setResource(savedFhirPatient);

            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                    .setCode(OperationOutcome.IssueType.INFORMATIONAL).setDiagnostics("Patient updated successfully");

            outcome.setOperationOutcome(operationOutcome);

        } catch (ResourceNotFoundException e) {
            throw e;

        } catch (StaleObjectStateException e) {

            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setCode(OperationOutcome.IssueType.CONFLICT)
                    .setDiagnostics("Patient was modified by another user");

            outcome.setOperationOutcome(operationOutcome);

        } catch (Exception e) {

            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setCode(OperationOutcome.IssueType.EXCEPTION).setDiagnostics(e.getMessage());

            outcome.setOperationOutcome(operationOutcome);
        }

        return outcome;
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

    private void preparePatientData(PatientManagementInfo patientInfo, Patient patient, HttpServletRequest request)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        String sysUserId = FhirProviderUtils.getSysUserId(request);

        if (patientInfo.getPatientUpdateStatus() == PatientUpdateStatus.UPDATE
                && StringUtils.isNotBlank(patientInfo.getPatientPK())) {

            Patient dbPatient = patientService.get(patientInfo.getPatientPK());

            if (dbPatient == null) {
                throw new IllegalStateException("Patient not found for update");
            }

            PropertyUtils.copyProperties(patient, dbPatient);

            List<PatientIdentity> identities = patientIdentityService.getPatientIdentitiesForPatient(dbPatient.getId());

            patientInfo.setPatientIdentities(identities != null ? identities : new ArrayList<>());
        }

        if (patient.getPerson() == null) {
            patient.setPerson(new Person());

        }

        PropertyUtils.copyProperties(patient, patientInfo);

        if (patient.getPerson() != null) {
            PropertyUtils.copyProperties(patient.getPerson(), patientInfo);
        }

        setAuditFields(patient, patientInfo, sysUserId);

        applyOptimisticLocking(patientInfo, patient);
    }

    private void setAuditFields(Patient patient, PatientManagementInfo patientInfo, String sysUserId) {

        patient.setSysUserId(sysUserId);

        if (patient.getPerson() != null) {
            patient.getPerson().setSysUserId(sysUserId);
        }

        if (patientInfo.getPatientIdentities() != null) {
            for (PatientIdentity identity : patientInfo.getPatientIdentities()) {
                identity.setSysUserId(sysUserId);
            }
        }

        if (patientInfo.getPatientContact() != null) {
            patientInfo.getPatientContact().setSysUserId(sysUserId);
        }
    }

    private void applyOptimisticLocking(PatientManagementInfo patientInfo, Patient patient) {

        if (patientInfo.getPatientUpdateStatus() != PatientUpdateStatus.UPDATE) {
            return;
        }

        if (StringUtils.isNotBlank(patientInfo.getPatientLastUpdated())) {
            patient.setLastupdated(Timestamp.valueOf(patientInfo.getPatientLastUpdated()));
        }

        if (StringUtils.isNotBlank(patientInfo.getPersonLastUpdated()) && patient.getPerson() != null) {

            patient.getPerson().setLastupdated(Timestamp.valueOf(patientInfo.getPersonLastUpdated()));
        }
    }
}
