/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.menu.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.menu.valueholder.Menu;

public class MenuItem {
    private Menu menu;

    private List<MenuItem> childMenus = new ArrayList<MenuItem>();

    public void setChildMenus(List<MenuItem> childMenus) {
        this.childMenus = childMenus;
    }

    public List<MenuItem> getChildMenus() {
        return childMenus;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public Menu getMenu() {
        return menu;
    }

    public void sortChildren() {
        Collections.sort(childMenus, new Comparator<MenuItem>() {

            @Override
            public int compare(MenuItem o1, MenuItem o2) {
                return o1.getMenu().getPresentationOrder() - o2.getMenu().getPresentationOrder();
            }
        });
    }
}
