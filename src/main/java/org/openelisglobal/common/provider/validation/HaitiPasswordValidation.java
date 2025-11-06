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

public class HaitiPasswordValidation implements ILoginPasswordValidation {

    /*
     * Current Haiti requirements # Password must:be at least 7 characters long #
     * contain only upper or lower case characters # numbers # must have at least
     * one of the following characters (*,$,#,!)
     *
     */

    private static final Pattern NEGATION_Of_MUST_ONLY_CONTAIN = Pattern.compile("[^\\w*$#!]");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[*$#!]+");

    public boolean passwordValid(String password) {
        // make sure it is long enough and
        // make sure it only contains the characters we want and
        // make sure it contains at least one special character

        return !GenericValidator.isBlankOrNull(password) && password.length() >= 7
                && !NEGATION_Of_MUST_ONLY_CONTAIN.matcher(password).find() && SPECIAL_CHARS.matcher(password).find();
    }

    public String getInstructions() {
        return MessageUtil.getMessage("login.complexity.message");
    }
}
