package org.openelisglobal.localization;

import java.util.Locale;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.localization.valueholder.Localization;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * The label of a reference value that OpenELIS ships with a display key (NCE
 * categories and types, shipped dictionary entries; OGC-1507 BR-1).
 *
 * <p>
 * A value ships when its display key is in the interface catalogue
 * ({@code en.json}), whose translations come from Transifex. For the user's
 * language its label is, in order: the lab's own label for that language, the
 * key's translation, the translation the configuration file carried while
 * Transifex has none, the lab's own English, and the key's English. A value
 * whose key is not in the catalogue is the site's own and keeps its stored
 * labels.
 */
public final class ShippedLabels {

    public enum Origin {
        SHIPPED, SITE_OVERRIDE, SITE_VALUE
    }

    private ShippedLabels() {
    }

    public static boolean isShipped(String displayKey) {
        return UiMessages.defines(displayKey);
    }

    /** The label in the current user's language. */
    public static String label(String displayKey, Localization stored, String name) {
        return label(displayKey, stored, name, LocaleContextHolder.getLocale());
    }

    public static String label(String displayKey, Localization stored, String name, Locale locale) {
        Locale resolved = locale == null ? Locale.ENGLISH : locale;
        if (!isShipped(displayKey)) {
            return storedLabel(stored, name, resolved);
        }
        String site = siteLabel(stored, resolved);
        if (site != null) {
            return site;
        }
        String translation = translation(displayKey, stored, resolved);
        if (translation != null) {
            return translation;
        }
        String siteEnglish = stored == null ? null : stored.getSiteValue(Locale.ENGLISH.getLanguage());
        return siteEnglish != null ? siteEnglish : englishLabel(displayKey);
    }

    /**
     * The lab's own label for the locale: for its regional language (fr-HT) when
     * there is one, else for its language.
     */
    private static String siteLabel(Localization stored, Locale locale) {
        if (stored == null) {
            return null;
        }
        String regional = locale.getCountry().isEmpty() ? null : stored.getSiteValue(locale.toLanguageTag());
        return regional != null ? regional : stored.getSiteValue(locale.getLanguage());
    }

    /**
     * The label a shipped value shows in a language when the lab has not changed
     * it: the key's translation, else the configuration file's translation, else
     * the key's English.
     */
    public static String shippedLabel(String displayKey, Localization stored, Locale locale) {
        String translation = translation(displayKey, stored, locale == null ? Locale.ENGLISH : locale);
        return translation != null ? translation : englishLabel(displayKey);
    }

    /**
     * The key's text for the locale when it is English or Transifex translated it,
     * else the configuration file's translation, else null.
     */
    private static String translation(String displayKey, Localization stored, Locale locale) {
        String key = displayKey.trim();
        String translated = UiMessages.cached(locale).get(key);
        if (Locale.ENGLISH.getLanguage().equals(locale.getLanguage()) || !translated.equals(englishLabel(key))) {
            return translated;
        }
        return stored == null ? null : stored.getSystemValue(locale.getLanguage());
    }

    private static String englishLabel(String displayKey) {
        return UiMessages.cached(Locale.ENGLISH).get(displayKey.trim());
    }

    /**
     * Where the label stored for a supported locale code (fr, fr-HT) comes from
     * (OGC-1507 FR-6).
     */
    public static Origin origin(String displayKey, Localization stored, String localeCode) {
        if (!isShipped(displayKey)) {
            return Origin.SITE_VALUE;
        }
        return stored != null && stored.getSiteValue(localeCode) != null ? Origin.SITE_OVERRIDE : Origin.SHIPPED;
    }

    private static String storedLabel(Localization stored, String name, Locale locale) {
        if (stored != null) {
            String value = stored.getLocalizedValue(locale);
            if (!GenericValidator.isBlankOrNull(value)) {
                return value;
            }
        }
        return name;
    }
}
