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

package org.openelisglobal.localization.valueholder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.i18n.LocaleContextHolder;

public class Localization extends BaseObject<String> {

    private static final long serialVersionUID = -7778285878061281494L;

    private String id;
    private String description;
    private Map<Locale, String> localeValues = new HashMap<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    // these methods are here until we have time to refactor the database
    // to store the localeValues Map in its own table instead of as columns in
    // localization
    public String getFrench() {
        if (localeValues.get(Locale.FRANCE) != null) {
            return localeValues.get(Locale.FRANCE);
        } else if (localeValues.get(Locale.FRENCH) != null) {
            return localeValues.get(Locale.FRENCH);
        } else {
            return "";
        }
    }

    // these methods are here until we have time to refactor the database
    // to store the localeValues Map in its own table instead of as columns in
    // localization
    public void setFrench(String french) {
        setLocalizedValue(Locale.FRENCH, french);
    }

    // these methods are here until we have time to refactor the database
    // to store the localeValues Map in its own table instead of as columns in
    // localization
    public String getEnglish() {
        if (localeValues.get(Locale.US) != null) {
            return localeValues.get(Locale.US);
        } else if (localeValues.get(Locale.ENGLISH) != null) {
            return localeValues.get(Locale.ENGLISH);
        } else {
            return "";
        }
    }

    // these methods are here until we have time to refactor the database
    // to store the localeValues Map in its own table instead of as columns in
    // localization
    public void setEnglish(String english) {
        setLocalizedValue(Locale.ENGLISH, english);
    }

    public Map<Locale, String> getLocaleValues() {
        return localeValues;
    }

    public void setLocaleValues(Map<Locale, String> localeValues) {
        this.localeValues = localeValues;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocalizedValue() {
        return getLocalizedValue(LocaleContextHolder.getLocale());
    }

    public String getLocalizedValue(Locale locale) {
        Locale secondaryLocale = Locale.forLanguageTag(locale.getLanguage());
        if (localeValues.containsKey(locale)) {
            return localeValues.get(locale);
        } else if (localeValues.containsKey(secondaryLocale)) {
            return localeValues.get(secondaryLocale);
        } else {
            return "";
        }
    }

    public void setLocalizedValue(Locale locale, String value) {
        localeValues.put(locale, value);
    }

    public void setLocalizedValue(String value) {
        setLocalizedValue(LocaleContextHolder.getLocale(), value);
    }

    public List<Locale> getAllActiveLocales() {
        return SpringContext.getBean(LocalizationService.class).getAllActiveLocales();
    }

    public List<Locale> getLocalesSortedForDisplay() {
        List<Locale> locales = getAllActiveLocales();
        sortLocales(locales);
        return locales;
    }

    public List<Locale> getLocalesWithValue() {
        List<Locale> locales = getAllActiveLocales();
        for (Locale locale : locales) {
            if (GenericValidator.isBlankOrNull(localeValues.get(locale))) {
                locales.remove(locale);
            }
        }
        return new ArrayList<>(locales);
    }

    public List<Locale> getLocalesWithValueSortedForDisplay() {
        List<Locale> locales = getLocalesWithValue();
        sortLocales(locales);
        return locales;
    }

    public List<String> getLocalesAndValuesOfLocalesWithValues() {
        List<String> localizationValues = new ArrayList<>();
        Locale displayLocale = LocaleContextHolder.getLocale();
        for (Locale localeWithValue : getLocalesWithValueSortedForDisplay()) {
            localizationValues
                    .add(localeWithValue.getDisplayLanguage(displayLocale) + ": " + getLocalizedValue(localeWithValue));
        }
        return localizationValues;
    }

    private void sortLocales(List<Locale> locales) {
        Locale displayLocale = LocaleContextHolder.getLocale();
        Comparator<Locale> comparator = new Comparator<Locale>() {
            @Override
            public int compare(Locale o1, Locale o2) {
                return o1.getDisplayLanguage(displayLocale).compareTo(o2.getDisplayLanguage(displayLocale));
            }
        };
        Collections.sort(locales, comparator);
    }
}
