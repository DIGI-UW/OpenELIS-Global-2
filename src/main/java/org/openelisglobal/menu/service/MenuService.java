package org.openelisglobal.menu.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.valueholder.Menu;

public interface MenuService extends BaseObjectService<Menu, String> {
    Menu getMenuByElementId(String elementId);

    List<Menu> getAllActiveMenus();

    MenuItem save(MenuItem menuItem);

    List<MenuItem> save(List<MenuItem> menuItems);
}
