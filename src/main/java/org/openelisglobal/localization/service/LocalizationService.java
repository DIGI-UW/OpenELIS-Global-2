package org.openelisglobal.localization.service;

import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.stereotype.Service;

@Service
public interface LocalizationService extends BaseObjectService<Localization, String> {

    @Override
    String insert(Localization localization);

    boolean languageChanged(Localization localization, Localization oldLocalization);

    void updateTestNames(Localization name, Localization reportingName);

    String getCurrentLocaleLanguage();

    String getLocalizedValueById(String id);

    List<Locale> getAllActiveLocales();

    Locale getCurrentLocale();

    /**
     * Find localizations missing a translation for the specified locale.
     *
     * @param locale the locale code to check
     * @return list of Localization entities missing translations
     */
    List<Localization> findMissingTranslationsForLocale(String locale);

    /**
     * Count localizations that have a non-empty translation for the specified
     * locale.
     *
     * @param locale the locale code to check
     * @return count of translated entries
     */
    int countTranslatedForLocale(String locale);

    /**
     * Get translation statistics for all active locales in a single query.
     *
     * @return list of Object arrays [localeCode, displayName, translated, missing]
     */
    List<Object[]> getTranslationStatsForAllActiveLocales();
}
