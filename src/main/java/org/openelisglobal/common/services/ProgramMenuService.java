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

package org.openelisglobal.common.services;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.program.service.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProgramMenuService {

    private final Map<String, Menu> elementToMenuMap = new HashMap<>();
    private final Map<String, Map<String, String>> menuLabelMap = new HashMap<>();
    private final Map<String, String> actionToKeyMap = new HashMap<>();
    @Autowired
    private MenuService menuService;
    @Autowired
    private ProgramService programService;

    public enum KnownMenu {
        ANALYZER("menu_program_pathology");

        private final String elementId;

        KnownMenu(String elementId) {
            this.elementId = elementId;
        }

        public String getElementId() {
            return elementId;
        }
    }

    public Menu getKnownMenu(KnownMenu knownMenu, String defaultKnownMenuParentId) {
        return knownMenu == null ? null : getMenuByElementId(knownMenu.getElementId(), defaultKnownMenuParentId);
    }

    public Menu getMenuByElementId(String elementId, String defaultKnownMenuParentId) {

        Menu menu = elementToMenuMap.get(elementId);

        if (menu != null) {
            return menu;
        }

        menu = menuService.getMenuByElementId(elementId);

        if (menu != null) {
            elementToMenuMap.put(elementId, menu);
            return menu;
        }

        menu = new Menu();
        Menu parent = menuService.getMenuByElementId(defaultKnownMenuParentId);
        menu.setParent(parent);
        menu.setPresentationOrder(5);
        menu.setElementId("menu_results_analyzer");
        menu.setDisplayKey("banner.menu.results.analyzer");
        MenuUtil.addMenu(menu);

        elementToMenuMap.put(elementId, menu);
        return menu;
    }

    public void insertLanguageKeyValue(String key, String value, String locale) {
        Map<String, String> localSpecificMap = menuLabelMap.get(locale);
        if (localSpecificMap == null) {
            localSpecificMap = new HashMap<>();
            menuLabelMap.put(locale, localSpecificMap);
        }
        localSpecificMap.put(key, value);
    }

    public String getMenuLabel(String locale, String key) {
        Map<String, String> localSpecificMap = getLocaleSpecificMap(Locale.forLanguageTag(locale));
        if (localSpecificMap == null) {
            return key;
        }

        String value = localSpecificMap.get(key);
        if (value == null) {
            return key;
        }
        return value;
    }

    private Map<String, String> getLocaleSpecificMap(Locale locale) {
        Map<String, String> localSpecificMap = menuLabelMap.get(locale.toLanguageTag());
        if (localSpecificMap == null) {
            localSpecificMap = menuLabelMap.get(locale.getLanguage());
        }
        return localSpecificMap;
    }

    public void addMenu(Menu menu) {
        MenuUtil.addMenu(menu);
        actionToKeyMap.put(menu.getActionURL(), menu.getDisplayKey());
    }

    public boolean hasMenu(Menu menu) {
        return actionToKeyMap.containsKey(menu.getActionURL());
    }

    public String getKeyForAction(String action) {
        return actionToKeyMap.get(action);
    }
}
