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

package org.openelisglobal.common.services;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.springframework.stereotype.Service;

/** */
@Service
public class PhoneNumberService {
    private static final Object lock = new Object();
    private static String rawFormat = null;
    private static String regex = null;
    private static final String FORMAT_REGEX = "[a-zA-Z0-9\\+\\(\\)\\s-]+";

    public static String getPhoneFormat() {
        return ConfigurationProperties.getInstance().getPropertyValue(ConfigurationProperties.Property.PHONE_FORMAT);
    }

    /**
     * Validates a phone number according to the given format but allows for an
     * arbitrary extension
     *
     * @param number to be validated
     * @return true if valid false otherwise
     */
    public boolean validatePhoneNumber(String number) {
        if (ConfigurationProperties.getInstance()
                .isPropertyValueEqual(ConfigurationProperties.Property.VALIDATE_PHONE_FORMAT, "false")
                || GenericValidator.isBlankOrNull(number)) {
            return true;
        }

        if (!getPhoneFormat().equals(rawFormat)) {
            synchronized (lock) {
                rawFormat = getPhoneFormat();
                buildRegEx();
            }
        }

        return number.matches(regex);
    }

    private void buildRegEx() {
        regex = rawFormat.replaceAll("[a-z]", "\\\\d").replaceAll(" ", "\\\\s").replaceAll("\\(", "\\\\(")
                .replaceAll("\\)", "\\\\)").replaceAll("\\+", "\\\\+") + "(\\s+.*)?";
    }

    public static boolean validatePhoneFormat(String format) {
        return format.matches(FORMAT_REGEX);
    }
}
