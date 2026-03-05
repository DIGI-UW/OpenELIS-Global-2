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
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.localization.valueholder.LocalizationValue;

public interface LocalizationValueDAO extends BaseDAO<LocalizationValue, String> {

    /**
     * Get all translation values for a specific localization entry.
     *
     * @param localizationId the ID of the parent Localization
     * @return list of LocalizationValue entries
     */
    List<LocalizationValue> getByLocalizationId(String localizationId);

    /**
     * Get a specific translation value for a localization and locale.
     *
     * @param localizationId the ID of the parent Localization
     * @param locale         the locale code (e.g., "en", "fr")
     * @return the LocalizationValue if found
     */
    Optional<LocalizationValue> getByLocalizationIdAndLocale(String localizationId, String locale);

    /**
     * Get all translation values for a specific locale.
     *
     * @param locale the locale code (e.g., "en", "fr")
     * @return list of LocalizationValue entries for the locale
     */
    List<LocalizationValue> getByLocale(String locale);
}
