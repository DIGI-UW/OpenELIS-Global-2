package org.openelisglobal.organization.service;

import java.util.List;
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
}
