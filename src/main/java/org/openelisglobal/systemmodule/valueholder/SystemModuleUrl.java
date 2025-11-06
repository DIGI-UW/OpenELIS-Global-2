package org.openelisglobal.systemmodule.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

// This class defines the urls that correspond to the different modules
public class SystemModuleUrl extends BaseObject<String> {

    private String id;
    private String urlPath;
    private SystemModule systemModule;
    // the needed request parameters that need to be present to access the module
    private SystemModuleParam param;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public void setUrlPath(String urlPath) {
        this.urlPath = urlPath;
    }

    public SystemModule getSystemModule() {
        return systemModule;
    }

    public void setSystemModule(SystemModule systemModule) {
        this.systemModule = systemModule;
    }

    public SystemModuleParam getParam() {
        return param;
    }

    public void setParam(SystemModuleParam param) {
        this.param = param;
    }
}
