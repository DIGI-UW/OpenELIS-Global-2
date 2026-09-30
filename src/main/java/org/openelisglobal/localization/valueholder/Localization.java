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

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Localization entity for storing translatable content.
 *
 * <p>
 * This entity supports an unlimited number of languages via the
 * localization_value table. Each Localization can have multiple
 * LocalizationValue entries, one per supported language.
 * </p>
 */
@Entity
@Table(name = "localization")
@DynamicUpdate
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class Localization extends BaseObject<String> {

    private static final long serialVersionUID = -7778285878061281494L;

    private static final String FALLBACK_LOCALE = "en";

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "localization_seq_gen")
    @GenericGenerator(name = "localization_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "localization_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "description")
    private String description;

    /**
     * Translation values stored in localization_value table. Key is locale code
     * (e.g., "en", "fr"), value is the LocalizationValue entity.
     */
    @OneToMany(mappedBy = "localization", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @MapKey(name = "locale")
    private Map<String, LocalizationValue> values = new HashMap<>();

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
     * @deprecated Use {@link #getLocalizedValue(Locale)} with Locale.FRENCH instead
     */
    @Deprecated
    public String getFrench() {
        if (values != null && values.containsKey("fr")) {
            return values.get("fr").getValue();
        }
        return "";
    }

    /**
     * Set the French translation.
     *
     * @param french the French translation value
     * @deprecated Use {@link #setLocalizedValue(String, String)} with "fr" locale
     *             instead
     */
    @Deprecated
    public void setFrench(String french) {
        setLocalizedValue("fr", french);
    }

    /**
     * Get the English translation.
     *
     * @return the English translation, or empty string if not found
     * @deprecated Use {@link #getLocalizedValue(Locale)} with Locale.ENGLISH
     *             instead
     */
    @Deprecated
    public String getEnglish() {
        if (values != null && values.containsKey("en")) {
            return values.get("en").getValue();
        }
        return "";
    }

    /**
     * Set the English translation.
     *
     * @param english the English translation value
     * @deprecated Use {@link #setLocalizedValue(String, String)} with "en" locale
     *             instead
     */
    @Deprecated
    public void setEnglish(String english) {
        setLocalizedValue("en", english);
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
     * Get the localized value for a specific locale. Falls back to English if the
     * requested locale is not found.
     *
     * @param locale the desired locale
     * @return the translation, or empty string if not found
     */
    public String getLocalizedValue(Locale locale) {
        String localeCode = locale.getLanguage();

        if (values == null || values.isEmpty()) {
            return "";
        }

        // Try exact locale match
        LocalizationValue lv = values.get(localeCode);
        if (lv != null && !GenericValidator.isBlankOrNull(lv.getValue())) {
            return lv.getValue();
        }

        // Fall back to English — the single universal fallback (OGC-1112 FR-23).
        // We deliberately do NOT fall back to "any other locale": that would leak a
        // language the user isn't working in (FR-24). Locale → English → blank.
        lv = values.get(FALLBACK_LOCALE);
        if (lv != null && !GenericValidator.isBlankOrNull(lv.getValue())) {
            return lv.getValue();
        }

        return "";
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
     * Set the translation for a specific locale.
     *
     * @param locale the locale
     * @param value  the translation value
     * @deprecated Use {@link #setLocalizedValue(String, String)} instead
     */
    @Deprecated
    public void setLocalizedValue(Locale locale, String value) {
        setLocalizedValue(locale.getLanguage(), value);
    }

    /**
     * Set the translation for a specific locale code.
     *
     * @param localeCode the locale code (e.g., "en", "fr")
     * @param value      the translation value
     */
    public void setLocalizedValue(String localeCode, String value) {
        if (values == null) {
            values = new HashMap<>();
        }
        LocalizationValue lv = values.get(localeCode);
        if (lv != null) {
            lv.setValue(value);
        } else {
            lv = new LocalizationValue(localeCode, value);
            lv.setLocalization(this);
            values.put(localeCode, lv);
        }
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
        if (values != null) {
            for (Map.Entry<String, LocalizationValue> entry : values.entrySet()) {
                if (!GenericValidator.isBlankOrNull(entry.getValue().getValue())) {
                    result.add(Locale.forLanguageTag(entry.getKey()));
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
