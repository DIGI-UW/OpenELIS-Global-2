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
import org.hl7.fhir.r4.model.OperationOutcome;
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
    public MethodOutcome create(@ResourceParam Practitioner practitioner) {

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

            fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Practitioner with UUID: " + provider.getFhirUuidAsString());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(practitioner.getIdElement());
            outcome.setResource(practitioner);
            outcome.setCreated(true);
            outcome.setResponseStatusCode(201);

            return outcome;

        } catch (FhirLocalPersistingException e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Persistence error while creating Practitioner: " + e.getMessage());

            throw buildUnprocessableEntity("Failed to persist Practitioner: " + e.getMessage());

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Practitioner: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while creating Practitioner", e);
        }
    }

    @Update
    public MethodOutcome update(@IdParam IdType theId, @ResourceParam Practitioner practitioner) {

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

            fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Practitioner with ID: " + theId.getIdPart());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(practitioner.getIdElement());
            outcome.setResource(practitioner);
            outcome.setCreated(false);
            outcome.setResponseStatusCode(200);

            return outcome;

        } catch (FhirLocalPersistingException e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Persistence error while updating Practitioner: " + e.getMessage());

            throw buildUnprocessableEntity("Failed to update Practitioner: " + e.getMessage());

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while updating Practitioner: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while updating Practitioner", e);
        }
    }

    private UnprocessableEntityException buildUnprocessableEntity(String message) {

        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.PROCESSING).setDiagnostics(message);

        return new UnprocessableEntityException(message, outcome);
    }
}
