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

import org.openelisglobal.common.log.LogEvent;

public abstract class MenuPlugin implements APlugin {
    public boolean connect() {
        try {
            insertMenu();
            return true;
        } catch (NullPointerException e) {
            LogEvent.logError(this.getClass().getName(), "connect",
                    "PluginMenuService not available during plugin initialization. "
                            + "This indicates a Spring bean initialization order issue.");
            LogEvent.logError(e);
            return false;
        }
    }

    protected abstract void insertMenu();
}
