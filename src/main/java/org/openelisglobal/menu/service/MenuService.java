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
     * Removes menu nodes the current user holds no privilege for.
     *
     * <p>
     * A node's target ({@code action_url}) is resolved through the same
     * {@code system_module_url} → {@code system_module} →
     * {@code system_role_module} chain that {@code ModuleAuthenticationInterceptor}
     * uses to authorize page access, so the menu cannot advertise a destination the
     * interceptor would refuse. A target with no policy declared for it stays
     * visible, which keeps deployments that have not populated the mapping
     * unchanged.
     *
     * @param menuTree the unfiltered tree; not modified
     * @return a filtered copy, or the same tree when filtering does not apply
     */
    List<MenuItem> filterByPrivilege(List<MenuItem> menuTree);
}
