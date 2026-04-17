package org.openelisglobal.menu.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.menu.util.MenuItem;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.security.access.prepost.PreAuthorize;

public interface MenuService extends BaseObjectService<Menu, String> {
    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    Menu getMenuByElementId(String elementId);

    @PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
    List<Menu> getAllActiveMenus();

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    MenuItem save(MenuItem menuItem);

    @PreAuthorize("hasAuthority('PRIV_SYSTEM_CONFIGURE')")
    List<MenuItem> save(List<MenuItem> menuItems);
}
