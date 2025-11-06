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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.provider.validation;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class PasswordValidationFactory {

    private static final String MINN_SITE = "MINN";
    private static final String HAITI_SITE = "HAITI";
    private static final String CDI_SITE = "CDI";

    public static ILoginPasswordValidation getPasswordValidator() {

        String requirementSite = ConfigurationProperties.getInstance()
                .getPropertyValueUpperCase(Property.PasswordRequirments);
        ILoginPasswordValidation validator = new MinnPasswordValidation();

        if (MINN_SITE.equals(requirementSite)) {
            // no-op Minnesota is the default
        } else if (HAITI_SITE.equals(requirementSite)) {
            validator = new HaitiPasswordValidation();
        } else if (CDI_SITE.equals(requirementSite)) {
            validator = new CDIPasswordValidation();
        }

        return validator;
    }
}
