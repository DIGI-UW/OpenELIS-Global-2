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

import java.util.regex.Pattern;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.internationalization.MessageUtil;

public class MinnPasswordValidation implements ILoginPasswordValidation {

    /*
     * Current requirements -- validation.xml only checks that there are one or more
     * characters that are alpha-numeric
     *
     * Remember passwords must meet the following complexity requirementsThe
     * password is at least eight characters long. The password does not contain
     * three or more characters from the user name. The password contains characters
     * from at least three of the following four categories:
     *
     * English uppercase characters (A - Z) English lowercase characters (a - z)
     * Base 10 digits (0 - 9) Non-alphanumeric (For example: !, $, #, or %)
     */
    private static final Pattern NEGATION_OF_MUST_ONLY_CONTAIN = Pattern.compile("[^\\w%$#!]");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[%$#!]+");
    private static final Pattern LOWER_CASE = Pattern.compile("[a-z]+");
    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]+");
    private static final Pattern DIGIT = Pattern.compile("\\d+");
    private static final String INSTRUCTION_KEY = "login.changePass.message";

    public boolean passwordValid(String password) {

        // make sure it's not null
        // make sure it is long enough
        // make sure it only contains the characters we want
        if (GenericValidator.isBlankOrNull(password) || password.length() < 8
                || NEGATION_OF_MUST_ONLY_CONTAIN.matcher(password).find()) {
            return false;
        }

        // make sure it has three of the four classes
        boolean missedOne = false;
        if (!LOWER_CASE.matcher(password).find()) {
            missedOne = true;
        }

        if (!UPPER_CASE.matcher(password).find()) {
            if (missedOne) {
                return false;
            }

            missedOne = true;
        }

        if (!DIGIT.matcher(password).find()) {
            if (missedOne) {
                return false;
            }

            missedOne = true;
        }

        if (!SPECIAL_CHAR.matcher(password).find()) {
            if (missedOne) {
                return false;
            }
        }

        // make sure it only has two characters of the users login name
        // TODO -- not clear if this is a current requirement for Minnesota. Waiting
        // for confirmation
        return true;
    }

    public String getInstructions() {
        return MessageUtil.getMessage(getResourceKeyForInstructions());
    }

    public String getResourceKeyForInstructions() {
        return INSTRUCTION_KEY;
    }
}
