package org.openelisglobal.privilege.daoimpl;

import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.privilege.dao.PrivilegeDAO;
import org.openelisglobal.privilege.valueholder.Privilege;
import org.openelisglobal.privilege.valueholder.RolePrivilege;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PrivilegeDAOImpl extends BaseDAOImpl<Privilege, Integer> implements PrivilegeDAO {

    public PrivilegeDAOImpl() {
        super(Privilege.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Privilege> getPrivilegesForRole(Integer roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        Session session = entityManager.unwrap(Session.class);
        Query<Privilege> query = session.createQuery(
                "SELECT rp.privilege FROM RolePrivilege rp WHERE rp.roleId = :roleId AND rp.privilege.active = true",
                Privilege.class);
        query.setParameter("roleId", roleId);
        return query.list();
    }

    @Override
    @Transactional
    public int replacePrivilegesForRole(Integer roleId, java.util.Collection<Integer> privilegeIds) {
        if (roleId == null) {
            return 0;
        }
        Session session = entityManager.unwrap(Session.class);

        // Delete-then-insert rather than diffing: the grant table is a plain
        // junction with no audit columns of its own, the sets are small (tens of
        // rows), and a replacement is what the caller asked for. Both statements
        // run in the caller's transaction, so a failure leaves the old set intact.
        session.createQuery("DELETE FROM RolePrivilege rp WHERE rp.roleId = :roleId").setParameter("roleId", roleId)
                .executeUpdate();

        if (privilegeIds == null || privilegeIds.isEmpty()) {
            return 0;
        }
        int granted = 0;
        for (Integer privilegeId : new java.util.LinkedHashSet<>(privilegeIds)) {
            if (privilegeId == null) {
                continue;
            }
            Privilege privilege = session.get(Privilege.class, privilegeId);
            if (privilege == null) {
                throw new IllegalArgumentException("No such privilege: " + privilegeId);
            }
            RolePrivilege grant = new RolePrivilege();
            grant.setRoleId(roleId);
            grant.setPrivilege(privilege);
            session.persist(grant);
            granted++;
        }
        session.flush();
        return granted;
    }
}
