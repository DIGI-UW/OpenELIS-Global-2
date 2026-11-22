package org.openelisglobal.organization.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.organization.valueholder.OrganizationChange;

public interface OrganizationChangeDAO extends BaseDAO<OrganizationChange, Integer> {

    /** The organization's entries, newest first. */
    List<OrganizationChange> getForOrganization(Integer organizationId);

    /** Entries that changed the name, for every organization, newest first. */
    List<OrganizationChange> getNameChanges();
}
