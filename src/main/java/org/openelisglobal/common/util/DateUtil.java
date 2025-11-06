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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.common.util;

import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;

public class DateUtil {

    private static final int EPIC = 1970;
    private static final String AMBIGUOUS_DATE_REGEX;
    public static final String AMBIGUOUS_DATE_CHAR;
    public static final String AMBIGUOUS_DATE_SEGMENT;
    private static final Pattern FOUR_DIGITS = Pattern.compile("\\d{4}");
    private static final Pattern TWO_DIGITS = Pattern.compile("\\d{2}");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern VALID_DATE = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    private static final long DAY_IN_MILLSEC = 1000L * 60L * 60L * 24L;

    private static final long WEEK_MS = DAY_IN_MILLSEC * 7L;

    static {
        AMBIGUOUS_DATE_CHAR = ConfigurationProperties.getInstance().getPropertyValue(Property.AmbiguousDateHolder);
        AMBIGUOUS_DATE_REGEX = "(?i)" + AMBIGUOUS_DATE_CHAR + AMBIGUOUS_DATE_CHAR;
        AMBIGUOUS_DATE_SEGMENT = AMBIGUOUS_DATE_CHAR + AMBIGUOUS_DATE_CHAR;
    }

    public static String formatDateTimeAsText(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(getDateTimeFormat());
        return format.format(date);
    }

    public static String formatTimeAsText(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(getTimeFormat());
        return format.format(date);
    }

    public static String formatDateAsText(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(getDateFormat());
        return format.format(date);
    }

    public static java.sql.Date convertStringDateToSqlDate(String date) {
        String stringLocale = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE);

