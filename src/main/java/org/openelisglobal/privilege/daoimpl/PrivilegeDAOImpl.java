package org.openelisglobal.privilege.daoimpl;

import java.util.Collections;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.privilege.dao.PrivilegeDAO;
import org.openelisglobal.privilege.valueholder.Privilege;
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
}
