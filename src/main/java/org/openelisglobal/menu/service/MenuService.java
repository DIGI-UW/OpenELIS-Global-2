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

    /**
     * Removes menu nodes the current user holds no privilege for, resolving each
     * node's {@code action_url} through the same module chain
     * {@code ModuleAuthenticationInterceptor} authorizes pages against — the menu
     * cannot advertise a destination the interceptor would refuse. A target with no
     * declared policy stays visible.
     *
     * @param menuTree the unfiltered tree; not modified
     * @return a filtered copy, or the same tree when filtering does not apply
     */
    List<MenuItem> filterByPrivilege(List<MenuItem> menuTree);
}
