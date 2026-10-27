package org.openelisglobal.dashboard.management;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Inclusive lab-calendar dates. The prior period is the same number of days before
// from, or for quarter and year-to-date the same span of the previous quarter or year.
public record ScopeRange(LocalDate from, LocalDate to, LocalDate priorFrom, LocalDate priorTo) {

    public static ScopeRange of(String scope, LocalDate today) {
        LocalDate from = switch (scope) {
        case "7d" -> today.minusDays(6);
        case "30d" -> today.minusDays(29);
        case "90d" -> today.minusDays(89);
        case "quarter" -> today.withDayOfMonth(1).withMonth((today.getMonthValue() - 1) / 3 * 3 + 1);
        case "ytd" -> today.withDayOfYear(1);
        case "year" -> today.minusYears(1).plusDays(1);
        default -> throw new IllegalArgumentException("Unknown scope: " + scope);
        };
        if (scope.equals("quarter") || scope.equals("ytd")) {
            return sameSpanEarlier(from, today, scope.equals("quarter") ? 3 : 12);
        }
        LocalDate priorTo = from.minusDays(1);
        return new ScopeRange(from, today, priorTo.minusDays(ChronoUnit.DAYS.between(from, today)), priorTo);
    }

    // A month-end today compares with the whole earlier month.
    private static ScopeRange sameSpanEarlier(LocalDate from, LocalDate today, int months) {
        LocalDate priorTo = today.minusMonths(months);
        if (today.getDayOfMonth() == today.lengthOfMonth()) {
            priorTo = priorTo.withDayOfMonth(priorTo.lengthOfMonth());
        }
        return new ScopeRange(from, today, from.minusMonths(months), priorTo);
    }
}
