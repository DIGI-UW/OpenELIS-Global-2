package org.openelisglobal.systemusermodule.valueholder;

import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.role.valueholder.Role;

/**
 * The primary purpose of this class is to make the code more literate. It adds
 * no new behavior
 *
 * @author pauls
 */
public class RoleModule extends PermissionModule {

    private static final long serialVersionUID = 1L;

    private String roleId;
    private ValueHolderInterface role;

    public RoleModule() {
        super();
        this.role = new ValueHolder();
    }

    protected void setRoleHolder(ValueHolderInterface role) {
        this.role = role;
    }

    protected ValueHolderInterface getRoleHolder() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role.setValue(role);
    }

    public Role getRole() {
        return (Role) this.role.getValue();
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleId() {
        return roleId;
    }

    @Override
    public String getPermissionAgentId() {
        return getRoleId();
    }

    @Override
    public PermissionAgent getPermissionAgent() {
        return getRole();
    }

    @Override
    public void setPermissionAgent(PermissionAgent agent) {
        setRole((Role) agent);
    }
}
