package org.openelisglobal.common.util;

import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
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
