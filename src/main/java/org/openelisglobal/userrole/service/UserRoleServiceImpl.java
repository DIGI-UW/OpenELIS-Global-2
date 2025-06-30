package org.openelisglobal.userrole.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.userrole.dao.UserLabUnitRolesDAO;
import org.openelisglobal.userrole.dao.UserRoleDAO;
import org.openelisglobal.userrole.valueholder.LabUnitRoleMap;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.openelisglobal.userrole.valueholder.UserRole;
import org.openelisglobal.userrole.valueholder.UserRolePK;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleServiceImpl extends AuditableBaseObjectServiceImpl<UserRole, UserRolePK>
        implements UserRoleService {
    @Autowired
    protected UserRoleDAO baseObjectDAO;
    @Autowired
    protected UserLabUnitRolesDAO userLabUnitRolesDAO;

    UserRoleServiceImpl() {
        super(UserRole.class);
        defaultSortOrder = new ArrayList<>();
    }

    @Override
    protected UserRoleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRoleIdsForUser(String userId) {
        return baseObjectDAO.getRoleIdsForUser(userId);
    }

    @Override
    @Transactional
    public boolean userInRole(String sysUserId, Collection<String> ableToCancelRoleNames) {
        return baseObjectDAO.userInRole(sysUserId, ableToCancelRoleNames);
    }

    @Override
    public boolean userInRole(String userId, String roleName) {
        return getBaseObjectDAO().userInRole(userId, roleName);
    }

    @Override
    public void saveOrUpdateUserLabUnitRoles(UserLabUnitRoles labRoles) {
        if (null == labRoles.getId()) {
            userLabUnitRolesDAO.insert(labRoles);
        } else {
            userLabUnitRolesDAO.update(labRoles);
        }
    }

    @Override
    public UserLabUnitRoles getUserLabUnitRoles(String userId) {
        return userLabUnitRolesDAO.get(Integer.parseInt(userId)).orElse(null);
    }

    @Override
    public void deleteLabUnitRoleMap(LabUnitRoleMap roleMap) {
        getBaseObjectDAO().deleteLabUnitRoleMap(roleMap);
    }

    @Override
    public List<UserLabUnitRoles> getAllUserLabUnitRoles() {
        return userLabUnitRolesDAO.getAll();
    }

    @Override
    public List<String> getUserIdsForRole(String roleName) {
        return baseObjectDAO.getUserIdsForRole(roleName);
    }
}
