package org.openelisglobal.menu.service;

import java.util.List;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.menu.valueholder.AdminMenuItem;

@CrossDomainService(callers = "LoginPageController (post-login menu rendering, all authenticated users) — read-only; no write methods")
public interface AdminMenuItemService {

    List<AdminMenuItem> getActiveItemsSorted();
}
