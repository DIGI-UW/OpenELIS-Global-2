package org.openelisglobal.common.fhir.dao;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Turns a FHIR date parameter into Criteria bounds over a timestamp column.
 *
 * <p>
 * OpenELIS stores dates as local timestamps, so a day-precision search value
 * spans that calendar day in the server's zone rather than in GMT. Prefixes
 * keep FHIR semantics: {@code gt} starts after the period, {@code lt} ends
 * before it, and {@code eq}/{@code ge}/{@code le} include it. The same rules
 * back {@code _lastUpdated} on every resource and the in-memory Location search
 * through {@link #contains(DateRangeParam, Date)}.
 */
public final class DateParamBounds {

    private DateParamBounds() {
    }

    public static List<Predicate> predicates(CriteriaBuilder criteriaBuilder, Expression<Date> column,
            DateRangeParam range) {

        List<Predicate> predicates = new ArrayList<>();
        if (range == null) {
            return predicates;
        }

        DateParam lower = range.getLowerBound();
        if (lower != null && lower.getValue() != null) {
            Date[] period = period(lower);
            boolean exclusive = lower.getPrefix() == ParamPrefixEnum.GREATERTHAN;
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(column, timestamp(exclusive ? period[1] : period[0])));
        }

        DateParam upper = range.getUpperBound();
        if (upper != null && upper.getValue() != null) {
            Date[] period = period(upper);
            boolean exclusive = upper.getPrefix() == ParamPrefixEnum.LESSTHAN;
            predicates.add(criteriaBuilder.lessThan(column, timestamp(exclusive ? period[0] : period[1])));
        }
        return predicates;
    }

    /**
     * Whether {@code value} falls inside the range, with the same period rules as
     * the predicates.
     */
    public static boolean contains(DateRangeParam range, Date value) {
        if (range == null) {
            return true;
        }
        if (value == null) {
            return range.getLowerBound() == null && range.getUpperBound() == null;
        }
        DateParam lower = range.getLowerBound();
        if (lower != null && lower.getValue() != null) {
            Date[] period = period(lower);
            Date from = lower.getPrefix() == ParamPrefixEnum.GREATERTHAN ? period[1] : period[0];
            if (value.before(from)) {
                return false;
            }
        }
        DateParam upper = range.getUpperBound();
        if (upper != null && upper.getValue() != null) {
            Date[] period = period(upper);
            Date to = upper.getPrefix() == ParamPrefixEnum.LESSTHAN ? period[0] : period[1];
            if (!value.before(to)) {
                return false;
            }
        }
        return true;
    }

    private static java.sql.Timestamp timestamp(Date date) {
        return new java.sql.Timestamp(date.getTime());
    }

    /** [start inclusive, end exclusive] of the period the parameter names. */
    private static Date[] period(DateParam parameter) {

        Date value = parameter.getValue();
        TemporalPrecisionEnum precision = parameter.getPrecision();
        if (precision == null || precision.ordinal() > TemporalPrecisionEnum.DAY.ordinal()) {
            return new Date[] { value, new Date(value.getTime() + 1) };
        }

        Calendar start = Calendar.getInstance();
        start.setTime(value);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        if (precision == TemporalPrecisionEnum.MONTH || precision == TemporalPrecisionEnum.YEAR) {
            start.set(Calendar.DAY_OF_MONTH, 1);
        }
        if (precision == TemporalPrecisionEnum.YEAR) {
            start.set(Calendar.MONTH, Calendar.JANUARY);
        }

        Calendar end = (Calendar) start.clone();
        if (precision == TemporalPrecisionEnum.YEAR) {
            end.add(Calendar.YEAR, 1);
        } else if (precision == TemporalPrecisionEnum.MONTH) {
            end.add(Calendar.MONTH, 1);
        } else {
            end.add(Calendar.DAY_OF_MONTH, 1);
        }
        return new Date[] { start.getTime(), end.getTime() };
    }
}
