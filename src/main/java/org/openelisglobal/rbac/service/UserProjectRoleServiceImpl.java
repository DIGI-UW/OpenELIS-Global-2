package org.openelisglobal.rbac.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.rbac.dao.UserProjectRoleDAO;
import org.openelisglobal.userrole.valueholder.UserProjectRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProjectRoleServiceImpl implements UserProjectRoleService {

    @Autowired
    private UserProjectRoleDAO userProjectRoleDAO;

    @Override
    public List<UserProjectRole> getProjectRolesForUser(String systemUserId) {
        if (systemUserId == null) return new ArrayList<>();
        try {
            return userProjectRoleDAO.getBySystemUserId(Integer.parseInt(systemUserId));
        } catch (NumberFormatException e) {
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getProjectIdsForUser(String systemUserId) {
        List<String> projectIds = new ArrayList<>();
        for (UserProjectRole pr : getProjectRolesForUser(systemUserId)) {
            if (!projectIds.contains(pr.getProjectId())) {
                projectIds.add(pr.getProjectId());
            }
        }
        return projectIds;
    }

    @Override
    public List<String> getRoleNamesForUser(String systemUserId) {
        List<String> roles = new ArrayList<>();
        for (UserProjectRole pr : getProjectRolesForUser(systemUserId)) {
            if (!roles.contains(pr.getRoleName())) {
                roles.add(pr.getRoleName());
            }
        }
        return roles;
    }

    @Override
    public boolean hasProjectRole(String systemUserId, String projectId, String roleName) {
        if (systemUserId == null) return false;
        try {
            return userProjectRoleDAO.existsByUserProjectRole(
                    Integer.parseInt(systemUserId), projectId, roleName);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void saveProjectRole(UserProjectRole projectRole) {
        if (projectRole.getId() == null) {
            userProjectRoleDAO.insert(projectRole);
        } else {
            userProjectRoleDAO.update(projectRole);
        }
    }

    @Override
    @Transactional
    public void revokeProjectRole(String systemUserId, String projectId, String roleName) {
        for (UserProjectRole pr : getProjectRolesForUser(systemUserId)) {
            if (projectId.equals(pr.getProjectId()) && roleName.equals(pr.getRoleName())) {
                pr.setActive(false);
                userProjectRoleDAO.update(pr);
            }
        }
    }
}
