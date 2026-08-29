package org.openelisglobal.menu.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.util.ControllerUtills;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.systemuser.controller.UnifiedSystemUserController;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.userrole.service.UserRoleService;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {

    // Element IDs of the three per-domain workflow parents in the menu tree.
    // These must match the element_id values seeded by
    // 025-split-workflow-menus.xml.
    private static final String MENU_CLINICAL = "menu_clinical_workflow";
    private static final String MENU_ENVIRONMENTAL = "menu_environmental_workflow";
    private static final String MENU_VECTOR = "menu_vector_workflow";

    @Autowired
    private MenuService menuService;
    @Autowired
    private UserService userService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private UserRoleService userRoleService;

    @GetMapping(value = "/rest/menu", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MenuItem> getMenuTree(HttpServletRequest request) {
        List<MenuItem> tree = MenuUtil.getMenuTree();
        return applyDomainFilter(tree, ControllerUtills.getSysUserId(request));
    }

    @GetMapping(value = "/rest/menu/{elementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<MenuItem> getMenuTree(@PathVariable String elementId) {
        return findMenuItem(elementId, MenuUtil.getMenuTree());
    }

    @GetMapping(value = "/rest/admin/menu/{elementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<MenuItem> getEditableMenuItem(@PathVariable String elementId) {
        return findMenuItem(elementId, MenuUtil.getUnfilteredMenuTree());
    }

    @PostMapping("/rest/menu")
    public List<MenuItem> postMenuTree(@RequestBody List<MenuItem> menuItems) {
        return menuService.save(menuItems);
    }

    @PostMapping("/rest/menu/{elementId}")
    public MenuItem postMenuTree(@PathVariable String elementId, @RequestBody MenuItem menuItem) {
        return menuService.save(menuItem);
    }

    /**
     * Removes workflow menu entries whose domain doesn't match any of the logged-in
     * user's assigned test sections. Falls back to showing all three workflows when
     * the user has no section assignments or is a global administrator /
     * {@code AllLabUnits} user.
     *
     * <p>
     * The top-level list is shallow-copied and every modified node is re-wrapped,
     * so the cached menu tree is never mutated.
     */
    private List<MenuItem> applyDomainFilter(List<MenuItem> tree, String sysUserId) {
        Set<Domain> allowedDomains = resolveAllowedDomains(sysUserId);
        if (allowedDomains == null) {
            return tree;
        }

        List<MenuItem> filtered = new ArrayList<>(tree);
        for (int i = 0; i < filtered.size(); i++) {
            MenuItem item = filtered.get(i);
            List<MenuItem> filteredChildren = filterWorkflowChildren(item.getChildMenus(), allowedDomains);
            if (filteredChildren != item.getChildMenus()) {
                MenuItem copy = new MenuItem();
                copy.setMenu(item.getMenu());
                copy.setChildMenus(filteredChildren);
                filtered.set(i, copy);
            }
        }
        return filtered;
    }

    /**
     * The domains the user is allowed to order in, derived from their assigned test
     * sections, or {@code null} meaning "no restriction".
     *
     * <p>
     * {@code null} is returned for a global administrator or an {@code AllLabUnits}
     * user (their assignments cover the whole lab), for a user with no assignments
     * at all, and for a user whose sections carry no recognisable domain yet.
     *
     * <p>
     * The assigned sections are resolved from a single
     * {@link TestSectionService#getAllActiveTestSections()} call rather than one
     * {@code getTestSectionById} per assignment: {@code /rest/menu} is hit on every
     * page load, so the per-section lookups were an N+1 on a hot path.
     */
    private Set<Domain> resolveAllowedDomains(String sysUserId) {
        if (isGlobalScopeUser(sysUserId)) {
            return null;
        }

        List<IdValuePair> userSections = userService.getUserTestSections(sysUserId, null);
        if (userSections == null || userSections.isEmpty()) {
            return null;
        }

        Set<String> assignedSectionIds = userSections.stream().map(IdValuePair::getId).collect(Collectors.toSet());
        Set<Domain> domains = testSectionService.getAllActiveTestSections().stream()
                .filter(ts -> ts != null && assignedSectionIds.contains(ts.getId())).map(TestSection::getDomain)
                .map(Domain::fromRaw).filter(Objects::nonNull).collect(Collectors.toSet());

        if (domains.isEmpty()) {
            return null;
        }
        return domains;
    }

    /**
     * True when the user's lab-unit assignments already cover every test section —
     * a global administrator or a user mapped to {@code AllLabUnits}. Needed
     * because {@link UserService#getUserTestSections(String, String)} hands such
     * users every active section, which is indistinguishable from an explicit
     * per-section assignment.
     */
    private boolean isGlobalScopeUser(String sysUserId) {
        if (sysUserId == null) {
            return false;
        }
        if (userRoleService.userInRole(sysUserId, Constants.ROLE_GLOBAL_ADMIN)) {
            return true;
        }
        UserLabUnitRoles labUnitRoles = userService.getUserLabUnitRoles(sysUserId);
        if (labUnitRoles == null || labUnitRoles.getLabUnitRoleMap() == null) {
            return false;
        }
        return labUnitRoles.getLabUnitRoleMap().stream()
                .anyMatch(roleMap -> UnifiedSystemUserController.ALL_LAB_UNITS.equals(roleMap.getLabUnit()));
    }

    /**
     * Returns a filtered copy of childMenus, removing workflow entries not in
     * allowedDomains. Returns the original list reference if nothing was removed.
     */
    private List<MenuItem> filterWorkflowChildren(List<MenuItem> children, Set<Domain> allowedDomains) {
        boolean anyRemoved = false;
        List<MenuItem> result = new ArrayList<>();
        for (MenuItem child : children) {
            String id = child.getMenu().getElementId();
            if (isWorkflowEntry(id) && !domainAllowed(id, allowedDomains)) {
                anyRemoved = true;
            } else {
                List<MenuItem> filteredGrandchildren = filterWorkflowChildren(child.getChildMenus(), allowedDomains);
                if (filteredGrandchildren != child.getChildMenus()) {
                    MenuItem copy = new MenuItem();
                    copy.setMenu(child.getMenu());
                    copy.setChildMenus(filteredGrandchildren);
                    result.add(copy);
                    anyRemoved = true;
                } else {
                    result.add(child);
                }
            }
        }
        return anyRemoved ? result : children;
    }

    private boolean isWorkflowEntry(String elementId) {
        return MENU_CLINICAL.equals(elementId) || MENU_ENVIRONMENTAL.equals(elementId) || MENU_VECTOR.equals(elementId);
    }

    private boolean domainAllowed(String elementId, Set<Domain> allowedDomains) {
        if (MENU_CLINICAL.equals(elementId))
            return allowedDomains.contains(Domain.CLINICAL);
        if (MENU_ENVIRONMENTAL.equals(elementId))
            return allowedDomains.contains(Domain.ENVIRONMENTAL);
        if (MENU_VECTOR.equals(elementId))
            return allowedDomains.contains(Domain.VECTOR);
        return true;
    }

    private Optional<MenuItem> findMenuItem(String elementId, List<MenuItem> menuItems) {
        Queue<MenuItem> queue = new ArrayDeque<>();
        queue.addAll(menuItems);
        while (!queue.isEmpty()) {
            MenuItem menuItem = queue.remove();
            if (elementId.equals(menuItem.getMenu().getElementId())) {
                return Optional.of(menuItem);
            } else {
                for (MenuItem childMenuItem : menuItem.getChildMenus()) {
                    if (!menuItem.getMenu().getElementId().equals(childMenuItem.getMenu().getElementId())) {
                        queue.add(childMenuItem);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
