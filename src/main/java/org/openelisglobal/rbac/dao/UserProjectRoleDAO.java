package org.openelisglobal.rbac.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.userrole.valueholder.UserProjectRole;

public interface UserProjectRoleDAO extends BaseDAO<UserProjectRole, Integer> {

    List<UserProjectRole> getBySystemUserId(Integer systemUserId);

    boolean existsByUserProjectRole(Integer systemUserId, String projectId, String roleName);
}
