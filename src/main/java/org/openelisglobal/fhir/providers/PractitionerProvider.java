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
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PractitionerProvider implements IResourceProvider {

    @Autowired
    private FhirUtil util;

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

    @Read
    public Practitioner getPractitionerByUUID(@IdParam IdType theId) {
        String method = "Read";
        try {
            if (theId == null || !theId.hasIdPart()) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Missing Practitioner ID for Read");
                throw new InvalidRequestException("Practitioner ID must be provided for Read");
            }
            Provider provider = providerService.getProviderByFhirId(UUID.fromString(theId.getIdPart()));
            if (provider == null) {
                throw new ResourceNotFoundException("Provider is null " + theId.getIdPart());
            }
            Practitioner practitioner = fhirTransformService.transformProviderToPractitioner(provider);
            return practitioner;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading Practitioner: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Practitioner", e);

        }

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
            provider.getPerson().setSysUserId(FhirProviderUtils.getSysUserId(request));
            Person savedPerson = personService.save(provider.getPerson());
            provider.setPerson(savedPerson);

            Provider providerTosave = providerService.save(provider);

            Practitioner practitionerToSave = fhirTransformService.transformProviderToPractitioner(providerTosave);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, practitionerToSave,
                    this.getClass().getSimpleName(), method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Practitioner with UUID: " + provider.getFhirUuidAsString());

            return FhirProviderUtils.buildCreateOutcome(practitionerToSave);

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

            FhirProviderUtils.validateIdParam(theId, "Practitioner", this.getClass().getSimpleName(), method);

            practitioner.setId(theId);

            Provider provider = providerService
                    .getProviderByFhirId(UUID.fromString(practitioner.getIdElement().getIdPart()));
            Person existingPerson = personService.get(provider.getPerson().getId());

            fhirTransformService.addHumanNameToPerson(practitioner.getNameFirstRep(), existingPerson);
            fhirTransformService.addTelecomToPerson(practitioner.getTelecom(), existingPerson);
            existingPerson.setSysUserId(FhirProviderUtils.getSysUserId(request));
            Person updatedPerson = personService.save(existingPerson);
            provider.setPerson(updatedPerson);
            Provider providerToUpdate = providerService.save(provider);
            Practitioner practitionerToSave = fhirTransformService.transformProviderToPractitioner(providerToUpdate);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, practitionerToSave,
                    this.getClass().getSimpleName(), method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Practitioner with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildUpdateOutcome(practitionerToSave);

        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while updating Practitioner: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while updating Practitioner", e);
        }
    }

    @Delete
    public MethodOutcome delete(@IdParam IdType theId, HttpServletRequest request) {

        String method = "delete";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR DELETE request for Practitioner ID: " + (theId != null ? theId.getIdPart() : "null"));

        try {

            FhirProviderUtils.validateIdParam(theId, "Practitioner", this.getClass().getSimpleName(), method);

            Provider provider = providerService.getProviderByFhirId(UUID.fromString(theId.getIdPart()));

            if (provider == null) {
                throw new ResourceNotFoundException("Practitioner/" + theId.getIdPart());
            }

            provider.setActive(false);
            provider.setSysUserId(FhirProviderUtils.getSysUserId(request));
            providerService.save(provider);

            Practitioner practitionerToDelete = fhirTransformService.transformProviderToPractitioner(provider);
            practitionerToDelete.setActive(false);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, practitionerToDelete,
                    this.getClass().getSimpleName(), method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully deleted Practitioner with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildDeleteOutcome(theId, "Practitioner");

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while deleting Practitioner: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while deleting Practitioner", e);
        }
    }

    @Search
    public Bundle searchPractitionerBundle(
            @OptionalParam(name = Practitioner.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Practitioner.SP_GIVEN) StringAndListParam given,
            @OptionalParam(name = Practitioner.SP_FAMILY) StringAndListParam family,
            @OptionalParam(name = Practitioner.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,
            @IncludeParam(reverse = true, allow = { "Encounter:" + Encounter.SP_PARTICIPANT,
                    "ServiceRequest:" + ServiceRequest.SP_REQUESTER, }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        String methodName = "searchPractitionerBundle";
        LogEvent.logDebug(this.getClass().getSimpleName(), methodName,
                "Searching for Practitioners (returning Bundle)");

        try {

            Bundle bundle = util.forwardSearchToFhirStore(request);

            return bundle;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Practitioners: " + e.getMessage());
            throw new InternalErrorException("Error searching Practitioners", e);
        }
    }
}
