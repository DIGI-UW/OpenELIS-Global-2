package org.openelisglobal.menu.service;

import java.util.List;
import org.openelisglobal.menu.valueholder.AdminMenuItem;

public interface AdminMenuItemService {

    List<AdminMenuItem> getActiveItemsSorted();
}
