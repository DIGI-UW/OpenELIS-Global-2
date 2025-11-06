/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.plugin;

import org.openelisglobal.menu.valueholder.Menu;

public class PluginMenu extends Menu {

    private static final long serialVersionUID = -2874772316504828792L;
    private Menu parentMenu;

    @Override
    public Menu getParent() {
        return parentMenu;
    }

    @Override
    public void setParent(Menu parentMenu) {
        this.parentMenu = parentMenu;
    }
}
