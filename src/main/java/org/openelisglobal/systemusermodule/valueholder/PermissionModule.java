package org.openelisglobal.systemusermodule.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemmodule.valueholder.SystemModule;

@MappedSuperclass
public abstract class PermissionModule extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "permission_module_id_gen")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "id", precision = 10, scale = 0)
    private String id;

    @Column(name = "has_select", length = 1)
    private String hasSelect;

    @Column(name = "has_add", length = 1)
    private String hasAdd;

    @Column(name = "has_update", length = 1)
    private String hasUpdate;

    @Column(name = "has_delete", length = 1)
    private String hasDelete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_module_id")
    private SystemModule systemModule;

    // kept for backward-compat; delegates to the JPA association
    @Transient
    private String systemModuleId;

    public PermissionModule() {
        super();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setHasSelect(String hasSelect) {
        this.hasSelect = hasSelect;
    }

    public String getHasSelect() {
        return hasSelect;
    }

    public void setHasAdd(String hasAdd) {
        this.hasAdd = hasAdd;
    }

    public String getHasAdd() {
        return hasAdd;
    }

    public void setHasUpdate(String hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

    public String getHasUpdate() {
        return hasUpdate;
    }

    public void setHasDelete(String hasDelete) {
        this.hasDelete = hasDelete;
    }

    public String getHasDelete() {
        return hasDelete;
    }

    public void setSystemModule(SystemModule systemModule) {
        this.systemModule = systemModule;
    }

    public SystemModule getSystemModule() {
        return systemModule;
    }

    public void setSystemModuleId(String systemModuleId) {
        this.systemModuleId = systemModuleId;
    }

    public String getSystemModuleId() {
        if (systemModuleId != null) {
            return systemModuleId;
        }
        return systemModule != null ? systemModule.getId() : null;
    }

    public abstract String getPermissionAgentId();

    public abstract PermissionAgent getPermissionAgent();

    public abstract void setPermissionAgent(PermissionAgent agent);
}
