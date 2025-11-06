package org.openelisglobal.menu.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.menu.dao.MenuDAO;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            menuItem.setMenu(oldMenu);
        }

        List<MenuItem> oldChildren = menuItem.getChildMenus();
        menuItem.setChildMenus(new ArrayList<>());
        for (MenuItem oldChild : oldChildren) {
            menuItem.getChildMenus().add(save(oldChild));
        }
        return menuItem;
    }
}
