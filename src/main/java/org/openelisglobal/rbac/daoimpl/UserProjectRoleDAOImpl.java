package org.openelisglobal.rbac.daoimpl;

import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.rbac.dao.UserProjectRoleDAO;
import org.openelisglobal.userrole.valueholder.UserProjectRole;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserProjectRoleDAOImpl extends BaseDAOImpl<UserProjectRole, Integer>
        implements UserProjectRoleDAO {

    public UserProjectRoleDAOImpl() {
        super(UserProjectRole.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProjectRole> getBySystemUserId(Integer systemUserId) {
        String hql = "FROM UserProjectRole WHERE systemUserId = :userId AND active = true";
        return entityManager.createQuery(hql, UserProjectRole.class)
                .setParameter("userId", systemUserId)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserProjectRole(Integer systemUserId, String projectId, String roleName) {
        String hql = "SELECT COUNT(r) FROM UserProjectRole r WHERE r.systemUserId = :userId "
                + "AND r.projectId = :projectId AND r.roleName = :roleName AND r.active = true";
        Long count = entityManager.createQuery(hql, Long.class)
                .setParameter("userId", systemUserId)
                .setParameter("projectId", projectId)
                .setParameter("roleName", roleName)
                .getSingleResult();
        return count > 0;
    }
}
