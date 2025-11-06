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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.apache.commons.validator.routines.DateValidator;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;

public class CustomDateValidator extends DateValidator {

    private static final long serialVersionUID = 8623867024483764609L;

    public enum DateRelation {
        PAST, FUTURE, TODAY, ANY
    }

    private static class SingletonHelper {
        private static final CustomDateValidator INSTANCE = new CustomDateValidator();
    }

    public static CustomDateValidator getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /** Protected constructor for subclasses to use. */
    protected CustomDateValidator() {
        super();
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
    public boolean isValid(String value, String datePattern, boolean strict) {
        if (value == null || datePattern == null || datePattern.length() <= 0) {
            return false;
        }
        SimpleDateFormat formatter = new SimpleDateFormat(datePattern);
        formatter.setLenient(false);
        try {
            formatter.parse(value);
        } catch (ParseException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            return false;
        }
        if (strict && (datePattern.length() != value.length())) {
            return false;
        }
        return true;
    }

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
    @Override
    public boolean isValid(String value, Locale locale) {
        if (value == null) {
            return false;
        }

        DateFormat formatter = null;
        if (locale != null) {
            formatter = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        } else {
            formatter = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        }
        formatter.setLenient(false);
        try {
            formatter.parse(value);
        } catch (ParseException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            return false;
        }
        return true;
    }

    public Date getDate(String date) {
        Locale locale = Locale
                .forLanguageTag(ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE));
        return validate(date, locale);
    }

    public String validateDate(Date date, DateRelation relative) {
        String result = IActionConstants.VALID;

        if (date == null) {
            result = IActionConstants.INVALID;
            return result;
        }

        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date today = calendar.getTime();

        // time insensitive compare, only year month day
        Calendar calendarDate = new GregorianCalendar();
        calendarDate.setTime(date);
        calendarDate.set(Calendar.HOUR_OF_DAY, 0);
        calendarDate.set(Calendar.MINUTE, 0);
        calendarDate.set(Calendar.SECOND, 0);
        calendarDate.set(Calendar.MILLISECOND, 0);
        date = calendarDate.getTime();

        int dateDiff = date.compareTo(today);

        if (relative.equals(DateRelation.PAST) && dateDiff > 0) {
            result = IActionConstants.INVALID_TO_LARGE;
        } else if (relative.equals(DateRelation.FUTURE) && dateDiff < 0) {
            result = IActionConstants.INVALID_TO_SMALL;
        } else if (relative.equals(DateRelation.TODAY) && dateDiff > 0) {
            result = IActionConstants.INVALID_TO_SMALL;
        } else if (relative.equals(DateRelation.TODAY) && dateDiff < 0) {
            result = IActionConstants.INVALID_TO_SMALL;
        }

        return result;
    }

    public boolean validate24HourTime(String time) {
        return time.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]");
    }
}
