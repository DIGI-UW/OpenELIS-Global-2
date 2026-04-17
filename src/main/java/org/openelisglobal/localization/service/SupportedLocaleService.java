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
import java.util.Locale;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.localization.valueholder.SupportedLocale;
import org.springframework.security.access.prepost.PreAuthorize;

public interface SupportedLocaleService extends BaseObjectService<SupportedLocale, String> {

    /**
     * Get all active locales ordered by sortOrder.
     *
     * @return list of active SupportedLocale entries
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    List<SupportedLocale> getAllActive();

    /**
     * Get all active locales as Java Locale objects.
     *
     * @return list of active Locale objects
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    List<Locale> getAllActiveAsLocales();

    /**
     * Get the fallback locale (typically English).
     *
     * @return the fallback SupportedLocale if one exists
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    Optional<SupportedLocale> getFallback();

    /**
     * Get the fallback locale code (e.g., "en").
     *
     * @return the fallback locale code, defaults to "en" if none configured
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    String getFallbackLocaleCode();

    /**
     * Get a locale by its code (e.g., "en", "fr").
     *
     * @param localeCode the locale code
     * @return the SupportedLocale if found
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_VIEW')")
    Optional<SupportedLocale> getByLocaleCode(String localeCode);

    /**
     * Set a locale as the fallback, atomically clearing any existing fallback. This
     * operation is transactional to ensure database consistency.
     *
     * @param localeId  the ID of the locale to set as fallback
     * @param sysUserId the user ID performing the operation
     * @return the updated SupportedLocale
     * @throws IllegalArgumentException if the locale ID is not found
     */
    @PreAuthorize("hasAuthority('PRIV_LOCALIZATION_MANAGE')")
    SupportedLocale setFallback(String localeId, String sysUserId);
}
