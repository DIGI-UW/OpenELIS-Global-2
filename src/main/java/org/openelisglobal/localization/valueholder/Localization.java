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

    private static final String FALLBACK_LOCALE = "en";

    private String id;
    private String description;

    /**
     * New normalized translation values stored in localization_value table.
     * Key is locale code (e.g., "en", "fr"), value is the LocalizationValue entity.
     */
    private Map<String, LocalizationValue> values = new HashMap<>();

    /**
     * Legacy locale values map used by the old Hibernate mapping.
     * This will be populated from the english/french columns during the transition period.
     *
     * @deprecated Use {@link #values} instead. This field will be removed after Phase 4.
     */
    @Deprecated
    private Map<Locale, String> localeValues = new HashMap<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get all translation values as a map of locale code to LocalizationValue.
     *
     * @return map of locale codes to LocalizationValue entities
     */
    public Map<String, LocalizationValue> getValues() {
        return values;
    }

    /**
     * Set all translation values.
     *
     * @param values map of locale codes to LocalizationValue entities
     */
    public void setValues(Map<String, LocalizationValue> values) {
        this.values = values;
    }

    /**
     * Get the French translation.
     *
     * @return the French translation, or empty string if not found
     * @deprecated Use {@link #getLocalizedValue(Locale)} with Locale.FRENCH instead.
     *             This method will be removed after Phase 4.
     */
    @Deprecated
    public String getFrench() {
        // First try the new values map
        if (values != null && values.containsKey("fr")) {
            return values.get("fr").getValue();
        }
        // Fall back to legacy localeValues
        if (localeValues.get(Locale.FRANCE) != null) {
            return localeValues.get(Locale.FRANCE);
        } else if (localeValues.get(Locale.FRENCH) != null) {
            return localeValues.get(Locale.FRENCH);
        } else {
            return "";
        }
    }

    /**
     * Set the French translation.
     *
     * @param french the French translation value
     * @deprecated Use {@link #setLocalizedValue(String, String)} with "fr" locale instead.
     *             This method will be removed after Phase 4.
     */
    @Deprecated
    public void setFrench(String french) {
        setLegacyLocalizedValue(Locale.FRENCH, french);
        // Also update new values map if it exists
        if (values != null) {
            LocalizationValue lv = values.get("fr");
            if (lv != null) {
                lv.setValue(french);
            } else {
                lv = new LocalizationValue("fr", french);
                lv.setLocalization(this);
                values.put("fr", lv);
            }
        }
    }

    /**
     * Get the English translation.
     *
     * @return the English translation, or empty string if not found
     * @deprecated Use {@link #getLocalizedValue(Locale)} with Locale.ENGLISH instead.
     *             This method will be removed after Phase 4.
     */
    @Deprecated
    public String getEnglish() {
        // First try the new values map
        if (values != null && values.containsKey("en")) {
            return values.get("en").getValue();
        }
        // Fall back to legacy localeValues
        if (localeValues.get(Locale.US) != null) {
            return localeValues.get(Locale.US);
        } else if (localeValues.get(Locale.ENGLISH) != null) {
            return localeValues.get(Locale.ENGLISH);
        } else {
            return "";
        }
    }

    /**
     * Set the English translation.
     *
     * @param english the English translation value
     * @deprecated Use {@link #setLocalizedValue(String, String)} with "en" locale instead.
     *             This method will be removed after Phase 4.
     */
    @Deprecated
    public void setEnglish(String english) {
        setLegacyLocalizedValue(Locale.ENGLISH, english);
        // Also update new values map if it exists
        if (values != null) {
            LocalizationValue lv = values.get("en");
            if (lv != null) {
                lv.setValue(english);
            } else {
                lv = new LocalizationValue("en", english);
                lv.setLocalization(this);
                values.put("en", lv);
            }
        }
    }

    /**
     * Get the legacy locale values map.
     *
     * @return map of Locale to translation value
     * @deprecated Use {@link #getValues()} instead. This method will be removed after Phase 4.
     */
    @Deprecated
    public Map<Locale, String> getLocaleValues() {
        return localeValues;
    }

    /**
     * Set the legacy locale values map.
     *
     * @param localeValues map of Locale to translation value
     * @deprecated Use {@link #setValues(Map)} instead. This method will be removed after Phase 4.
     */
    @Deprecated
    public void setLocaleValues(Map<Locale, String> localeValues) {
        this.localeValues = localeValues;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the localized value for the current locale.
     *
     * @return the translation for the current locale, with fallback to English
     */
    public String getLocalizedValue() {
        return getLocalizedValue(LocaleContextHolder.getLocale());
    }

    /**
     * Get the localized value for a specific locale.
     * Falls back to English if the requested locale is not found.
     *
     * @param locale the desired locale
     * @return the translation, or empty string if not found
     */
    public String getLocalizedValue(Locale locale) {
        String localeCode = locale.getLanguage();

        // First try the new values map (preferred)
        if (values != null && !values.isEmpty()) {
            // Try exact locale match
            LocalizationValue lv = values.get(localeCode);
            if (lv != null && !GenericValidator.isBlankOrNull(lv.getValue())) {
                return lv.getValue();
            }
            // Fall back to English
            lv = values.get(FALLBACK_LOCALE);
            if (lv != null && !GenericValidator.isBlankOrNull(lv.getValue())) {
                return lv.getValue();
            }
            // Last resort: return any available value
            for (LocalizationValue value : values.values()) {
                if (!GenericValidator.isBlankOrNull(value.getValue())) {
                    return value.getValue();
                }
            }
        }

        // Fall back to legacy localeValues map
        Locale secondaryLocale = Locale.forLanguageTag(localeCode);
        if (localeValues.containsKey(locale)) {
            return localeValues.get(locale);
        } else if (localeValues.containsKey(secondaryLocale)) {
            return localeValues.get(secondaryLocale);
        } else {
            return "";
        }
    }

    /**
     * Get the localized value for a specific locale code.
     *
     * @param localeCode the locale code (e.g., "en", "fr")
     * @return the translation, or empty string if not found
     */
    public String getLocalizedValue(String localeCode) {
        return getLocalizedValue(Locale.forLanguageTag(localeCode));
    }

    /**
     * Set the translation for a specific locale (legacy method).
     *
     * @param locale the locale
     * @param value the translation value
     * @deprecated Use {@link #setLocalizedValue(String, String)} instead
     */
    @Deprecated
    public void setLocalizedValue(Locale locale, String value) {
        setLegacyLocalizedValue(locale, value);
        // Also update new values map
        String localeCode = locale.getLanguage();
        if (values != null) {
            LocalizationValue lv = values.get(localeCode);
            if (lv != null) {
                lv.setValue(value);
            } else {
                lv = new LocalizationValue(localeCode, value);
                lv.setLocalization(this);
                values.put(localeCode, lv);
            }
        }
    }

    /**
     * Internal method to set legacy locale value without triggering recursion.
     */
    private void setLegacyLocalizedValue(Locale locale, String value) {
        localeValues.put(locale, value);
    }

    /**
     * Set the translation for a specific locale code.
     *
     * @param localeCode the locale code (e.g., "en", "fr")
     * @param value the translation value
     */
    public void setLocalizedValue(String localeCode, String value) {
        // Update new values map
        if (values != null) {
            LocalizationValue lv = values.get(localeCode);
            if (lv != null) {
                lv.setValue(value);
            } else {
                lv = new LocalizationValue(localeCode, value);
                lv.setLocalization(this);
                values.put(localeCode, lv);
            }
        }
        // Also update legacy map for backwards compatibility
        localeValues.put(Locale.forLanguageTag(localeCode), value);
    }

    /**
     * Set the translation for the current locale.
     *
     * @param value the translation value
     */
    public void setLocalizedValue(String value) {
        setLocalizedValue(LocaleContextHolder.getLocale().getLanguage(), value);
    }

    public List<Locale> getAllActiveLocales() {
        return SpringContext.getBean(LocalizationService.class).getAllActiveLocales();
    }

    public List<Locale> getLocalesSortedForDisplay() {
        List<Locale> locales = new ArrayList<>(getAllActiveLocales());
        sortLocales(locales);
        return locales;
    }

    public List<Locale> getLocalesWithValue() {
        List<Locale> result = new ArrayList<>();

        // Check new values map first
        if (values != null && !values.isEmpty()) {
            for (Map.Entry<String, LocalizationValue> entry : values.entrySet()) {
                if (!GenericValidator.isBlankOrNull(entry.getValue().getValue())) {
                    result.add(Locale.forLanguageTag(entry.getKey()));
                }
            }
        } else {
            // Fall back to legacy localeValues
            for (Locale locale : getAllActiveLocales()) {
                if (!GenericValidator.isBlankOrNull(localeValues.get(locale))) {
                    result.add(locale);
                }
            }
        }

        return result;
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

    /**
     * Get all translation values as a simple map of locale code to value string.
     *
     * @return map of locale codes to translation strings
     */
    public Map<String, String> getValuesAsMap() {
        Map<String, String> result = new HashMap<>();
        if (values != null) {
            for (Map.Entry<String, LocalizationValue> entry : values.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getValue());
            }
        }
        return result;
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
