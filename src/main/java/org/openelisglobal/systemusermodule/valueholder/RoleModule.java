package org.openelisglobal.systemusermodule.valueholder;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.openelisglobal.role.valueholder.Role;

/**
 * The primary purpose of this class is to make the code more literate. It adds
 * no new behavior
 *
 * @author pauls
 */
@Entity
@Table(name = "system_role_module", schema = "clinlims")
@SequenceGenerator(name = "system_role_module_seq_gen", sequenceName = "system_role_module_seq", schema = "clinlims", allocationSize = 1)
public class RoleModule extends PermissionModule {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_role_id")
    private Role role;

    // transient — kept so existing callers of setRoleId/getRoleId still compile
    @Transient
    private String roleId;

    public RoleModule() {
        super();
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getRoleId() {
        if (roleId != null) {
            return roleId;
        }
        return role != null ? String.valueOf(role.getId()) : null;
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
