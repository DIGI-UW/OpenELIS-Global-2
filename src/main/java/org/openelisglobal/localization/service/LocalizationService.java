package org.openelisglobal.localization.service;

import java.util.List;
import java.util.Locale;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@CrossDomainService(callers = "Every page render (CommonPageAttributesInterceptor localizes the banner/title on"
        + " all requests, including pre-login /session polls), locale resolution, and localized test/panel names"
        + " in order entry, results and reports. Localized display strings are UI infrastructure, not privileged"
        + " data — reads are ungated; mutations remain gated with PRIV_LOCALIZATION_MANAGE")
public interface LocalizationService extends BaseObjectService<Localization, String> {

    @Override
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_MANAGE')")
    String insert(Localization localization);

    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_MANAGE')")
    boolean languageChanged(Localization localization, Localization oldLocalization);

    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_MANAGE')")
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
