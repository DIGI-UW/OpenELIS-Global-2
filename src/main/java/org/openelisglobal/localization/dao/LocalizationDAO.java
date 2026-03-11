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

package org.openelisglobal.localization.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.localization.valueholder.Localization;

/** */
public interface LocalizationDAO extends BaseDAO<Localization, String> {

    /**
     * Find localizations that are missing a translation for the specified locale. A
     * translation is considered "missing" if no LocalizationValue exists for that
     * locale, or if the value is null or empty.
     *
     * @param locale the locale code to check (e.g., "fr", "es", "fr-CI")
     * @return list of Localization entities missing translations for the locale
     */
    List<Localization> findMissingTranslationsForLocale(String locale);

    /**
     * Count localizations that have a non-empty translation for the specified
     * locale.
     *
     * @param locale the locale code to check
     * @return count of localizations with translations for the locale
     */
    int countTranslatedForLocale(String locale);

    /**
     * Get translation statistics for all active locales in a single query. Returns
     * rows with: localeCode, displayName, translatedCount, missingCount
     *
     * @return list of Object arrays [localeCode, displayName, translated, missing]
     */
    List<Object[]> getTranslationStatsForAllActiveLocales();

}
