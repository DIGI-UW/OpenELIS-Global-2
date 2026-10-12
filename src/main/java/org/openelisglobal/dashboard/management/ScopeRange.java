package org.openelisglobal.dashboard.management;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Inclusive lab-calendar dates; the prior period has the same length and ends
// the day before from, as on the QI Dashboard.
public record ScopeRange(@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate from,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate to,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate priorFrom,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate priorTo) {

    public static ScopeRange of(String scope, LocalDate today) {
        LocalDate from = switch (scope) {
        case "7d" -> today.minusDays(7);
        case "30d" -> today.minusDays(30);
        case "90d" -> today.minusDays(90);
        case "quarter" -> today.withDayOfMonth(1).withMonth((today.getMonthValue() - 1) / 3 * 3 + 1);
        case "ytd" -> today.withDayOfYear(1);
        case "year" -> today.minusYears(1);
        default -> throw new IllegalArgumentException("Unknown scope: " + scope);
        };
        LocalDate priorTo = from.minusDays(1);
        return new ScopeRange(from, today, priorTo.minusDays(ChronoUnit.DAYS.between(from, today)), priorTo);
    }
}
