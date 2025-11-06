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
 */
package org.openelisglobal.common.util.validator;

import java.io.Serializable;
import java.util.Locale;
import org.apache.commons.validator.routines.DateValidator;

/** This class contains basic methods for performing validations. */
public class GenericValidator extends org.apache.commons.validator.GenericValidator implements Serializable {

    /**
     * &lt;p&gt;Checks if the field is a valid date. The
     * &lt;code&gt;Locale&lt;/code&gt; is used with
     * &lt;code&gt;java.text.DateFormat&lt;/code&gt;. The setLenient method is set
     * to &lt;code&gt;false&lt;/code&gt; for all.&lt;/p&gt;
     *
     * @param value  The value validation is being performed on.
     * @param locale The locale to use for the date format, defaults to the default
     *               system default if null.
     */
    public static boolean isDateFormat(String value, Locale locale) {
        return DateValidator.getInstance().isValid(value, locale);
    }

    /**
     * &lt;p&gt;Checks if the field is a valid date. The pattern is used with
     * &lt;code&gt;java.text.SimpleDateFormat&lt;/code&gt;. If strict is true, then
     * the length will be checked so '2/12/1999' will not pass validation with the
     * format 'MM/dd/yyyy' because the month isn't two digits. The setLenient method
     * is set to &lt;code&gt;false&lt;/code&gt; for all.&lt;/p&gt;
     *
     * @param value       The value validation is being performed on.
     * @param datePattern The pattern passed to
     *                    &lt;code&gt;SimpleDateFormat&lt;/code&gt;.
     * @param strict      Whether or not to have an exact match of the datePattern.
     */
    public static boolean isDateFormat(String value, String datePattern, boolean strict) {
        return CustomDateValidator.getInstance().isValid(value, datePattern, strict);
    }

    public static boolean isBool(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    public static boolean is24HourTime(String value) {
        if (value == null) {
            return false;
        } else if (value.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) {
            return true;
        } else {
            return false;
        }
    }
}
