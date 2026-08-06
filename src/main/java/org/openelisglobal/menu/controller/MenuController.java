package org.openelisglobal.menu.controller;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
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

    @Autowired
    private MenuService menuService;

    @GetMapping(value = "/rest/menu", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MenuItem> getMenuTree() {
        return MenuUtil.getMenuTree();
    }

    @GetMapping(value = "/rest/menu/{elementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<MenuItem> getMenuTree(@PathVariable String elementId) {
        return findMenuItem(elementId, MenuUtil.getMenuTree());
    }

    /**
     * Serves the unfiltered tree for the menu configuration screens, which must be
     * able to edit nodes the editing admin would not otherwise see.
     *
     * <p>
     * Admin-only: without this the privilege filter on {@code /rest/menu} is
     * trivially sidestepped, because any authenticated caller could read the
     * unfiltered node here instead. The interceptor does not cover it — no
     * {@code system_module_url} row maps this path, so {@code /rest/**} is
     * auto-allowed. Every caller (the Billing, Patient, NonConformity and Global
     * menu configuration screens) already sits behind a GLOBAL_ADMIN route.
     */
    @PreAuthorize("hasRole('ADMIN')")
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

}
