package org.openelisglobal.module.runtime.contributions;

import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.URLUtil;
import org.openelisglobal.module.api.manifest.MenuEntry;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.api.manifest.PermissionDeclaration;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleStartupException;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.rolemodule.service.RoleModuleService;
import org.openelisglobal.security.DaemonContextExecutor;
import org.openelisglobal.systemmodule.service.SystemModuleService;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registers a module's permission keys as {@code system_module} rows named
 * {@code mod.<id>.<key>}, idempotently, at every start. A granted key becomes a
 * Spring authority at login (the {@code mod.} prefix is one of the permission
 * prefixes) and reaches the browser in the session payload, so
 * {@code hasAuthority('mod.<id>.<key>')} gates module endpoints and the same
 * key gates module pages and menu entries.
 *
 * <p>
 * Every key is granted to the Global Administrator role at every start, so an
 * administrator can always reach a module's pages and endpoints, and
 * {@code defaultRoles} are granted on the module's first install only, so later
 * changes in Role Management are never undone. Each menu entry with an
 * {@code actionURL} and a {@code permission} also gets a
 * {@code system_module_url} row, which is how the core's menu service hides
 * entries from users who lack the key.
 */
public class ModulePermissionRegistrar {

    private static final String LOG = "ModulePermissionRegistrar";
    private static final String DAEMON_USER_ID = "1";

    private final SystemModuleService systemModuleService;
    private final SystemModuleUrlService systemModuleUrlService;
    private final RoleModuleService roleModuleService;
    private final RoleService roleService;
    private final DaemonContextExecutor daemonContextExecutor;
    private final TransactionTemplate transactionTemplate;

    public ModulePermissionRegistrar(SystemModuleService systemModuleService,
            SystemModuleUrlService systemModuleUrlService, RoleModuleService roleModuleService, RoleService roleService,
            DaemonContextExecutor daemonContextExecutor, TransactionTemplate transactionTemplate) {
        this.systemModuleService = systemModuleService;
        this.systemModuleUrlService = systemModuleUrlService;
        this.roleModuleService = roleModuleService;
        this.roleService = roleService;
        this.daemonContextExecutor = daemonContextExecutor;
        this.transactionTemplate = transactionTemplate;
    }

    public void register(ModuleRecord module) {
        ModuleManifest manifest = module.manifest();
        if (manifest.getPermissions().isEmpty()) {
            return;
        }
        try {
            daemonContextExecutor.executeAsDaemon(() -> transactionTemplate.executeWithoutResult(status -> {
                for (PermissionDeclaration declaration : manifest.getPermissions()) {
                    SystemModule systemModule = ensureSystemModule(manifest, declaration);
                    grant(module, Constants.ROLE_GLOBAL_ADMIN, systemModule, "administrators");
                    if (module.isFirstInstall()) {
                        for (String roleName : declaration.getDefaultRoles()) {
                            grant(module, roleName, systemModule, "first install");
                        }
                    }
                    for (MenuEntry entry : manifest.getMenu()) {
                        if (declaration.getKey().equals(entry.getPermission()) && entry.getActionURL() != null
                                && !entry.getActionURL().isEmpty()) {
                            ensureUrl(systemModule, entry.getActionURL());
                        }
                    }
                }
            }));
        } catch (RuntimeException e) {
            throw new ModuleStartupException("registering permissions failed: " + e.getMessage(), e);
        }
    }

    private SystemModule ensureSystemModule(ModuleManifest manifest, PermissionDeclaration declaration) {
        String name = manifest.getPermissionPrefix() + declaration.getKey();
        SystemModule existing = systemModuleService.getSystemModuleByName(name);
        if (existing != null) {
            return existing;
        }
        SystemModule systemModule = new SystemModule();
        systemModule.setSystemModuleName(name);
        systemModule.setDescription(
                truncate(declaration.getDescription() == null ? name : declaration.getDescription(), 80));
        systemModule.setHasSelectFlag("Y");
        systemModule.setHasAddFlag("N");
        systemModule.setHasUpdateFlag("N");
        systemModule.setHasDeleteFlag("N");
        systemModule.setSysUserId(DAEMON_USER_ID);
        systemModuleService.insert(systemModule);
        LogEvent.logInfo(LOG, "ensureSystemModule",
                "[module:" + manifest.getId() + "] permission " + name + " registered");
        return systemModule;
    }

    private void grant(ModuleRecord module, String roleName, SystemModule systemModule, String reason) {
        Role role = roleService.getRoleByName(roleName);
        if (role == null) {
            module.addWarning("role '" + roleName + "' for " + systemModule.getSystemModuleName()
                    + " does not exist; grant the key in Role Management");
            LogEvent.logWarn(LOG, "grant", "[module:" + module.id() + "] no role named '" + roleName + "' for "
                    + systemModule.getSystemModuleName());
            return;
        }
        if (isGranted(role, systemModule)) {
            return;
        }
        RoleModule roleModule = new RoleModule();
        roleModule.setRole(role);
        roleModule.setSystemModule(systemModule);
        roleModule.setHasSelect("Y");
        roleModule.setHasAdd("N");
        roleModule.setHasUpdate("N");
        roleModule.setHasDelete("N");
        roleModule.setSysUserId(DAEMON_USER_ID);
        roleModuleService.insert(roleModule);
        LogEvent.logInfo(LOG, "grant", "[module:" + module.id() + "] " + systemModule.getSystemModuleName()
                + " granted to role '" + roleName + "' (" + reason + ")");
    }

    /**
     * The role-module DAO answers an absent grant with an empty {@link RoleModule}
     * rather than null, so only a persisted id counts as granted.
     */
    private boolean isGranted(Role role, SystemModule systemModule) {
        RoleModule existing = roleModuleService.getRoleModuleByRoleAndModuleId(role.getId(), systemModule.getId());
        return existing != null && existing.getId() != null;
    }

    private void ensureUrl(SystemModule systemModule, String actionURL) {
        String path = URLUtil.getResourcePath(actionURL);
        if (systemModuleUrlService.getByModuleAndUrl(systemModule.getId(), path) != null) {
            return;
        }
        SystemModuleUrl url = new SystemModuleUrl();
        url.setSystemModule(systemModule);
        url.setUrlPath(path);
        url.setSysUserId(DAEMON_USER_ID);
        systemModuleUrlService.insert(url);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
