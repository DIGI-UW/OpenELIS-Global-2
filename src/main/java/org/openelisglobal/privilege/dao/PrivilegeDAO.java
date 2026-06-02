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
}
