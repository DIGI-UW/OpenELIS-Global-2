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

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.util.MenuUtil;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class PluginMenuService {

    private static final Logger logger = LoggerFactory.getLogger(PluginMenuService.class);

    static PluginMenuService INSTANCE;

    private final Map<String, Menu> elementToMenuMap = new HashMap<>();
    private final Map<String, Map<String, String>> menuLabelMap = new HashMap<>();
    private final Map<String, String> actionToKeyMap = new HashMap<>();
    @Autowired
    private MenuService menuService;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private IPluginPermissionService pluginPermissionService;

    public enum KnownMenu {
        ANALYZER("menu_results_analyzer"), WORKPLAN("menu_workplan");

        private final String elementId;

        KnownMenu(String elementId) {
            this.elementId = elementId;
        }

        public String getElementId() {
            return elementId;
        }
    }

    @PostConstruct
    private void registerInstance() {
        INSTANCE = this;
    }

    @EventListener(ContextRefreshedEvent.class)
    private void onApplicationReady() {
        initializeAnalyzerMenus();
    }

    public static PluginMenuService getInstance() {
        return INSTANCE;
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

    /**
     * Register an analyzer's menu entry and permission in one call. Creates the
     * in-memory menu under Results > Analyzer, adds language keys, binds the
     * permission, and invalidates the menu cache.
     *
     * @param analyzerName the analyzer name (used for menu element ID, URL, and
     *                     display label)
     */
    public void registerAnalyzerMenuAndPermission(String analyzerName, String analyzerId) {
        String elementId = analyzerName + "_" + analyzerId + "_plugin";
        String actionURL = "/AnalyzerResults?id=" + analyzerId;

        // Skip if already registered (avoid duplicates on re-init)
        if (actionToKeyMap.containsKey(actionURL)) {
            logger.debug("Menu already registered for analyzer: {}", analyzerName);
            return;
        }

        // Create menu entry under Results > Analyzer
        Menu menu = new Menu();
        menu.setParent(getKnownMenu(KnownMenu.ANALYZER, "menu_results"));
        menu.setPresentationOrder(10);
        menu.setElementId(elementId);
        menu.setActionURL(actionURL);
        menu.setDisplayKey(analyzerName);
        menu.setOpenInNewWindow(false);

        addMenu(menu);
        MenuUtil.forceRebuild();

        // Language keys (English + French default to analyzer name)
        insertLanguageKeyValue(analyzerName, analyzerName, Locale.ENGLISH.toLanguageTag());
        insertLanguageKeyValue(analyzerName, analyzerName, Locale.FRENCH.toLanguageTag());

        // Permission: bind AnalyzerResults module to Results role
        SystemModule module = pluginPermissionService.getOrCreateSystemModule("AnalyzerResults", analyzerName,
                "Results->Analyzer->" + analyzerName);
        Role role = pluginPermissionService.getSystemRole("Results");
        pluginPermissionService.bindRoleToModule(role, module);

        logger.info("Registered menu and permission for analyzer: {}", analyzerName);
    }

    /**
     * Initialize menus for all existing analyzers on application startup. Called
     * after the Spring context is fully ready.
     */
    public void initializeAnalyzerMenus() {
        try {
            List<Analyzer> analyzers = analyzerService.getAll();
            int count = 0;
            for (Analyzer analyzer : analyzers) {
                if (analyzer.getName() != null && !analyzer.getName().trim().isEmpty()) {
                    registerAnalyzerMenuAndPermission(analyzer.getName(), analyzer.getId());
                    count++;
                }
            }
            logger.info("Initialized menus for {} analyzers", count);
        } catch (Exception e) {
            logger.error("Error initializing analyzer menus", e);
        }
    }
}
