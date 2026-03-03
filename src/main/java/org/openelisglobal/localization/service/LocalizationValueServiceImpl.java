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

import java.util.HashMap;
import java.util.List;
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
        // Try the requested locale first
        Optional<LocalizationValue> value = getByLocalizationIdAndLocale(localizationId, locale);
        if (value.isPresent()) {
            return value.get().getValue();
        }

        // Fall back to the configured fallback locale
        String fallbackLocale = supportedLocaleService.getFallbackLocaleCode();
        if (!fallbackLocale.equals(locale)) {
            value = getByLocalizationIdAndLocale(localizationId, fallbackLocale);
            if (value.isPresent()) {
                return value.get().getValue();
            }
        }

        // Last resort: return any available value
        List<LocalizationValue> allValues = getByLocalizationId(localizationId);
        if (!allValues.isEmpty()) {
            return allValues.get(0).getValue();
        }

        return "";
    }

    @Override
    @Transactional
    public LocalizationValue setTranslation(String localizationId, String locale, String value) {
        Optional<LocalizationValue> existing = getByLocalizationIdAndLocale(localizationId, locale);

        if (existing.isPresent()) {
            LocalizationValue lv = existing.get();
            lv.setValue(value);
            return update(lv);
        } else {
            LocalizationValue lv = new LocalizationValue(locale, value);
            Localization localization = localizationDAO.get(localizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Localization not found: " + localizationId));
            lv.setLocalization(localization);
            insert(lv);
            return lv;
        }
    }
}
