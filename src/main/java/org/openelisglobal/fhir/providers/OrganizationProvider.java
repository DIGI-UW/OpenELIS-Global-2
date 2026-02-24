package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrganizationProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirPersistanceService fhirPersistenceService;

    @Autowired
    private OrganizationService organizationService;

    private static final Class<org.hl7.fhir.r4.model.Organization> FHIR_RESOURCE_TYPE = org.hl7.fhir.r4.model.Organization.class;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return FHIR_RESOURCE_TYPE;
    }

    @Create
    public MethodOutcome create(@ResourceParam org.hl7.fhir.r4.model.Organization fhirOrganization,
            HttpServletRequest request) throws FhirLocalPersistingException {

        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR CREATE request for Organization");

        try {

            if (fhirOrganization == null) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Organization resource is null");
                throw new InvalidRequestException("Organization resource cannot be null");
            }

            Organization organization = fhirTransformService.transformToOrganization(fhirOrganization);
            organization.setSysUserId(ControllerUtills.getSysUserId(request));
            organization.setFhirUuid(UUID.randomUUID());
            Organization savedOrganization = organizationService.save(organization);

            org.hl7.fhir.r4.model.Organization resultFhirOrg = fhirTransformService
                    .transformToFhirOrganization(savedOrganization);

            try {
                fhirPersistenceService.updateFhirResourceInFhirStore(resultFhirOrg);
            } catch (Exception syncEx) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "FHIR store sync failed (continuing anyway): " + syncEx.getMessage());
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Organization with UUID: " + savedOrganization.getFhirUuidAsString());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(resultFhirOrg.getIdElement());
            outcome.setResource(resultFhirOrg);
            outcome.setCreated(true);
            outcome.setResponseStatusCode(201);

            return outcome;

        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while creating Organization: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while creating Organization", e);
        }
    }

    @Update
    public MethodOutcome update(@IdParam IdType theId,
            @ResourceParam org.hl7.fhir.r4.model.Organization fhirOrganization, HttpServletRequest request)
            throws FhirLocalPersistingException {

        String method = "update";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR UPDATE request for Organization ID: " + (theId != null ? theId.getIdPart() : "null"));

        try {

            if (theId == null || !theId.hasIdPart()) {

                LogEvent.logError(this.getClass().getSimpleName(), method, "Missing Organization ID for update");

                throw new InvalidRequestException("Organization ID must be provided for update");
            }

            fhirOrganization.setId(theId);

            Organization existingOrg = organizationService.getOrganizationByFhirId(theId.getIdPart());
            if (existingOrg == null) {
                throw new ResourceNotFoundException("Organization/" + theId.getIdPart());
            }

            existingOrg.setOrganizationName(fhirOrganization.getName());
            existingOrg.setIsActive(
                    Boolean.FALSE.equals(fhirOrganization.getActiveElement().getValue()) ? IActionConstants.NO
                            : IActionConstants.YES);
            existingOrg.setSysUserId(ControllerUtills.getSysUserId(request));
            Organization updatedOrg = organizationService.save(existingOrg);

            org.hl7.fhir.r4.model.Organization resultFhirOrg = fhirTransformService
                    .transformToFhirOrganization(updatedOrg);
            try {
                fhirPersistenceService.updateFhirResourceInFhirStore(resultFhirOrg);
            } catch (Exception syncEx) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "FHIR store sync failed during update (continuing anyway): " + syncEx.getMessage());
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Organization with ID: " + theId.getIdPart());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(resultFhirOrg.getIdElement());
            outcome.setResource(resultFhirOrg);
            outcome.setCreated(false);
            outcome.setResponseStatusCode(200);

            return outcome;

        } catch (UnprocessableEntityException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {

            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while updating Organization: " + e.getMessage());

            throw new InternalErrorException("Unexpected server error while updating Organization", e);
        }
    }

    @Delete
    public MethodOutcome delete(@IdParam IdType theId, HttpServletRequest request) {

        String method = "delete";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR DELETE request for Organization ID: " + (theId != null ? theId.getIdPart() : "null"));

        try {

            if (theId == null || !theId.hasIdPart()) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Missing Organization ID for delete");
                throw new InvalidRequestException("Organization ID must be provided for delete");
            }

            Organization organization = organizationService.getOrganizationByFhirId(theId.getIdPart());

            if (organization == null) {
                throw new ResourceNotFoundException("Organization/" + theId.getIdPart());
            }

            organization.setIsActive(IActionConstants.NO);
            organization.setSysUserId(ControllerUtills.getSysUserId(request));
            organizationService.save(organization);

            try {
                org.hl7.fhir.r4.model.Organization fhirOrgToSync = fhirTransformService
                        .transformToFhirOrganization(organization);
                fhirOrgToSync.setActive(false);
                fhirPersistenceService.updateFhirResourceInFhirStore(fhirOrgToSync);
            } catch (Exception syncEx) {
                LogEvent.logError(this.getClass().getSimpleName(), method,
                        "FHIR store sync failed during delete (continuing anyway): " + syncEx.getMessage());
            }

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully deleted Organization with ID: " + theId.getIdPart());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(theId);
            outcome.setResponseStatusCode(204);

            OperationOutcome operationOutcome = new OperationOutcome();
            operationOutcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION)
                    .setDiagnostics("Organization " + theId.getIdPart() + " has been deleted");
            outcome.setOperationOutcome(operationOutcome);

            return outcome;

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while deleting Organization: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while deleting Organization", e);
        }
    }
}
