package org.openelisglobal.module.api.manifest;

import java.util.ArrayList;
import java.util.List;

/**
 * The parsed {@code META-INF/openelis-module.json}. Plain mutable properties so
 * that any JSON binder can fill it; the registry validates the result against
 * the schema and the rules documented on each section.
 *
 * <p>
 * Every namespace a module owns derives from {@link #getId()}: permission keys
 * {@code mod.<id>.<key>}, message keys {@code mod.<id>.*}, menu element ids
 * {@code menu_mod_<id>_*}, REST paths {@code /rest/modules/<id>/**}, routes
 * {@code /m/<id>/**} and table prefixes {@code mod_<id>_} (hyphens become
 * underscores in SQL identifiers, menu ids and the federation remote name).
 */
public class ModuleManifest {

    /**
     * The manifest id pattern: lower-case, starts with a letter, 3 to 41 characters
     * of letters, digits and hyphens.
     */
    public static final String ID_PATTERN = "^[a-z][a-z0-9-]{2,40}$";

    private int schemaVersion = 1;
    private String id;
    private String kind;
    private String name;
    private String version;
    private String description;
    private Vendor vendor;
    private Requires requires = new Requires();
    private List<String> conflicts = new ArrayList<>();
    private boolean required;
    private BackendSection backend;
    private List<PermissionDeclaration> permissions = new ArrayList<>();
    private List<MenuEntry> menu = new ArrayList<>();
    private FrontendSection frontend;
    private ConfigurationSection configuration;

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** {@code provider} or {@code module}. */
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public Requires getRequires() {
        return requires;
    }

    public void setRequires(Requires requires) {
        this.requires = requires == null ? new Requires() : requires;
    }

    /** Ids of modules that must not be installed next to this one. */
    public List<String> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts == null ? new ArrayList<>() : conflicts;
    }

    /**
     * A required module is never skipped by an isolation policy; reserved, the
     * runtime fails fast regardless.
     */
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public BackendSection getBackend() {
        return backend;
    }

    public void setBackend(BackendSection backend) {
        this.backend = backend;
    }

    public List<PermissionDeclaration> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDeclaration> permissions) {
        this.permissions = permissions == null ? new ArrayList<>() : permissions;
    }

    public List<MenuEntry> getMenu() {
        return menu;
    }

    public void setMenu(List<MenuEntry> menu) {
        this.menu = menu == null ? new ArrayList<>() : menu;
    }

    public FrontendSection getFrontend() {
        return frontend;
    }

    public void setFrontend(FrontendSection frontend) {
        this.frontend = frontend;
    }

    public ConfigurationSection getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ConfigurationSection configuration) {
        this.configuration = configuration;
    }

    /**
     * The module id with hyphens replaced by underscores, as used in SQL
     * identifiers, menu ids and remote names.
     */
    public String getIdAsIdentifier() {
        return id == null ? null : id.replace('-', '_');
    }

    /** The prefix of every permission key of this module: {@code mod.<id>.}. */
    public String getPermissionPrefix() {
        return "mod." + id + ".";
    }

    /**
     * The prefix of every REST mapping of this module: {@code /rest/modules/<id>/}.
     */
    public String getRestPrefix() {
        return "/rest/modules/" + id + "/";
    }

    /** The prefix of every route of this module: {@code /m/<id>/}. */
    public String getRoutePrefix() {
        return "/m/" + id + "/";
    }

    /** The prefix of every table and sequence of this module: {@code mod_<id>_}. */
    public String getTablePrefix() {
        return "mod_" + getIdAsIdentifier() + "_";
    }
}
