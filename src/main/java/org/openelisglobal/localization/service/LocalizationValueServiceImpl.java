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

package org.openelisglobal.localization.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.localization.dao.LocalizationDAO;
import org.openelisglobal.localization.dao.LocalizationValueDAO;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.LocalizationValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalizationValueServiceImpl extends BaseObjectServiceImpl<LocalizationValue, String>
        implements LocalizationValueService {

    @Autowired
    private LocalizationValueDAO localizationValueDAO;

    @Autowired
    private LocalizationDAO localizationDAO;

    @Autowired
    private SupportedLocaleService supportedLocaleService;

    public LocalizationValueServiceImpl() {
        super(LocalizationValue.class);
    }

    @Override
    protected LocalizationValueDAO getBaseObjectDAO() {
        return localizationValueDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalizationValue> getByLocalizationId(String localizationId) {
        return localizationValueDAO.getByLocalizationId(localizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getValuesAsMap(String localizationId) {
        Map<String, String> result = new HashMap<>();
        for (LocalizationValue lv : getByLocalizationId(localizationId)) {
            result.put(lv.getLocale(), lv.getValue());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalizationValue> getByLocalizationIdAndLocale(String localizationId, String locale) {
        return localizationValueDAO.getByLocalizationIdAndLocale(localizationId, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public String getLocalizedValue(String localizationId, String locale) {
        if (localizationId == null || localizationId.isEmpty()) {
            return "";
        }
        if (locale == null || locale.isEmpty()) {
            return "";
        }

        List<LocalizationValue> allValues = getByLocalizationId(localizationId);
        if (allValues.isEmpty()) {
            return "";
        }

        // Build lookup map for O(1) access
        Map<String, String> valuesByLocale = new HashMap<>();
        for (LocalizationValue lv : allValues) {
            valuesByLocale.put(lv.getLocale(), lv.getValue());
        }

        // Try each locale in fallback chain
        for (String candidate : buildFallbackChain(locale)) {
            String value = valuesByLocale.get(candidate);
            if (value != null) {
                return value;
            }
        }

        // Last resort: return first available translation
        return allValues.get(0).getValue();
    }

    /**
     * Builds a BCP-47 compliant fallback chain for locale resolution.
     *
     * For "fr-CI" (French - Côte d'Ivoire), produces: [fr-CI, fr, en] (assuming
     * "en" is the system fallback)
     *
     * For "zh-Hans-CN" (Simplified Chinese - China), produces: [zh-Hans-CN,
     * zh-Hans, zh, en]
     */
    private List<String> buildFallbackChain(String localeTag) {
        List<String> chain = new ArrayList<>();
        String systemFallback = supportedLocaleService.getFallbackLocaleCode();

        // Parse as BCP-47 language tag
        Locale locale = Locale.forLanguageTag(localeTag);

        // Add the original locale
        chain.add(localeTag);

        // Strip components progressively: variant -> country -> script
        if (!locale.getVariant().isEmpty()) {
            // Remove variant: zh-Hans-CN-variant -> zh-Hans-CN
            Locale withoutVariant = new Locale.Builder().setLocale(locale).setVariant("").build();
            String tag = withoutVariant.toLanguageTag();
            if (!chain.contains(tag)) {
                chain.add(tag);
            }
            locale = withoutVariant;
        }

        if (!locale.getCountry().isEmpty()) {
            // Remove country: fr-CI -> fr, zh-Hans-CN -> zh-Hans
            Locale withoutCountry = new Locale.Builder().setLocale(locale).setRegion("").build();
            String tag = withoutCountry.toLanguageTag();
            if (!chain.contains(tag)) {
                chain.add(tag);
            }
            locale = withoutCountry;
        }

        if (!locale.getScript().isEmpty()) {
            // Remove script: zh-Hans -> zh
            Locale withoutScript = new Locale.Builder().setLocale(locale).setScript("").build();
            String tag = withoutScript.toLanguageTag();
            if (!chain.contains(tag)) {
                chain.add(tag);
            }
        }

        // Add system fallback if not already in chain
        if (systemFallback != null && !chain.contains(systemFallback)) {
            chain.add(systemFallback);
        }

        return chain;
    }

    @Override
    @Transactional
    public LocalizationValue setTranslation(String localizationId, String locale, String value, String sysUserId) {
        return getByLocalizationIdAndLocale(localizationId, locale).map(lv -> {
            lv.setValue(value);
            lv.setSysUserId(sysUserId);
            return update(lv);
        }).orElseGet(() -> {
            Localization localization = localizationDAO.get(localizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Localization not found: " + localizationId));
            LocalizationValue lv = new LocalizationValue(locale, value);
            lv.setLocalization(localization);
            lv.setSysUserId(sysUserId);
            insert(lv);
            return lv;
        });
    }
}
