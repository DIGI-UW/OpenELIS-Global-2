package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.Organization;
import org.springframework.security.access.prepost.PreAuthorize;

public interface OrganizationService extends BaseObjectService<Organization, String> {

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    void getData(Organization organization);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Organization getActiveOrganizationByName(Organization organization, boolean ignoreCase);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getOrganizationsByParentId(String parentId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getOrganizationsByTypeName(String orderByProperty, String[] typeName);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Integer getTotalOrganizationCount();

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getAllOrganizations();

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getPagesOfSearchedOrganizations(int startRecNo, String searchString);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Organization getOrganizationById(String organizationId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getPageOfOrganizations(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getOrganizations(String filter);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getOrganizationsByTypeNameAndLeadingChars(String partialName, String typeName);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Organization getOrganizationByLocalAbbreviation(Organization organization, boolean ignoreCase);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Integer getTotalSearchedOrganizationCount(String searchString);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    void linkOrganizationAndType(Organization organization, String typeId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<String> getTypeIdsForOrganizationId(String id);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    void deleteAllLinksForOrganization(String id);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getOrganizationsByTypeName(String orderByProperty, String referralOrgType);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    void activateOrganizationsAndDeactivateOthers(List<String> organizationNames);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    void deactivateAllOrganizations();

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    void activateOrganizations(List<String> organizationNames);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    void deactivateOrganizations(List<Organization> organizations);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Organization getOrganizationByName(Organization organization, boolean ignoreCase);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Organization getOrganizationByShortName(String shortName, boolean ignoreCase);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> getActiveOrganizations();

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Organization getOrganizationByFhirId(String idPart);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    Organization getOrganizationByCode(String code);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<Organization> searchOrganizationsWithTypes(String filter);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    String generateSiteCode();
}
