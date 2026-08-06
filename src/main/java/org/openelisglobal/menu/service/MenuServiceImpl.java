package org.openelisglobal.menu.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.URLUtil;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.menu.dao.MenuDAO;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.openelisglobal.systemmodule.valueholder.SystemModuleParam;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.systemusermodule.valueholder.PermissionModule;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MenuServiceImpl extends AuditableBaseObjectServiceImpl<Menu, String> implements MenuService {

    /**
     * Authorities that see the whole tree, mirroring the
     * {@code || userModuleService.isUserAdmin(request)} arm of
     * {@code ModuleAuthenticationInterceptor.hasPermission}. Both are needed:
     * production grants them together, but some contexts grant only ROLE_ADMIN.
     */
    private static final Set<String> ADMIN_AUTHORITIES = Set.of("ROLE_GLOBAL_ADMIN", "ROLE_ADMIN");

    @Autowired
    protected MenuDAO baseObjectDAO;

    @Autowired
    private SystemModuleUrlService systemModuleUrlService;

    @Autowired
    private UserRoleService userRoleService;

    @Autowired
    private PermissionModuleService<PermissionModule> permissionModuleService;

    @Autowired
    private UserContextHolder userContextHolder;

    MenuServiceImpl() {
        super(Menu.class);
        disableLogging();
    }

    @Override
    protected MenuDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public Menu getMenuByElementId(String elementId) {
        return getMatch("elementId", elementId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> getAllActiveMenus() {
        return getAllMatching("isActive", true);
    }

    @Override
    @Transactional
    public MenuItem save(MenuItem menuItem) {
        MenuItem item = saveMenuItem(menuItem);
        MenuUtil.forceRebuild();
        return item;
    }

    @Override
    @Transactional
    public List<MenuItem> save(List<MenuItem> menuItems) {
        List<MenuItem> menuItemsNew = new ArrayList<>();
        for (MenuItem menuItem : menuItems) {
            MenuItem item = saveMenuItem(menuItem);
            menuItemsNew.add(item);
        }
        MenuUtil.forceRebuild();
        return menuItemsNew;
    }

    private MenuItem saveMenuItem(MenuItem menuItem) {
        Menu menu = menuItem.getMenu();
        Menu oldMenu;
        if (GenericValidator.isBlankOrNull(menu.getId())) {
            oldMenu = getMatch("elementId", menu.getElementId()).orElse(null);
        } else {
            oldMenu = get(menu.getId());
        }

        // Update menu item if it was added outside the database
        if (oldMenu == null) {
            MenuUtil.updateMenu(menu);
        } else {
            oldMenu.setActionURL(menu.getActionURL());
            oldMenu.setIsActive(menu.getIsActive());
            menuItem.setMenu(save(oldMenu));
        }

        List<MenuItem> oldChildren = menuItem.getChildMenus();
        menuItem.setChildMenus(new ArrayList<>());
        for (MenuItem oldChild : oldChildren) {
            menuItem.getChildMenus().add(save(oldChild));
        }
        return menuItem;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItem> filterByPrivilege(List<MenuItem> menuTree) {
        if (holdsAdminAuthority()) {
            return menuTree;
        }
        String sysUserId = userContextHolder.getCurrentSysUserId();
        if (GenericValidator.isBlankOrNull(sysUserId)) {
            // No resolvable identity (anonymous or daemon context): return the tree
            // rather than hiding every node with a declared policy.
            return menuTree;
        }

        Set<String> permittedModules = permittedModulesForRolesOf(sysUserId);
        Map<String, List<SystemModuleUrl>> urlsByPath = indexModuleUrlsByPath();
        Set<String> visibleElementIds = new HashSet<>();
        collectIndependentlyVisible(menuTree, permittedModules, urlsByPath, visibleElementIds);

        // filterByIncludes already keeps any ancestor of a surviving node, so listing
        // only the independently visible ids implements the pruning rule: a folder
        // whose children were all removed is dropped with them.
        return MenuUtil.filterByIncludes(menuTree, visibleElementIds, Collections.emptySet());
    }

    private boolean holdsAdminAuthority() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(ADMIN_AUTHORITIES::contains);
    }

    /**
     * ponytail: resolves privileges from the user's roles only. The USER value of
     * the {@code permissions.agent} property is unsupported here — it is set in no
     * deployment and {@code system_user_module} carries no rows.
     */
    private Set<String> permittedModulesForRolesOf(String sysUserId) {
        Set<String> permittedModules = new HashSet<>();
        List<String> roleIds = userRoleService.getRoleIdsForUser(sysUserId);
        if (roleIds == null) {
            return permittedModules;
        }
        for (String roleId : roleIds) {
            if (!GenericValidator.isBlankOrNull(roleId)) {
                permittedModules.addAll(
                        permissionModuleService.getAllPermittedPagesFromAgentId(Integer.parseInt(roleId.trim())));
            }
        }
        return permittedModules;
    }

    private Map<String, List<SystemModuleUrl>> indexModuleUrlsByPath() {
        return systemModuleUrlService.getAll().stream().collect(Collectors.groupingBy(SystemModuleUrl::getUrlPath));
    }

    private void collectIndependentlyVisible(List<MenuItem> menuItems, Set<String> permittedModules,
            Map<String, List<SystemModuleUrl>> urlsByPath, Set<String> visibleElementIds) {
        for (MenuItem menuItem : menuItems) {
            if (isIndependentlyVisible(menuItem, permittedModules, urlsByPath)) {
                visibleElementIds.add(menuItem.getMenu().getElementId());
            }
            collectIndependentlyVisible(menuItem.getChildMenus(), permittedModules, urlsByPath, visibleElementIds);
        }
    }

    private boolean isIndependentlyVisible(MenuItem menuItem, Set<String> permittedModules,
            Map<String, List<SystemModuleUrl>> urlsByPath) {
        String actionURL = menuItem.getMenu().getActionURL();
        if (GenericValidator.isBlankOrNull(actionURL)) {
            // No target to authorize. A childless node is a placeholder whose URL is
            // supplied later by an admin (menu_billing), so it stands on its own; a node
            // with children is a folder and survives only through a surviving child.
            return menuItem.getChildMenus().isEmpty();
        }

        List<SystemModuleUrl> candidates = urlsByPath.get(URLUtil.getResourcePath(actionURL));
        if (candidates == null || candidates.isEmpty()) {
            return true;
        }

        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUriString(actionURL).build()
                .getQueryParams();
        boolean anyCandidateApplies = false;
        for (SystemModuleUrl candidate : candidates) {
            if (!paramMatches(candidate.getParam(), queryParams)) {
                continue;
            }
            anyCandidateApplies = true;
            if (permittedModules.contains(candidate.getSystemModule().getSystemModuleName())) {
                return true;
            }
        }
        // Every candidate was ruled out by its parameter predicate, so no policy
        // covers this exact target and the node is treated as undeclared.
        return !anyCandidateApplies;
    }

    private boolean paramMatches(SystemModuleParam param, MultiValueMap<String, String> queryParams) {
        return param == null || Objects.equals(param.getValue(), queryParams.getFirst(param.getName()));
    }
}
