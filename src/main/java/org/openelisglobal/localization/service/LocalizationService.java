package org.openelisglobal.localization.service;

import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public interface LocalizationService extends BaseObjectService<Localization, String> {

    @Override
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_MANAGE')")
    String insert(Localization localization);

    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_MANAGE')")
    boolean languageChanged(Localization localization, Localization oldLocalization);

    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_MANAGE')")
    void updateTestNames(Localization name, Localization reportingName);

    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    String getCurrentLocaleLanguage();

    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    String getLocalizedValueById(String id);

    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    List<Locale> getAllActiveLocales();

    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    Locale getCurrentLocale();

    /**
     * Find localizations missing a translation for the specified locale.
     *
     * @param locale the locale code to check
     * @return list of Localization entities missing translations
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    List<Localization> findMissingTranslationsForLocale(String locale);

    /**
     * Count localizations that have a non-empty translation for the specified
     * locale.
     *
     * @param locale the locale code to check
     * @return count of translated entries
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    int countTranslatedForLocale(String locale);

    /**
     * Get translation statistics for all active locales in a single query.
     *
     * @return list of Object arrays [localeCode, displayName, translated, missing]
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    List<Object[]> getTranslationStatsForAllActiveLocales();
}
