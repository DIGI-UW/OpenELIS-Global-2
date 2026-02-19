package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformServiceImpl;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PractitionerProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirPersistanceService fhirPersistenceService;

    @Autowired
    private ProviderService providerService;
    @Autowired
    private PersonService personService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Practitioner.class;
    }

    @Create
    public MethodOutcome create(@ResourceParam Practitioner practitioner, HttpServletRequest request)
            throws FhirLocalPersistingException {

        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR CREATE request for Practitioner");

        try {

            if (practitioner == null) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Practitioner resource is null");
                throw new InvalidRequestException("Practitioner resource cannot be null");

            } else if (practitioner.getIdElement().getIdPart() == null) {
                practitioner.setId(UUID.randomUUID().toString());
            }

            Provider provider = fhirTransformService.transformToProvider(practitioner);
            provider.getPerson().setSysUserId(ControllerUtills.getSysUserId(request));
            Person savedPerson = personService.save(provider.getPerson());
            provider.setPerson(savedPerson);

            Provider providerTosave = providerService.save(provider);

            Practitioner practitionerToSave = fhirTransformService.transformProviderToPractitioner(providerTosave);

            try {
                fhirPersistenceService.updateFhirResourceInFhirStore(practitionerToSave);
            } catch (Exception syncEx) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "FHIR store sync failed (continuing anyway): " + syncEx.getMessage());
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Practitioner with UUID: " + provider.getFhirUuidAsString());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(practitionerToSave.getIdElement());
            outcome.setResource(practitionerToSave);
            outcome.setCreated(true);
            outcome.setResponseStatusCode(201);

            return outcome;

        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Practitioner: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while creating Practitioner", e);
        }
    }

    @Update
    public MethodOutcome update(@IdParam IdType theId, @ResourceParam Practitioner practitioner,
            HttpServletRequest request) throws FhirLocalPersistingException {

        String method = "update";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR UPDATE request for Practitioner ID: " + (theId != null ? theId.getIdPart() : "null"));

        try {

            if (theId == null || !theId.hasIdPart()) {

                LogEvent.logError(this.getClass().getSimpleName(), method, "Missing Practitioner ID for update");

                throw new InvalidRequestException("Practitioner ID must be provided for update");
            }

            practitioner.setId(theId);

            Provider provider = providerService
                    .getProviderByFhirId(UUID.fromString(practitioner.getIdElement().getIdPart()));
            Person existingPerson = personService.get(provider.getPerson().getId());

            FhirTransformServiceImpl transForm = new FhirTransformServiceImpl();
            transForm.addHumanNameToPerson(practitioner.getNameFirstRep(), existingPerson);
            transForm.addTelecomToPerson(practitioner.getTelecom(), existingPerson);
            existingPerson.setSysUserId(ControllerUtills.getSysUserId(request));
            Person updatedPerson = personService.save(existingPerson);
            provider.setPerson(updatedPerson);
            Provider providerToUpdate = providerService.save(provider);
            Practitioner practitionerToSave = fhirTransformService.transformProviderToPractitioner(providerToUpdate);
            try {
                fhirPersistenceService.updateFhirResourceInFhirStore(practitionerToSave);
            } catch (Exception syncEx) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "FHIR store sync failed during update (continuing anyway): " + syncEx.getMessage());
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Practitioner with ID: " + theId.getIdPart());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(practitionerToSave.getIdElement());
            outcome.setResource(practitionerToSave);
            outcome.setCreated(false);
            outcome.setResponseStatusCode(200);

            return outcome;

        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while updating Practitioner: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while updating Practitioner", e);
        }
    }

}
