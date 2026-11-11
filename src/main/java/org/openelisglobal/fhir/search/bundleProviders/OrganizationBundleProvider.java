package org.openelisglobal.fhir.search.bundleProviders;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntrySearchModeEnum;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.exception.FhirTransformationException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.fhir.FhirConstants;
import org.openelisglobal.fhir.search.searchparams.OrganizationSearchParams;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.search.dao.OrganizationSearchDao;

/**
 * Pages OpenELIS organizations as FHIR Organization resources, adding parents
 * for {@code _include=Organization:partof} and children for
 * {@code _revinclude=Organization:partof}.
 */
public class OrganizationBundleProvider
        extends BaseFhirBundleProvider<Organization, org.hl7.fhir.r4.model.Organization> {

    private final OrganizationSearchParams searchParams;
    private final OrganizationSearchDao organizationSearchDao;
    private final FhirTransformService fhirTransformService;

    public OrganizationBundleProvider(OrganizationSearchParams searchParams,
            OrganizationSearchDao organizationSearchDao, FhirTransformService fhirTransformService) {
        this.searchParams = Objects.requireNonNull(searchParams, "OrganizationSearchParams must not be null");
        this.organizationSearchDao = Objects.requireNonNull(organizationSearchDao,
                "OrganizationSearchDao must not be null");
        this.fhirTransformService = Objects.requireNonNull(fhirTransformService,
                "FhirTransformService must not be null");
    }

    @Override
    protected List<Organization> loadEntities(int offset, int pageSize) {
        return organizationSearchDao.search(searchParams, offset, pageSize);
    }

    @Override
    protected long countEntities() {
        return organizationSearchDao.count(searchParams);
    }

    @Override
    protected org.hl7.fhir.r4.model.Organization transformEntity(Organization organization) {
        try {
            return fhirTransformService.transformToFhirOrganization(organization);
        } catch (FhirTransformationException e) {
            LogEvent.logWarn(getClass().getSimpleName(), "transformEntity",
                    "skipping Organization " + organization.getId() + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<IBaseResource> getResources(int fromIndex, int toIndex) {

        int offset = effectiveOffset(fromIndex);
        int pageSize = effectivePageSize(fromIndex, toIndex);
        if (pageSize <= 0) {
            return List.of();
        }

        List<Organization> matches = loadEntities(offset, pageSize);
        List<IBaseResource> resources = new ArrayList<>();
        for (Organization organization : matches) {
            org.hl7.fhir.r4.model.Organization resource = transformEntity(organization);
            if (resource != null) {
                ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(resource, BundleEntrySearchModeEnum.MATCH);
                resources.add(resource);
            }
        }
        if (matches.isEmpty()) {
            return resources;
        }

        Map<String, Organization> included = new LinkedHashMap<>();
        if (searchParams.hasInclude(FhirConstants.ORGANIZATION_PARTOF_INCLUDE)) {
            for (Organization organization : matches) {
                Organization parent = organization.getOrganization();
                if (parent != null && parent.getId() != null) {
                    included.putIfAbsent(parent.getId(), parent);
                }
            }
        }
        if (searchParams.hasRevInclude(FhirConstants.ORGANIZATION_PARTOF_INCLUDE)) {
            List<String> matchIds = matches.stream().map(Organization::getId).toList();
            for (Organization child : organizationSearchDao.findByParentIds(matchIds)) {
                included.putIfAbsent(child.getId(), child);
            }
        }
        matches.forEach(organization -> included.remove(organization.getId()));

        for (Organization organization : included.values()) {
            org.hl7.fhir.r4.model.Organization resource = transformEntity(organization);
            if (resource != null) {
                ResourceMetadataKeyEnum.ENTRY_SEARCH_MODE.put(resource, BundleEntrySearchModeEnum.INCLUDE);
                resources.add(resource);
            }
        }
        return resources;
    }
}
