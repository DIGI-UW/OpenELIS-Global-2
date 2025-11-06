/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.services;

import java.util.Locale;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.stereotype.Service;

@Service
public class LabIdentificationService {

    public String getLabId() {
        return ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode);
    }

    public String getLabName() {
        return ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName);
    }

    public String getLabAddress() {
        return "";
    }

    public String getLabPhone() {
        return "";
    }

    public Locale getLanguageLocale() {
        return Locale
                .forLanguageTag(ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE));
    }
}
