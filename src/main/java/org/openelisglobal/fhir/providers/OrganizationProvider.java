package org.openelisglobal.fhir.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.OrganizationSearchParams;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.search.service.OrganizationSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Read and search Organization resources from OpenELIS. */
@Component
public class OrganizationProvider implements IResourceProvider {

    @Autowired
    private OrganizationSearchService organizationSearchService;

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
            FhirProviderUtils.requireUuidId(theId, "Organization");
            org.openelisglobal.organization.valueholder.Organization organization = organizationService
                    .getOrganizationByFhirId(theId.getIdPart());
            if (organization == null) {
                throw new ResourceNotFoundException("Organization is null " + theId.getIdPart());
            }
            Organization fhirOrganization = fhirTransformService.transformToFhirOrganization(organization);
            return fhirOrganization;
        } catch (ResourceNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Organization", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), method,
                    "Unexpected error while Reading Organization: " + e.getMessage());
            throw new InternalErrorException("Unexpected server error while Reading Organization", e);

        }

    }

    @Search
    public IBundleProvider searchOrganizations(@OptionalParam(name = Organization.SP_RES_ID) TokenAndListParam id,
            @OptionalParam(name = Organization.SP_IDENTIFIER) TokenAndListParam identifier,
            @OptionalParam(name = Organization.SP_NAME) StringAndListParam name,
            @OptionalParam(name = Organization.SP_ACTIVE) TokenAndListParam active,
            @OptionalParam(name = Organization.SP_TYPE) TokenAndListParam type,
            @OptionalParam(name = Organization.SP_PARTOF) ReferenceAndListParam partOf,
            @OptionalParam(name = Organization.SP_ADDRESS_CITY) StringAndListParam addressCity,
            @OptionalParam(name = Organization.SP_ADDRESS_STATE) StringAndListParam addressState,
            @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort,
            @Offset Integer offset, @Count Integer count,
            @IncludeParam(allow = { FhirConstants.ORGANIZATION_PARTOF_INCLUDE }) HashSet<Include> includes,
            @IncludeParam(reverse = true, allow = {
                    FhirConstants.ORGANIZATION_PARTOF_INCLUDE }) HashSet<Include> revIncludes,
            HttpServletRequest request) {

        String methodName = "searchOrganizations";
        LogEvent.logDebug(this.getClass().getSimpleName(), methodName, "Searching for Organizations");

        try {
            OrganizationSearchParams params = new OrganizationSearchParams(id, identifier, name, active, type, partOf,
                    addressCity, addressState, lastUpdated, sort, includes, revIncludes);
            return FhirProviderUtils.withPaging(organizationSearchService.searchOrganizations(params), offset, count);
        } catch (InvalidRequestException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Invalid Organization search parameter: " + e.getMessage());
            throw new InvalidRequestException("Invalid Organization search parameter: " + e.getMessage(), e);
        } catch (Exception e) {
            if (FhirProviderUtils.isDataError(e)) {
                throw FhirProviderUtils.unprocessableData("Organization", e);
            }
            LogEvent.logError(this.getClass().getSimpleName(), methodName,
                    "Error searching Organizations: " + e.getMessage());
            throw new InternalErrorException("Error searching Organizations", e);
        }
    }
}
