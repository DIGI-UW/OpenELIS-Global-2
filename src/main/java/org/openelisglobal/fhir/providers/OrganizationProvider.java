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
import org.hl7.fhir.r4.model.Organization;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.organization.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * HAPI FHIR resource provider for the Organization resource. Handles Create,
 * Update, and Delete operations against the local OpenELIS database with
 * synchronization to the FHIR store.
 *
 * <p>
 * Auto-discovered by {@link org.openelisglobal.fhir.servlets.FhirRestfulServer}
 * as a Spring {@code @Component} implementing {@link IResourceProvider}.
 */
@Component
public class OrganizationProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private FhirPersistanceService fhirPersistenceService;

    @Autowired
    private OrganizationService organizationService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Organization.class;
    }

    @Create
    public MethodOutcome create(@ResourceParam Organization fhirOrganization, HttpServletRequest request)
            throws FhirLocalPersistingException {

        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR CREATE request for Organization");

        try {

            if (fhirOrganization == null) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Organization resource is null");
                throw new InvalidRequestException("Organization resource cannot be null");
            }

            org.openelisglobal.organization.valueholder.Organization organization = fhirTransformService
                    .transformToOrganization(fhirOrganization);
            organization.setSysUserId(ControllerUtills.getSysUserId(request));
            organization.setFhirUuid(UUID.randomUUID());
            org.openelisglobal.organization.valueholder.Organization savedOrganization = organizationService
                    .save(organization);

            Organization resultFhirOrg = fhirTransformService.transformToFhirOrganization(savedOrganization);
            syncToFhirStore(resultFhirOrg, method);

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
    public MethodOutcome update(@IdParam IdType theId, @ResourceParam Organization fhirOrganization,
            HttpServletRequest request) throws FhirLocalPersistingException {

        String method = "update";
        LogEvent.logDebug(this.getClass().getSimpleName(), method,
                "Received FHIR UPDATE request for Organization ID: " + (theId != null ? theId.getIdPart() : "null"));

        try {

            if (theId == null || !theId.hasIdPart()) {

                LogEvent.logError(this.getClass().getSimpleName(), method, "Missing Organization ID for update");

                throw new InvalidRequestException("Organization ID must be provided for update");
            }

            fhirOrganization.setId(theId);

            org.openelisglobal.organization.valueholder.Organization existingOrg = organizationService
                    .getOrganizationByFhirId(theId.getIdPart());
            if (existingOrg == null) {
                throw new ResourceNotFoundException("Organization/" + theId.getIdPart());
            }

            // Use the transform to extract all fields from the FHIR resource, then merge
            // into the existing entity to preserve its database identity and fhirUuid.
            org.openelisglobal.organization.valueholder.Organization incomingOrg = fhirTransformService
                    .transformToOrganization(fhirOrganization);
            existingOrg.setOrganizationName(incomingOrg.getOrganizationName());
            existingOrg.setIsActive(incomingOrg.getIsActive());
            existingOrg.setSysUserId(ControllerUtills.getSysUserId(request));
            org.openelisglobal.organization.valueholder.Organization updatedOrg = organizationService.save(existingOrg);

            Organization resultFhirOrg = fhirTransformService.transformToFhirOrganization(updatedOrg);
            syncToFhirStore(resultFhirOrg, method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Organization with ID: " + theId.getIdPart());

            MethodOutcome outcome = new MethodOutcome();
            outcome.setId(resultFhirOrg.getIdElement());
            outcome.setResource(resultFhirOrg);
            outcome.setCreated(false);
            outcome.setResponseStatusCode(200);

            return outcome;

        } catch (ResourceNotFoundException | UnprocessableEntityException | InvalidRequestException e) {
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

            org.openelisglobal.organization.valueholder.Organization organization = organizationService
                    .getOrganizationByFhirId(theId.getIdPart());

            if (organization == null) {
                throw new ResourceNotFoundException("Organization/" + theId.getIdPart());
            }

            organization.setIsActive(IActionConstants.NO);
            organization.setSysUserId(ControllerUtills.getSysUserId(request));
            organizationService.save(organization);

            Organization fhirOrgToSync = fhirTransformService.transformToFhirOrganization(organization);
            fhirOrgToSync.setActive(false);
            syncToFhirStore(fhirOrgToSync, method);

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

    private void syncToFhirStore(Organization fhirOrg, String callingMethod) {
        try {
            fhirPersistenceService.updateFhirResourceInFhirStore(fhirOrg);
        } catch (Exception syncEx) {
            LogEvent.logError(this.getClass().getSimpleName(), callingMethod,
                    "FHIR store sync failed (continuing anyway): " + syncEx.getMessage());
        }
    }
}
