package org.openelisglobal.menu.service;

import java.util.List;
import org.openelisglobal.menu.valueholder.AdminMenuItem;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_SITE_INFO_VIEW')")
public interface AdminMenuItemService {

    List<AdminMenuItem> getActiveItemsSorted();
}
