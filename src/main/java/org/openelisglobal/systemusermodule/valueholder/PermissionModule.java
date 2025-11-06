package org.openelisglobal.systemusermodule.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.systemmodule.valueholder.SystemModule;

public abstract class PermissionModule extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private String hasSelect;
    private String hasAdd;
    private String hasUpdate;
    private String hasDelete;
    private String systemModuleId;

    private ValueHolderInterface systemModule;

    public PermissionModule() {
        super();
        this.systemModule = new ValueHolder();
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

    protected void setSystemModuleHolder(ValueHolderInterface systemModule) {
        this.systemModule = systemModule;
    }

    protected ValueHolderInterface getSystemModuleHolder() {
        return this.systemModule;
    }

    public void setSystemModule(SystemModule systemModule) {
        this.systemModule.setValue(systemModule);
    }

    public SystemModule getSystemModule() {
        return (SystemModule) this.systemModule.getValue();
    }

    public void setSystemModuleId(String systemModuleId) {
        this.systemModuleId = systemModuleId;
    }

    public String getSystemModuleId() {
        return systemModuleId;
    }

    public abstract String getPermissionAgentId();

    public abstract PermissionAgent getPermissionAgent();

    public abstract void setPermissionAgent(PermissionAgent agent);
}
