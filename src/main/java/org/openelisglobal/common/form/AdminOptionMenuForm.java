package org.openelisglobal.common.form;

import java.util.List;
import org.openelisglobal.menu.form.AdminMenuForm;

public abstract class AdminOptionMenuForm<T> extends AdminMenuForm {

    private static final long serialVersionUID = 1631221689201039414L;

    public abstract List<T> getMenuList();

    public abstract void setMenuList(List<T> menuList);

    public abstract List<String> getSelectedIDs();

    public abstract void setSelectedIDs(List<String> selectedIDs);
}
