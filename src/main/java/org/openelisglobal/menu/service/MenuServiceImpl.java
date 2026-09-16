package org.openelisglobal.menu.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.menu.dao.MenuDAO;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MenuServiceImpl extends AuditableBaseObjectServiceImpl<Menu, String> implements MenuService {

    @Autowired
    protected MenuDAO baseObjectDAO;

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
        MenuItem item = saveMenuItem(menuItem, configuredMenus());
        rebuildAfterCommit();
        return item;
    }

    @Override
    @Transactional
    public List<MenuItem> save(List<MenuItem> menuItems) {
        List<MenuItem> menuItemsNew = new ArrayList<>();
        Map<String, Menu> configuration = configuredMenus();
        for (MenuItem menuItem : menuItems) {
            MenuItem item = saveMenuItem(menuItem, configuration);
            menuItemsNew.add(item);
        }
        rebuildAfterCommit();
        return menuItemsNew;
    }

    private Map<String, Menu> configuredMenus() {
        Map<String, Menu> result = new HashMap<>();
        collectMenus(MenuUtil.getUnfilteredMenuTree(), result);
        return result;
    }

    private void collectMenus(List<MenuItem> items, Map<String, Menu> result) {
        for (MenuItem item : items) {
            result.put(item.getMenu().getElementId(), item.getMenu());
            collectMenus(item.getChildMenus(), result);
        }
    }

    private void rebuildAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    MenuUtil.forceRebuild();
                }
            });
        } else {
            MenuUtil.forceRebuild();
        }
    }

    private MenuItem saveMenuItem(MenuItem menuItem, Map<String, Menu> configuration) {
        Menu menu = menuItem.getMenu();
        Menu oldMenu;
        if (GenericValidator.isBlankOrNull(menu.getId())) {
            oldMenu = getMatch("elementId", menu.getElementId()).orElse(null);
        } else {
            oldMenu = get(menu.getId());
        }

        Menu effective = configuration.get(menu.getElementId());
        Set<String> controlled = effective == null ? Set.of() : effective.getConfigurationFields();
        if (oldMenu == null) {
            if (effective == null || !effective.isConfigurationOnly()) {
                MenuUtil.updateMenu(menu);
            } else {
                menuItem.setMenu(effective);
            }
        } else {
            if (!controlled.contains("actionURL")) {
                oldMenu.setActionURL(menu.getActionURL());
            }
            if (!controlled.contains("isActive")) {
                oldMenu.setIsActive(menu.getIsActive());
            }
            if (menu.isPresentationStyleSpecified() && !controlled.contains("presentationStyle")) {
                String style = normalize(menu.getPresentationStyle());
                if (style != null && !"section".equals(style)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported menu presentation style");
                }
                oldMenu.setPresentationStyle(style);
            }
            if (menu.isIconSpecified() && !controlled.contains("icon")) {
                String icon = normalize(menu.getIcon());
                if (icon != null && !icon.matches("[a-z][a-z0-9-]{0,59}")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid menu icon name");
                }
                oldMenu.setIcon(icon);
            }
            menuItem.setMenu(save(oldMenu));
        }

        List<MenuItem> oldChildren = menuItem.getChildMenus();
        menuItem.setChildMenus(new ArrayList<>());
        for (MenuItem oldChild : oldChildren) {
            menuItem.getChildMenus().add(saveMenuItem(oldChild, configuration));
        }
        return menuItem;
    }

    private String normalize(String value) {
        return GenericValidator.isBlankOrNull(value) ? null : value.trim();
    }
}
