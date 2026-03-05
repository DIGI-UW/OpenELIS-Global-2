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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.LocalizationValue;

public interface LocalizationValueService extends BaseObjectService<LocalizationValue, String> {

    /**
     * Get all translation values for a specific localization entry.
     *
     * @param localizationId the ID of the parent Localization
     * @return list of LocalizationValue entries
     */
    List<LocalizationValue> getByLocalizationId(String localizationId);

    /**
     * Get all translation values for a localization as a map.
     *
     * @param localizationId the ID of the parent Localization
     * @return map of locale code to value
     */
    Map<String, String> getValuesAsMap(String localizationId);

    /**
     * Get a specific translation value for a localization and locale.
     *
     * @param localizationId the ID of the parent Localization
     * @param locale         the locale code (e.g., "en", "fr")
     * @return the LocalizationValue if found
     */
    Optional<LocalizationValue> getByLocalizationIdAndLocale(String localizationId, String locale);

    /**
     * Get the localized value for a localization entry. Falls back to the fallback
     * locale (typically English) if not found.
     *
     * @param localizationId the ID of the Localization
     * @param locale         the preferred locale code
     * @return the localized value, or empty string if not found
     */
    String getLocalizedValue(String localizationId, String locale);

    /**
     * Set or update a translation value for a localization.
     *
     * @param localizationId the ID of the parent Localization
     * @param locale         the locale code
     * @param value          the translation value
     * @return the saved LocalizationValue
     */
    LocalizationValue setTranslation(String localizationId, String locale, String value);
}
