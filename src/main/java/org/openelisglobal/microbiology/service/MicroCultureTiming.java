package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;

/** All incubation deadlines share the original inoculation clock. */
public final class MicroCultureTiming {
    private MicroCultureTiming() {
    }

    public static long milliseconds(BigDecimal value, String unit) {
        if (value == null || value.stripTrailingZeros().scale() > 2 || value.signum() <= 0
                || value.compareTo(new BigDecimal("87600")) > 0)
            throw new IllegalArgumentException("MICROBIOLOGY_CULTURE_INVALID_DURATION");
        long factor = switch (unit == null ? "" : unit) {
        case "HOURS" -> 3600000L;
        case "DAYS" -> 86400000L;
        case "WEEKS" -> 604800000L;
        default -> throw new IllegalArgumentException("MICROBIOLOGY_CULTURE_INVALID_UNIT");
        };
        return value.multiply(BigDecimal.valueOf(factor)).longValueExact();
    }

    public static void validateTemperature(BigDecimal value) {
        if (value != null && (value.stripTrailingZeros().scale() > 2 || value.compareTo(new BigDecimal("-100")) < 0
                || value.compareTo(new BigDecimal("100")) > 0))
            throw new IllegalArgumentException("MICROBIOLOGY_CULTURE_TEMPERATURE");
    }

    public static void validateLoopVolume(BigDecimal value) {
        if (value != null && (value.signum() <= 0 || value.stripTrailingZeros().scale() > 2
                || value.compareTo(new BigDecimal("99999999.99")) > 0))
            throw new IllegalArgumentException("MICROBIOLOGY_CULTURE_LOOP_VOLUME");
    }

    public static Timestamp ends(Timestamp start, BigDecimal duration, String unit, long extensionMillis) {
        return new Timestamp(
                Math.addExact(start.getTime(), Math.addExact(milliseconds(duration, unit), extensionMillis)));
    }

    public static Timestamp nextCheck(Timestamp start, Timestamp lastReading, BigDecimal intervalHours) {
        if (intervalHours == null)
            return null;
        long interval = milliseconds(intervalHours, "HOURS");
        long elapsed = lastReading == null ? 0 : Math.max(0, lastReading.getTime() - start.getTime());
        return new Timestamp(start.getTime() + (elapsed / interval + 1) * interval);
    }

    public static BigDecimal hours(Timestamp start, Timestamp finish) {
        return BigDecimal.valueOf(finish.getTime() - start.getTime()).divide(BigDecimal.valueOf(3600000), 2,
                RoundingMode.HALF_UP);
    }

    public static boolean due(Timestamp deadline, Instant now) {
        return deadline != null && !deadline.toInstant().isAfter(now);
    }
}
