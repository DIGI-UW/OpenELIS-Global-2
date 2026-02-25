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
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
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
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
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
    private FhirUtil util;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private OrganizationService organizationService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Organization.class;
    }

    @Read
    public Organization readOrganization(@IdParam IdType theId) {
        String method = "Read";
        try {
            if (theId == null || !theId.hasIdPart()) {
                LogEvent.logError(this.getClass().getSimpleName(), method, "Missing Practitioner ID for Read");
                throw new InvalidRequestException("Organization ID must be provided for Read");
            }
            org.openelisglobal.organization.valueholder.Organization organization = organizationService
                    .getOrganizationByFhirId(theId.getIdPart());
            if (organization == null) {
                throw new ResourceNotFoundException("Organization is null " + theId.getIdPart());
            }
            Organization fhirOrganization = fhirTransformService.transformToFhirOrganization(organization);
            return fhirOrganization;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading Organization: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Organization", e);

        }

    }

    @Create
    public MethodOutcome create(@ResourceParam Organization fhirOrganization, HttpServletRequest request)
            throws FhirLocalPersistingException {

        String method = "create";
        LogEvent.logDebug(this.getClass().getSimpleName(), method, "Received FHIR CREATE request for Organization");

        try {

            if (fhirOrganization == null) {
                throw new InvalidRequestException("Organization resource cannot be null");
            }

            if (!fhirOrganization.hasId()) {
                fhirOrganization.setId(UUID.randomUUID().toString());
            }

            org.openelisglobal.organization.valueholder.Organization organization = fhirTransformService
                    .transformToOrganization(fhirOrganization);

            org.openelisglobal.organization.valueholder.Organization savedOrganization = organizationService
                    .save(organization);
            if (savedOrganization == null) {
                throw new InternalErrorException("Failed to save organization");
            }

            fhirTransformService.transformPersistOrganization(savedOrganization);

            Organization response = fhirTransformService.transformToFhirOrganization(savedOrganization);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully created Organization: " + savedOrganization.getFhirUuidAsString());

            return FhirProviderUtils.buildCreateOutcome(response);

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

            org.openelisglobal.organization.valueholder.Organization incomingOrg = fhirTransformService
                    .transformToOrganization(fhirOrganization);
            existingOrg.setOrganizationName(incomingOrg.getOrganizationName());
            existingOrg.setIsActive(incomingOrg.getIsActive());
            existingOrg.setSysUserId(FhirProviderUtils.getSysUserId(request));
            org.openelisglobal.organization.valueholder.Organization updatedOrg = organizationService.save(existingOrg);
            if (updatedOrg == null) {
                throw new InternalErrorException("Failed to save updated organization is null");
            }

            fhirTransformService.transformPersistOrganization(updatedOrg);

            Organization FhirOrg = fhirTransformService.transformToFhirOrganization(updatedOrg);

            LogEvent.logInfo(this.getClass().getSimpleName(), method,
                    "Successfully updated Organization with ID: " + theId.getIdPart());

            return FhirProviderUtils.buildUpdateOutcome(FhirOrg);

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
            org.openelisglobal.organization.valueholder.Organization deletedOrg = organizationService
                    .save(organization);

            fhirTransformService.transformPersistOrganization(deletedOrg);

            Organization fhirOrgToSync = fhirTransformService.transformToFhirOrganization(deletedOrg);
            fhirOrgToSync.setActive(false);

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

    @Search
    public Bundle searchPractitionerBundle(
            @OptionalParam(name = Practitioner.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Organization.SP_NAME) StringParam name,
            @OptionalParam(name = Organization.SP_ACTIVE) TokenParam active,
            @OptionalParam(name = Organization.SP_TYPE) TokenAndListParam type,
            @OptionalParam(name = Organization.SP_ADDRESS) StringParam address,
            @OptionalParam(name = Organization.SP_ADDRESS_CITY) StringParam addressCity,
            @OptionalParam(name = Organization.SP_ADDRESS_STATE) StringParam addressState,
            @OptionalParam(name = Organization.SP_ADDRESS_POSTALCODE) StringParam addressPostalCode,
            @OptionalParam(name = Organization.SP_ADDRESS_COUNTRY) StringParam addressCountry,
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
