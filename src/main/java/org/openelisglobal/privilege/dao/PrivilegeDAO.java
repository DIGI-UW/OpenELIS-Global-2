package org.openelisglobal.privilege.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.privilege.valueholder.Privilege;

public interface PrivilegeDAO extends BaseDAO<Privilege, Integer> {

    /**
     * Returns all active privileges directly assigned to the given role.
     *
     * @param roleId the numeric PK of the system role
     * @return list of active {@link Privilege} records for the role; never null
     */
    List<Privilege> getPrivilegesForRole(Integer roleId);

    /**
     * Replaces the DIRECT privilege grants of a role with exactly
     * {@code privilegeIds}. Inherited privileges are untouched — they belong to the
     * parent role, not this one.
     *
     * @return the number of grant rows after the replacement
     */
    int replacePrivilegesForRole(Integer roleId, java.util.Collection<Integer> privilegeIds);
}
