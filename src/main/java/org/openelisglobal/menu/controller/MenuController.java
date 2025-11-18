package org.openelisglobal.menu.controller;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
        List<MenuItem> tree = MenuUtil.getMenuTree();
        ensureAnalyzerQcPlaceholders(tree);
        return tree;
    }

    @GetMapping(value = "/rest/menu/{elementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Optional<MenuItem> getMenuTree(@PathVariable String elementId) {
        return findMenuItem(elementId, MenuUtil.getMenuTree());
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

    /**
     * Ensures QC placeholder entries are present under the Analyzers parent in the
     * returned menu tree (non-persistent). This augments DB-defined menus to
     * surface the unified analyzer hierarchy per FR-020.
     */
    private void ensureAnalyzerQcPlaceholders(List<MenuItem> root) {
        Optional<MenuItem> analyzersParent = findMenuItem("menu_analyzers", root);
        if (!analyzersParent.isPresent()) {
            return;
        }

        MenuItem analyzers = analyzersParent.get();
        boolean hasQcGroup = analyzers.getChildMenus().stream()
                .anyMatch(mi -> "menu_analyzers_qc".equals(mi.getMenu().getElementId()));

        if (!hasQcGroup) {
            // Create QC group parent
            Menu qcGroup = new Menu();
            qcGroup.setElementId("menu_analyzers_qc");
            qcGroup.setActionURL("/analyzers/qc");
            qcGroup.setDisplayKey("analyzer.navigation.qc");
            qcGroup.setToolTipKey("analyzer.navigation.qc");
            qcGroup.setPresentationOrder(4);
            qcGroup.setIsActive(true);
            qcGroup.setHideInOldUI(true);
            qcGroup.setParent(analyzers.getMenu());

            MenuItem qcGroupItem = new MenuItem();
            qcGroupItem.setMenu(qcGroup);

            // Child: QC Alerts & Violations
            Menu qcAlerts = new Menu();
            qcAlerts.setElementId("menu_analyzers_qc_alerts");
            qcAlerts.setActionURL("/analyzers/qc/alerts");
            qcAlerts.setDisplayKey("analyzer.navigation.qcAlerts");
            qcAlerts.setToolTipKey("analyzer.navigation.qcAlerts");
            qcAlerts.setPresentationOrder(1);
            qcAlerts.setIsActive(true);
            qcAlerts.setHideInOldUI(true);
            qcAlerts.setParent(qcGroup);

            MenuItem qcAlertsItem = new MenuItem();
            qcAlertsItem.setMenu(qcAlerts);

            // Child: Corrective Actions
            Menu qcCorrective = new Menu();
            qcCorrective.setElementId("menu_analyzers_qc_corrective_actions");
            qcCorrective.setActionURL("/analyzers/qc/corrective-actions");
            qcCorrective.setDisplayKey("analyzer.navigation.qcCorrectiveActions");
            qcCorrective.setToolTipKey("analyzer.navigation.qcCorrectiveActions");
            qcCorrective.setPresentationOrder(2);
            qcCorrective.setIsActive(true);
            qcCorrective.setHideInOldUI(true);
            qcCorrective.setParent(qcGroup);

            MenuItem qcCorrectiveItem = new MenuItem();
            qcCorrectiveItem.setMenu(qcCorrective);

            qcGroupItem.getChildMenus().add(qcAlertsItem);
            qcGroupItem.getChildMenus().add(qcCorrectiveItem);
            qcGroupItem.sortChildren();

            analyzers.getChildMenus().add(qcGroupItem);
            analyzers.sortChildren();
        }
    }
}
