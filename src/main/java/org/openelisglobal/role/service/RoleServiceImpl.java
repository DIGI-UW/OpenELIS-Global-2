package org.openelisglobal.role.service;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.role.dao.RoleDAO;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleServiceImpl extends AuditableBaseObjectServiceImpl<Role, Integer> implements RoleService {
    @Autowired
    protected RoleDAO baseObjectDAO;

    RoleServiceImpl() {
        super(Role.class);
        this.auditTrailLog = true;
    }

    @Override
    protected RoleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllActiveRoles() {
        return baseObjectDAO.getAllMatching("active", true);
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Role role) {
        getBaseObjectDAO().getData(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getReferencingRoles(Role role) {
        return getBaseObjectDAO().getReferencingRoles(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getPageOfRoles(int startingRecNo) {
        return getBaseObjectDAO().getPageOfRoles(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleByName(String name) {
        Role role = getBaseObjectDAO().getRoleByName(name);
        if (role == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "getRoleByName",
                    "Role not found in database: '" + name + "'");
            // Preserve the never-null contract callers rely on (e.g.
            // ServiceRequestProvider -> getUserSampleTypes): a sentinel id of -1
            // matches no persisted role, so downstream role-scoped lookups return
            // empty rather than NPEing on a null Role. The RBAC migration made the
            // id Integer; the sentinel moved from "-1" to -1 accordingly.
            Role stub = new Role();
            stub.setId(-1);
            stub.setName(name);
            return stub;
        }
        return role;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return getBaseObjectDAO().getAllRoles();
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRoleById(Integer roleId) {
        return getBaseObjectDAO().getRoleById(roleId);
    }

    @Override
    @Transactional
    public Role createAssignableRole(String name, String description, String displayKey, String groupingParentName,
            String parentRoleName, String sysUserId) {
        if (GenericValidator.isBlankOrNull(name)) {
            throw new IllegalArgumentException("Role name is required");
        }
        String trimmed = name.trim();
        Role existing = getRoleByName(trimmed);
        if (existing != null && !Integer.valueOf(-1).equals(existing.getId())) {
            throw new IllegalArgumentException("A role named '" + trimmed + "' already exists");
        }

        Role role = new Role();
        role.setName(trimmed);
        role.setDescription(GenericValidator.isBlankOrNull(description) ? trimmed : description.trim());
        if (!GenericValidator.isBlankOrNull(displayKey)) {
            role.setDisplayKey(displayKey.trim());
        }
        role.setActive(true);
        role.setEditable(true);
        role.setGroupingRole(false);
        role.setGroupingParent(resolveRequiredRoleId(groupingParentName, "grouping parent"));
        if (!GenericValidator.isBlankOrNull(parentRoleName)) {
            role.setParentRoleId(resolveRequiredRoleId(parentRoleName, "inheritance parent"));
        }
        role.setSysUserId(sysUserId);

        Integer id = insert(role);
        return get(id);
    }

    /**
     * Resolves a role NAME to its id, failing loudly rather than silently nulling.
     */
    private Integer resolveRequiredRoleId(String roleName, String what) {
        if (GenericValidator.isBlankOrNull(roleName)) {
            throw new IllegalArgumentException("A " + what + " is required");
        }
        Role parent = getRoleByName(roleName.trim());
        if (parent == null || Integer.valueOf(-1).equals(parent.getId())) {
            throw new IllegalArgumentException("No such role for " + what + ": '" + roleName.trim() + "'");
        }
        return parent.getId();
    }
}