        return convertStringDateToSqlDate(date, stringLocale);
    }

    public static LocalDate convertStringDateToLocalDate(String date) {
        Locale locale = Locale
                .forLanguageTag(ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE));

        return convertStringDateToLocalDate(date, locale);
    }

    private static LocalDate convertStringDateToLocalDate(String date, Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(getDateFormat());
        LocalDate returnDate = null;

        if (!StringUtil.isNullorNill(date)) {
            formatter = formatter.withLocale(locale);
            returnDate = LocalDate.parse(date, formatter);
        }
        return returnDate;
    }

    public static java.sql.Date convertStringDateToSqlDate(String date, String stringLocale)
            throws LIMSRuntimeException {
        SimpleDateFormat format = new SimpleDateFormat(getDateFormat());
        format.setLenient(false);
        java.sql.Date returnDate = null;

        if (!StringUtil.isNullorNill(date)) {
            try {
                returnDate = new java.sql.Date(format.parse(date).getTime());
            } catch (ParseException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error parsing date", e);
            }
        }
        return returnDate;
    }

    public static java.sql.Date convertStringDateTimeToSqlDate(String date) throws LIMSRuntimeException {
        SimpleDateFormat format = new SimpleDateFormat(getDateTimeFormat());
        java.sql.Date returnDate = null;
        if (!StringUtil.isNullorNill(date)) {
            try {
                returnDate = new java.sql.Date(format.parse(date).getTime());
            } catch (ParseException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error parsing date", e);
            }
        }
        return returnDate;
    }

    public static Timestamp convertStringDateToTruncatedTimestamp(String date) throws LIMSRuntimeException {
        SimpleDateFormat format = new SimpleDateFormat(getDateFormat());
        Timestamp returnTimestamp = null;

        if (!StringUtil.isNullorNill(date)) {
            try {
                returnTimestamp = new Timestamp(format.parse(date).getTime());
            } catch (ParseException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error parsing date", e);
            }
        }
        return returnTimestamp;
    }

    public static Timestamp convertStringDateToTimestamp(String date) throws LIMSRuntimeException {
        SimpleDateFormat format = new SimpleDateFormat(getDateTimeFormat());
        Timestamp returnTimestamp = null;

        if (!StringUtil.isNullorNill(date)) {
            try {
                returnTimestamp = new Timestamp(format.parse(date).getTime());
            } catch (ParseException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error parsing date", e);
            }
        }
        return returnTimestamp;
    }

    public static Timestamp convertStringDateToTimestampWithPatternNoLocale(String date, String pattern)
            throws LIMSRuntimeException {
        SimpleDateFormat format = new SimpleDateFormat(pattern);

        Timestamp returnTimestamp = null;
        if (!StringUtil.isNullorNill(date)) {
            try {
                returnTimestamp = new Timestamp(format.parse(date).getTime());
            } catch (ParseException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error parsing date", e);
            }
        }
        return returnTimestamp;
    }

    public static Timestamp convertStringDateToTimestampWithPattern(String date, String pattern)
            throws LIMSRuntimeException {
        Locale locale = Locale
                .forLanguageTag(ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_LANG_LOCALE));
        SimpleDateFormat format = new SimpleDateFormat(pattern, locale);

        Timestamp returnTimestamp = null;
        if (!StringUtil.isNullorNill(date)) {
            try {
                returnTimestamp = new Timestamp(format.parse(date).getTime());
            } catch (ParseException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error parsing date", e);
            }
        }
        return returnTimestamp;
    }

    // TIMESTAMP
    public static Timestamp convertStringTimeToTimestamp(Timestamp date, String time) throws LIMSRuntimeException {
        if (!StringUtil.isNullorNill(time) && date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(0, 2)));
            cal.set(Calendar.MINUTE, Integer.valueOf(time.substring(3, 5)));
            date = new Timestamp(cal.getTimeInMillis());
        }
        return date;
    }

    public static String convertSqlDateToStringDate(java.sql.Date date) throws LIMSRuntimeException {
        SimpleDateFormat format = new SimpleDateFormat(getDateFormat());
        String returnDate = null;
        if (date != null) {
            try {
                returnDate = format.format(date);
            } catch (RuntimeException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error converting date", e);
            }
        }
        return returnDate;
    }

    public static String convertTimestampToStringDate(Timestamp date) throws LIMSRuntimeException {
        return convertTimestampToStringDate(date, false);
    }

    public static String convertTimestampToTwoYearStringDate(Timestamp date) throws LIMSRuntimeException {
        return convertTimestampToStringDate(date, true);
    }

    private static String convertTimestampToStringDate(Timestamp date, boolean twoYearDate)
            throws LIMSRuntimeException {
        if (date == null) {
            return "";
        }

        String pattern = getDateFormat();
        if (twoYearDate) {
            pattern = pattern.replace("yyyy", "yy");
        }

        SimpleDateFormat format = new SimpleDateFormat(pattern);
        String returnDate;

        try {
            returnDate = format.format(date);
        } catch (RuntimeException e) {

            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error converting date", e);
        }

        return returnDate;
    }

    public static String convertTimestampToStringTime(Timestamp date) throws LIMSRuntimeException {

        String returnTime = null;
        String hours;
        String minutes;
        if (date != null) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                if (cal.get(Calendar.HOUR_OF_DAY) <= 9) {
                    hours = "0" + String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
                } else {
                    hours = String.valueOf(cal.get(Calendar.HOUR_OF_DAY));
                }

                if (cal.get(Calendar.MINUTE) <= 9) {
                    minutes = "0" + String.valueOf(cal.get(Calendar.MINUTE));
                } else {
                    minutes = String.valueOf(cal.get(Calendar.MINUTE));
                }

                returnTime = hours + ":" + minutes;
            } catch (RuntimeException e) {
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error converting date", e);
            }
        }

        return returnTime;
    }

    // Decodes a time value in "hh:mm:ss" format and returns it as milliseconds
    // since midnight.
    public static synchronized int decodeTime(String s) throws LIMSException {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown", "Passed in
        // this
        // time " +s);
        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        f.setTimeZone(utcTimeZone);
        f.setLenient(false);
        ParsePosition p = new ParsePosition(0);
        Date d = f.parse(s, p);
        if (d == null || !StringUtil.isRestOfStringBlank(s, p.getIndex())) {
            throw new LIMSException("Invalid time value (hh:mm:ss): \"" + s + "\".");
        }
        return (int) d.getTime();
    }

    public static Timestamp formatStringToTimestamp(String ts) {

        StringBuffer tssb = new StringBuffer();
        tssb.append(ts);
        if (ts.length() < 23) {
            for (int i = 23; ts.length() < i; i--) {
                tssb.append("0");
            }
        }

        ts = tssb.toString();

        SimpleDateFormat format = new SimpleDateFormat(getDateTimeFormat());

        Timestamp tsToReturn = null;

        if (!GenericValidator.isBlankOrNull(ts)) {
            try {
                java.util.Date date = format.parse(ts);
                tsToReturn = new Timestamp(date.getTime());
            } catch (ParseException e) {
                // bugzilla 2154
                LogEvent.logError(e);
                throw new LIMSRuntimeException("Error converting date", e);
            }
        }
        return tsToReturn;
    }

    public static String getTwoDigitYear() {

        int year = new GregorianCalendar().get(Calendar.YEAR) - 2000;

        return String.format("%02d", year);
    }

    public static Timestamp convertAmbiguousStringDateToTimestamp(String dateForDisplay) {

        dateForDisplay = normalizeAmbiguousDate(dateForDisplay);

        return dateForDisplay == null ? null : convertStringDateToTruncatedTimestamp(dateForDisplay);
    }

    public static boolean yearSpecified(String dateString) {
        String[] dateParts = dateString.split("/");

        return dateParts.length == 3 && FOUR_DIGITS.matcher(dateParts[2]).find();
    }

    public static String normalizeAmbiguousDate(String date) {
        if (VALID_DATE.matcher(date).find()) {
            return date;
        }

        String replaceValue = ConfigurationProperties.getInstance().getPropertyValue(Property.AmbiguousDateValue);
        // N.B. This is suppose to clean-up historical data in the database. We will do
        // the best we can
        if (date.length() != 10) {
            String[] dateParts = date.split("/");
            if (dateParts.length != 3 || !FOUR_DIGITS.matcher(dateParts[2]).find()) {
                return null;
            }
            if (date.length() > 10) {
                return replaceValue + "/" + replaceValue + "/" + date.split("/")[2];
            }

            for (int i = 0; i < 2; i++) {
                if (dateParts[i].length() == 1 && DIGIT.matcher(dateParts[i]).find()) {
                    dateParts[i] = "0" + dateParts[i];
                } else if (dateParts[i].length() == 1 || !TWO_DIGITS.matcher(dateParts[i]).find()) {
                    // if there is a single 'x' or 'X' then replacing it with 01 is what we want
                    return replaceValue + "/" + replaceValue + "/" + date.split("/")[2];
                }
            }

            // if we made it here we have corrected what we can
            return dateParts[0] + "/" + dateParts[1] + "/" + dateParts[2];
        } else {
            date = StringUtil.replaceCharAtIndex(date, '/', 2);
            date = StringUtil.replaceCharAtIndex(date, '/', 5);
            if (!yearSpecified(date)) {
                return null;
            }
            date = date.replaceAll(AMBIGUOUS_DATE_REGEX, replaceValue);

            if (VALID_DATE.matcher(date).find()) {
                return date;
            } else {
                return replaceValue + "/" + replaceValue + "/" + date.split("/")[2];
            }
        }
    }

    public static java.sql.Date getNowAsSqlDate() {
        return new java.sql.Date(new Date().getTime());
    }

    public static String getCurrentAgeForDate(Timestamp birthDate, Timestamp endDate) {
        if (birthDate != null && endDate != null) {
            Period period = Period.between(birthDate.toLocalDateTime().toLocalDate(),
                    endDate.toLocalDateTime().toLocalDate());
            return String.valueOf(period.getYears());
        }

        return null;
    }

    public static Period getPeriodBetweenDates(Timestamp birthDate, Timestamp endDate) {
        if (birthDate != null) {
            return Period.between(birthDate.toLocalDateTime().toLocalDate(), endDate.toLocalDateTime().toLocalDate());
        }
        return null;
    }

    public static int getDaysInPastForDate(Date date) {
        if (date == null) {
            return 0;
        }
        long age = new Date().getTime() - date.getTime();
        return (int) Math.floor(age / DAY_IN_MILLSEC);
    }

    public static String getCurrentDateAsText() {
        return formatDateAsText(new Date());
    }

    public static String getCurrentDateAsText(String pattern) {
        if (GenericValidator.isBlankOrNull(pattern)) {
            return null;
        }

        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(new Date());
    }

    public static String getCurrentTimeAsText() {
        int hour = getCurrentHour();
        int minute = getCurrentMinute();
        return String.format("%02d:%02d", hour, minute);
    }

    public static int getAgeInWeeks(Date startDate, Date endDate) {
        long duration = endDate.getTime() - startDate.getTime();
        return (int) Math.floor(duration / WEEK_MS);
    }

    public static int getAgeInDays(Date startDate, Date endDate) {
        long duration = endDate.getTime() - startDate.getTime();
        return (int) Math.floor(duration / DAY_IN_MILLSEC);
    }

    public static int getAgeInMonths(Date startDate, Date endDate) {
        Calendar start = new GregorianCalendar();
        start.setTime(startDate);
        int startMOY = start.get(Calendar.MONTH);
        int startYear = start.get(Calendar.YEAR);
        Calendar end = new GregorianCalendar();
        end.setTime(endDate);
        int endMOY = end.get(Calendar.MONTH);
        int endYear = end.get(Calendar.YEAR);
        // months between Jan. of start year and Jan. of end year
        int dMons = (endYear - startYear) * 12;
        // correct to actual months.
        dMons += endMOY - startMOY;
        // if the start day of month is after end day of month we have one too
        // months.
        if (start.get(Calendar.DAY_OF_MONTH) > end.get(Calendar.DAY_OF_MONTH)) {
            --dMons;
        }
        return dMons;
    }

    public static int getAgeInYears(Date startDate, Date endDate) {
        Calendar start = new GregorianCalendar();
        start.setTime(startDate);
        Calendar end = new GregorianCalendar();
        end.setTime(endDate);
        int year = end.get(Calendar.YEAR) - start.get(Calendar.YEAR);
        if (start.get(Calendar.MONTH) > end.get(Calendar.MONTH) || (start.get(Calendar.MONTH) == end.get(Calendar.MONTH)
                && start.get(Calendar.DAY_OF_MONTH) > end.get(Calendar.DAY_OF_MONTH))) {
            --year;
        }
        return year;
    }

    public static Timestamp getTimestampAtMidnightForDaysAgo(int days) {
        Calendar now = new GregorianCalendar();
        now.add(Calendar.DAY_OF_YEAR, -days);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        return new Timestamp(now.getTimeInMillis());
    }

    public static Timestamp getTimestampForBeginingOfYear() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.MONTH, 0);
        now.set(Calendar.DAY_OF_MONTH, 1);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        return new Timestamp(now.getTimeInMillis());
    }

    public static Timestamp getTimestampForBeginningOfMonth() {
        Calendar now = new GregorianCalendar();
        now.set(Calendar.DAY_OF_MONTH, 1);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        return new Timestamp(now.getTimeInMillis());
    }

    public static int getMonthForTimestamp(Timestamp date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        return calendar.get(Calendar.MONTH);
    }

    public static int getCurrentMonth() {
        return new GregorianCalendar().get(Calendar.MONTH);
    }

    public static Timestamp getTimestampForBeginningOfMonthAgo(int months) {
        Calendar now = new GregorianCalendar();
        now.add(Calendar.MONTH, -months);
        now.set(Calendar.DAY_OF_MONTH, 1);
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        return new Timestamp(now.getTimeInMillis());
    }

    public static int getCurrentYear() {
        return new GregorianCalendar().get(Calendar.YEAR);
    }

    public static int getCurrentHour() {
        return new GregorianCalendar().get(Calendar.HOUR_OF_DAY);
    }

    public static int getCurrentMinute() {
        return new GregorianCalendar().get(Calendar.MINUTE);
    }

    public static Timestamp getNowAsTimestamp() {
        return new Timestamp(new Date().getTime());
    }

    public static String convertTimestampToStringDateAndConfiguredHourTime(Timestamp timestamp) {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.CLOCK_24, "true")) {
            return convertTimestampToStringDateAndTime(timestamp);
        } else {
            return convertTimestampToStringDateAnd12HourTime(timestamp);
        }
    }

    public static String convertTimestampToStringDateAndTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new SimpleDateFormat(getDateTimeFormat()).format(timestamp);
    }

    public static String convertTimestampToStringDateAnd12HourTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new SimpleDateFormat(getDateTime12HourFormat()).format(timestamp);
    }

    public static String convertTimestampToStringConfiguredHourTime(Timestamp timestamp) {
        if (ConfigurationProperties.getInstance().isPropertyValueEqual(Property.CLOCK_24, "true")) {
            return convertTimestampToStringHourTime(timestamp);
        } else {
            return convertTimestampToString12HourTime(timestamp);
        }
    }

    public static String convertTimestampToString12HourTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new SimpleDateFormat("KK:mm a").format(timestamp);
    }

    public static String convertTimestampToStringHourTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new SimpleDateFormat("HH:mm").format(timestamp);
    }

    public static java.sql.Date convertTimestampToSqlDate(Timestamp timestamp) {
        return new java.sql.Date(timestamp.getTime());
    }

    public static Timestamp convertSqlDateToTimestamp(java.sql.Date date) {
        return new Timestamp(date.getTime());
    }

    public static String nowTimeAsText() {
        return convertTimestampToStringTime(getNowAsTimestamp());
    }

    public static Timestamp convertStringDateStringTimeToTimestamp(String date, String time) {
        if (!GenericValidator.isBlankOrNull(date) && !GenericValidator.isBlankOrNull(time)) {
            date = date + " " + time;
        } else if (!GenericValidator.isBlankOrNull(date) && GenericValidator.isBlankOrNull(time)) {
            date = date + " 09:00";
        } else {
            return null;
        }
        return convertStringDateToTimestamp(date);
    }

    /**
     * The purpose of this is to not overwrite an old value with a less specified
     * new value If the new time is empty but the dates are the same then return the
     * timestamp of the old date/time If the dates differ use the new date/time
     *
     * @param oldDate
     * @param oldTime
     * @param newDate
     * @param newTime
     * @return
     */
    public static Timestamp convertStringDatePreserveStringTimeToTimestamp(String oldDate, String oldTime,
            String newDate, String newTime) {
        if (!GenericValidator.isBlankOrNull(newTime)) {
            return convertStringDateStringTimeToTimestamp(newDate, newTime);
        }

        if (newDate != null && newDate.equals(oldDate)) {
            return convertStringDateStringTimeToTimestamp(oldDate, oldTime);
        }

        return convertStringDateStringTimeToTimestamp(newDate, newTime);
    }

    public static java.sql.Date addDaysToSQLDate(java.sql.Date date, int days) {
        return new java.sql.Date(date.getTime() + (days * DAY_IN_MILLSEC));
    }

    public static String getDateUserPrompt() {
        Locale locale = getDateFormatLocale();
        String yearRepresentation = MessageUtil.getMessage("date.format.display.year");
        String dayRepresentation = MessageUtil.getMessage("date.format.display.day");
        return MessageUtil.getMessage("sample.date.format", new String[] { dayRepresentation, yearRepresentation },
                locale);
    }

    public static String getDateFormat() {
        Locale locale = getDateFormatLocale();
        return MessageUtil.getMessage("date.format.formatKey", locale);
    }

    public static String getTimeFormat() {
        Locale locale = getDateFormatLocale();
        return MessageUtil.getMessage("time.format.formatKey", locale);
    }

    public static String getDateTimeFormat() {
        Locale locale = getDateFormatLocale();
        return MessageUtil.getMessage("timestamp.format.formatKey", locale);
    }

    public static String getDateTime12HourFormat() {
        Locale locale = getDateFormatLocale();
        return MessageUtil.getMessage("timestamp.format.formatKey.12", locale);
    }

    public static Locale getDateFormatLocale() {
        return Locale
                .forLanguageTag(ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE));
    }

    public static String getTimeUserPrompt() {
        return "(hh:mm)";
    }

    public static String getMonthFromInt(int month, boolean zeroIndexed) {
        if (zeroIndexed) {
            return new DateFormatSymbols().getMonths()[month];
        } else {
            return new DateFormatSymbols().getMonths()[month - 1];
        }
    }

    public static Date getFistDayOfTheYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_YEAR, 1);
        return cal.getTime();
    }

    public static Date getLastDayOfTheYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, 11); // 11 = december
        cal.set(Calendar.DAY_OF_MONTH, 31); // new years eve
        return cal.getTime();
    }

    public static java.sql.Date convertDateTimeToSqlDate(Date date) throws LIMSRuntimeException {
        java.sql.Date returnDate = null;
        if (date != null) {
            returnDate = new java.sql.Date(date.getTime());
        }
        return returnDate;
    }

    public static String formatStringDate(String dateStr, String outputFormat) {
        if (dateStr == null) {
            return "";
        }
        // Define the input date formats
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        LocalDate date = null;

        // Attempt to parse with the first format
        try {
            date = LocalDate.parse(dateStr, formatter1);
        } catch (DateTimeParseException e) {
            // Attempt to parse with the second format
            try {
                date = LocalDate.parse(dateStr, formatter2);
            } catch (DateTimeParseException ex) {
                // Handle invalid date format
                return "Invalid date format: " + dateStr;
            }
        }

        // Define the output date format
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);

        // Format the parsed date to the desired output format
        return date.format(outputFormatter);
    }
}
