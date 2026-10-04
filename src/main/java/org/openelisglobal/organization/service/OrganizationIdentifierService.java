package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationIdentifier;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(write = "PRIV_ORGANIZATION_MANAGE", read = "PRIV_ORGANIZATION_VIEW")
public interface OrganizationIdentifierService extends BaseObjectService<OrganizationIdentifier, Integer> {

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<OrganizationIdentifier> getForOrganization(Integer organizationId);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<OrganizationIdentifier> getForOrganizations(List<Integer> organizationIds);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<OrganizationIdentifier> getByLabelAndValue(String label, String value);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<OrganizationIdentifier> searchByValue(String text, int limit);

    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_VIEW')")
    List<String> getDistinctLabels();

    /**
     * Replaces the organization's identifiers with the given set and returns them
     * as stored.
     */
    @PreAuthorize("hasAuthority('PRIV_ORGANIZATION_MANAGE')")
    List<OrganizationIdentifier> replaceForOrganization(Integer organizationId, List<OrganizationIdentifier> wanted,
            String sysUserId);
}
