package org.openelisglobal.organization.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.organization.valueholder.OrganizationIdentifier;

public interface OrganizationIdentifierService extends BaseObjectService<OrganizationIdentifier, Integer> {

    List<OrganizationIdentifier> getForOrganization(Integer organizationId);

    List<OrganizationIdentifier> getForOrganizations(List<Integer> organizationIds);

    List<OrganizationIdentifier> getByLabelAndValue(String label, String value);

    List<OrganizationIdentifier> searchByValue(String text, int limit);

    List<String> getDistinctLabels();

    /**
     * Replaces the organization's identifiers with the given set and returns them
     * as stored.
     */
    List<OrganizationIdentifier> replaceForOrganization(Integer organizationId, List<OrganizationIdentifier> wanted,
            String sysUserId);
}
