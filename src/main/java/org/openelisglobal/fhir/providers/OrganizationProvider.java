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
import org.hl7.fhir.r4.model.Organization;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
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
            organization.setSysUserId(FhirProviderUtils.getSysUserId(request));
            organization.setFhirUuid(UUID.randomUUID());
            org.openelisglobal.organization.valueholder.Organization savedOrganization = organizationService
                    .save(organization);

            Organization resultFhirOrg = fhirTransformService.transformToFhirOrganization(savedOrganization);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, resultFhirOrg, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Organization with UUID: " + savedOrganization.getFhirUuidAsString());

            return FhirProviderUtils.buildCreateOutcome(resultFhirOrg);

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

            FhirProviderUtils.validateIdParam(theId, "Organization", this.getClass().getSimpleName(), method);

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
            existingOrg.setSysUserId(FhirProviderUtils.getSysUserId(request));
            org.openelisglobal.organization.valueholder.Organization updatedOrg = organizationService.save(existingOrg);

            Organization resultFhirOrg = fhirTransformService.transformToFhirOrganization(updatedOrg);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, resultFhirOrg, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Organization with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildUpdateOutcome(resultFhirOrg);

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

            FhirProviderUtils.validateIdParam(theId, "Organization", this.getClass().getSimpleName(), method);

            org.openelisglobal.organization.valueholder.Organization organization = organizationService
                    .getOrganizationByFhirId(theId.getIdPart());

            if (organization == null) {
                throw new ResourceNotFoundException("Organization/" + theId.getIdPart());
            }

            organization.setIsActive(IActionConstants.NO);
            organization.setSysUserId(FhirProviderUtils.getSysUserId(request));
            organizationService.save(organization);

            Organization fhirOrgToSync = fhirTransformService.transformToFhirOrganization(organization);
            fhirOrgToSync.setActive(false);
            FhirProviderUtils.syncToFhirStore(fhirPersistenceService, fhirOrgToSync, this.getClass().getSimpleName(),
                    method);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully deleted Organization with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildDeleteOutcome(theId, "Organization");

        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while deleting Organization: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while deleting Organization", e);
        }
    }
}
