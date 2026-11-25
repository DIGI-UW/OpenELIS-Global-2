package org.openelisglobal.common.util;

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites an ISO-8601 date ({@code 2026-09-26}, optionally with a time part)
 * as the site's display date, so API callers can send ISO dates to fields that
 * are validated and stored in the display format. Anything else, including a
 * value already in the display format, is returned unchanged for validation to
 * judge.
 */
public final class IsoDateNormalizer {

    private static final Pattern ISO_DATE = Pattern.compile(
            "^(\\d{4})-(\\d{2})-(\\d{2})(?:[T ]\\d{2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?(?:Z|[+-]\\d{2}:?\\d{2})?)?$");

    private IsoDateNormalizer() {
    }

    public static String toDisplayFormat(String value) {
        if (value == null || !ISO_DATE.matcher(value.trim()).matches()) {
            return value;
        }
        return toDisplayFormat(value, DateUtil.getDateFormat());
    }

    /**
     * The ISO date ({@code yyyy-MM-dd}) for a date in the site's display format or
     * an ISO date-time; anything that is neither, read strictly, comes back
     * unchanged. Dates kept as text are stored this way so they still read after
     * the site changes its date format (OGC-1669).
     */
    public static String toIso(String value) {
        return toIso(value, DateUtil.getDateFormat());
    }

    public static String toIso(String value, String displayPattern) {
        LocalDate date = toLocalDate(value, displayPattern);
        return date == null ? value : date.toString();
    }

    /**
     * The date a stored value names: an ISO date, or a date in the site's display
     * format read strictly, so a day and a month are never swapped. A day or month
     * written with one digit ({@code 1/5/2026}) reads too, as the date validator
     * accepts it. Null when it is neither.
     */
    public static LocalDate toLocalDate(String value) {
        return toLocalDate(value, DateUtil.getDateFormat());
    }

    public static LocalDate toLocalDate(String value, String displayPattern) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Matcher iso = ISO_DATE.matcher(value.trim());
        try {
            if (iso.matches()) {
                return LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)),
                        Integer.parseInt(iso.group(3)));
            }
            return LocalDate.parse(value.trim(),
                    DateTimeFormatter
                            .ofPattern(displayPattern.replace("yyyy", "uuuu").replace("MM", "M").replace("dd", "d"))
                            .withResolverStyle(ResolverStyle.STRICT));
        } catch (DateTimeException e) {
            return null;
        }
    }

    public static String toDisplayFormat(String value, String displayPattern) {
        if (value == null) {
            return null;
        }
        Matcher matcher = ISO_DATE.matcher(value.trim());
        if (!matcher.matches()) {
            return value;
        }
        try {
            LocalDate date = LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
            return new SimpleDateFormat(displayPattern).format(java.sql.Date.valueOf(date));
        } catch (DateTimeException e) {
            return value;
        }
    }
}
