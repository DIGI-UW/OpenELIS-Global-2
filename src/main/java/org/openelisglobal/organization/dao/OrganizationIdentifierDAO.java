package org.openelisglobal.organization.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.organization.valueholder.OrganizationIdentifier;

public interface OrganizationIdentifierDAO extends BaseDAO<OrganizationIdentifier, Integer> {

    List<OrganizationIdentifier> getForOrganization(Integer organizationId);

    List<OrganizationIdentifier> getForOrganizations(List<Integer> organizationIds);

    /** Identifiers carrying this label and value, case-insensitively. */
    List<OrganizationIdentifier> getByLabelAndValue(String label, String value);

    /** Identifiers whose value contains the text, case-insensitively. */
    List<OrganizationIdentifier> searchByValue(String text, int limit);

    List<String> getDistinctLabels();

    void deleteForOrganization(Integer organizationId);
}
