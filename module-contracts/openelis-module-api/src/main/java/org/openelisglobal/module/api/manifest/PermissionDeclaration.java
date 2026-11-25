package org.openelisglobal.module.api.manifest;

import java.util.ArrayList;
import java.util.List;

/**
 * One permission key of the module. Registered as a {@code system_module} row
 * named {@code mod.<id>.<key>}; the granted key becomes a Spring authority at
 * login and reaches the browser in the session payload, so controllers gate
 * with {@code hasAuthority('mod.<id>.<key>')} and pages with the same key.
 */
public class PermissionDeclaration {

    private String key;
    private String description;
    private List<String> defaultRoles = new ArrayList<>();

    /** The key without the module prefix: {@code view}, {@code manage}, ... */
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Role names granted this key when the module is installed for the first time;
     * never applied again.
     */
    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<String> defaultRoles) {
        this.defaultRoles = defaultRoles == null ? new ArrayList<>() : defaultRoles;
    }
}
