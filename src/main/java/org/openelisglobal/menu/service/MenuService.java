package org.openelisglobal.menu.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.security.access.prepost.PreAuthorize;

@CrossDomainService(callers = "PluginMenuService (analyzer registration, all users), admin configure (PRIV_SYSTEM_CONFIGURE) — read methods are public; write methods are guarded per-method")
public interface MenuService extends BaseObjectService<Menu, String> {
    Menu getMenuByElementId(String elementId);

    List<Menu> getAllActiveMenus();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    MenuItem save(MenuItem menuItem);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<MenuItem> save(List<MenuItem> menuItems);
}
