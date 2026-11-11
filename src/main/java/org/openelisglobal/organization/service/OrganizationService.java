package org.openelisglobal.organization.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.Organization;

public interface OrganizationService extends BaseObjectService<Organization, String> {
    void getData(Organization organization);

    Organization getActiveOrganizationByName(Organization organization, boolean ignoreCase);

    List<Organization> getOrganizationsByParentId(String parentId);

    List<Organization> getOrganizationsByTypeName(String orderByProperty, String[] typeName);

    Integer getTotalOrganizationCount();

    List<Organization> getAllOrganizations();

    List<Organization> getPagesOfSearchedOrganizations(int startRecNo, String searchString);

    Organization getOrganizationById(String organizationId);

    List<Organization> getPageOfOrganizations(int startingRecNo);

    List<Organization> getOrganizations(String filter);

    List<Organization> getOrganizationsByTypeNameAndLeadingChars(String partialName, String typeName);

    Organization getOrganizationByLocalAbbreviation(Organization organization, boolean ignoreCase);

    Integer getTotalSearchedOrganizationCount(String searchString);

    void linkOrganizationAndType(Organization organization, String typeId);

    List<String> getTypeIdsForOrganizationId(String id);

    void deleteAllLinksForOrganization(String id);

    List<Organization> getOrganizationsByTypeName(String orderByProperty, String referralOrgType);

    void activateOrganizationsAndDeactivateOthers(List<String> organizationNames);

    void deactivateAllOrganizations();

    void activateOrganizations(List<String> organizationNames);

    void deactivateOrganizations(List<Organization> organizations);

    Organization getOrganizationByName(Organization organization, boolean ignoreCase);

    Organization getOrganizationByShortName(String shortName, boolean ignoreCase);

    List<Organization> getActiveOrganizations();

    Organization getOrganizationByFhirId(String idPart);

    Organization getOrganizationByCode(String code);

    /**
     * Search organizations by name with eager loading of organization types and
     * parent organizations. Optimized for address hierarchy search.
     *
     * @param filter Search filter for organization name
     * @return List of organizations with types eagerly loaded
     */
    List<Organization> searchOrganizationsWithTypes(String filter);

    /**
     * Inserts without the duplicate-name refusal; the Locations menu reports a
     * duplicate name as a warning the admin can accept (OGC-1363 FR-C4).
     */
    String insertUnchecked(Organization organization);

    /** Updates without the duplicate-name refusal (OGC-1363 FR-C4). */
    Organization updateUnchecked(Organization organization);

    List<Organization> getAllWithTypes();

    List<Organization> getChildrenWithTypes(String parentId);

    List<Organization> getByTypeIdWithTypes(String typeId);

    Map<String, Long> countActiveChildren(Collection<String> parentIds);

    List<Organization> searchAreas(String text, int limit);

    /**
     * Deactivates the organizations the given source supplied (the facility
     * registry sync, before it re-applies the registry's current list), leaving
     * local records alone (OGC-1363 FR-H4).
     */
    void deactivateOrganizationsFromSource(String source);
}
