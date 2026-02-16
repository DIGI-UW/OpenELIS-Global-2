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
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
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

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Practitioner.class;
    }

    @Create
    public MethodOutcome create(@ResourceParam Practitioner practitioner) throws FhirLocalPersistingException {

        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR CREATE request for Practitioner");

        try {

            if (practitioner == null) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Practitioner resource is null");
                throw new InvalidRequestException("Practitioner resource cannot be null");
            }

            Provider provider = fhirTransformService.transformToProviderForPersistance(practitioner);
            providerService.insert(provider);

            practitioner.setId(new IdType("Practitioner", provider.getFhirUuidAsString()));

            try {
                fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);
            } catch (Exception syncEx) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "FHIR store sync failed (continuing anyway): " + syncEx.getMessage());
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Practitioner with UUID: " + provider.getFhirUuidAsString());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(practitioner.getIdElement());
            outcome.setResource(practitioner);
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
    public MethodOutcome update(@IdParam IdType theId, @ResourceParam Practitioner practitioner)
            throws FhirLocalPersistingException {

        String method = "update";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR UPDATE request for Practitioner ID: " + (theId != null ? theId.getIdPart() : "null"));

        try {

            if (theId == null || !theId.hasIdPart()) {

                LogEvent.logError(this.getClass().getSimpleName(), method, "Missing Practitioner ID for update");

                throw new InvalidRequestException("Practitioner ID must be provided for update");
            }

            practitioner.setId(theId);

            Provider provider = fhirTransformService.transformToProviderForUpdate(practitioner);
            providerService.save(provider);

            try {
                fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);
            } catch (Exception syncEx) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "FHIR store sync failed during update (continuing anyway): " + syncEx.getMessage());
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Practitioner with ID: " + theId.getIdPart());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(practitioner.getIdElement());
            outcome.setResource(practitioner);
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
