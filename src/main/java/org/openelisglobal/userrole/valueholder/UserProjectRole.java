package org.openelisglobal.userrole.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * UserProjectRole - maps a system user to a project with a specific role.
 * Implements the Project Role scope of the three-tier RBAC model (TR-01).
 */
@Entity
@Table(name = "user_project_roles", schema = "clinlims")
public class UserProjectRole extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "system_user_id", nullable = false)
    private Integer systemUserId;

    @Column(name = "project_id", nullable = false, length = 255)
    private String projectId;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "granted_by")
    private Integer grantedBy;

    @Column(name = "granted_at", insertable = false, updatable = false)
    private Timestamp grantedAt;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(Integer grantedBy) {
        this.grantedBy = grantedBy;
    }

    public Timestamp getGrantedAt() {
        return grantedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
