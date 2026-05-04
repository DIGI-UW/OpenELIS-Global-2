package org.openelisglobal.menu.controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {

    private static final String DATABASE_CLEANING_ELEMENT_ID = "menu_admin_database_cleaning";

    @Autowired
    private MenuService menuService;

    @GetMapping(value = "/rest/menu", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MenuItem> getMenuTree() {
        return MenuUtil.getMenuTree();
    }

    /**
     * Returns the admin sidenav tree (rows in {@code clinlims.menu} with
     * {@code nav_scope='admin'}). Kept on its own endpoint — and gated by ADMIN —
     * so admin URLs don't ride along on {@code /rest/menu}, which is served to
     * every authenticated user without per-row role filtering. Database Cleaning is
     * dropped at request time on non-training installations.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/rest/admin-menu", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MenuItem> getAdminMenuTree() {
        List<MenuItem> tree = MenuUtil.getAdminMenuTree();
        if (!isTrainingInstallation()) {
            tree = removeByElementId(tree, DATABASE_CLEANING_ELEMENT_ID);
        }
        return tree;
    }

    /**
     * Toggle is_active on admin sidenav rows. Mirrors {@code POST /rest/menu} for
     * the lab nav and shares the same {@link MenuService#save} flow, which
     * deliberately writes only is_active and action_url. nav_scope, parent_id,
     * presentation_order, and element_id are preserved on every row.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/rest/admin-menu", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MenuItem> postAdminMenuTree(@RequestBody List<MenuItem> menuItems) {
        return menuService.save(menuItems);
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

    private Optional<MenuItem> findMenuItem(String elementId, List<MenuItem> menuItems) {
        Queue<MenuItem> queue = new ArrayDeque<>();
        queue.addAll(menuItems);
        while (!queue.isEmpty()) {
            MenuItem menuItem = queue.remove();
            if (elementId.equals(menuItem.getMenu().getElementId())) {
                return Optional.of(menuItem);
            } else {
                for (MenuItem childMenuItem : menuItem.getChildMenus()) {
                    if (menuItem.getMenu().getElementId() != childMenuItem.getMenu().getElementId()) {
                        queue.add(childMenuItem); // prevent infinite loops if a menu option points to itself
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static List<MenuItem> removeByElementId(List<MenuItem> tree, String elementId) {
        List<MenuItem> result = new ArrayList<>();
        for (MenuItem item : tree) {
            if (elementId.equals(item.getMenu().getElementId())) {
                continue;
            }
            MenuItem copy = new MenuItem();
            copy.setMenu(item.getMenu());
            copy.setChildMenus(removeByElementId(item.getChildMenus(), elementId));
            result.add(copy);
        }
        return result;
    }

    private static boolean isTrainingInstallation() {
        return "true".equalsIgnoreCase(ConfigurationProperties.getInstance()
                .getPropertyValueLowerCase(ConfigurationProperties.Property.TrainingInstallation));
    }

}
