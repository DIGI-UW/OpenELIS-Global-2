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

import java.util.Locale;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Represents a supported language/locale in the system. This table drives which
 * languages are available for metadata translations.
 */
public class SupportedLocale extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private String localeCode;
    private String displayName;
    private boolean active;
    private boolean fallback;
    private int sortOrder;

    public SupportedLocale() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Converts the locale code to a Java Locale object.
     *
     * @return the Locale corresponding to this SupportedLocale
     */
    public Locale toLocale() {
        return Locale.forLanguageTag(localeCode);
    }
}
