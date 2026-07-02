package org.openelisglobal.testcatalog.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.springframework.stereotype.Service;

/**
 * OGC-949 M7 / OGC-973 — reference-range coverage validation (the activation
 * safety gate, H-03).
 *
 * Given a test's reference ranges (each with a gender + an age window in
 * fractional years), determines, per sex, whether the age axis is fully covered
 * from birth (age 0) — or whether there are GAPS (uncovered age windows, e.g. a
 * neonatal bilirubin range for day 0–1 and day 3–30 leaves days 1–3 uncovered)
 * or OVERLAPS (two ranges claiming the same age window).
 *
 * Pure logic, no persistence — so it is unit-tested directly with synthetic
 * age-window fixtures. A gender-less ("all"/"both") range counts toward BOTH
 * male and female coverage (spec fix M-08).
 */
@Service
public class RangeCoverageValidationService {

    /** Coverage outcome for one sex. GAP outranks OVERLAP for the gate. */
    public enum Status {
        COMPLETE, GAP, OVERLAP, EMPTY
    }

    /** A half-open age window [fromAge, toAge) in fractional years. */
    public static class AgeInterval {
        public double fromAge;
        public double toAge;

        public AgeInterval() {
        }

        public AgeInterval(double fromAge, double toAge) {
            this.fromAge = fromAge;
            this.toAge = toAge;
        }
    }

    public static class SexCoverage {
        public String sex;
        public Status status;
        public List<AgeInterval> gaps = new ArrayList<>();
        public List<AgeInterval> overlaps = new ArrayList<>();
    }

    public static class CoverageReport {
        public SexCoverage male;
        public SexCoverage female;

        /** True if either sex has an uncovered age window — the activation gate. */
        public boolean hasGaps() {
            return (male != null && !male.gaps.isEmpty()) || (female != null && !female.gaps.isEmpty());
        }
    }

    // Tolerance for floating-point age comparisons (well under one hour in years).
    private static final double EPSILON = 1e-9;

    public CoverageReport validate(List<ResultLimit> limits) {
        CoverageReport report = new CoverageReport();
        report.male = coverageForSex(limits, "M");
        report.female = coverageForSex(limits, "F");
        return report;
    }

    private SexCoverage coverageForSex(List<ResultLimit> limits, String sex) {
        SexCoverage coverage = new SexCoverage();
        coverage.sex = sex;

        List<ResultLimit> applicable = new ArrayList<>();
        for (ResultLimit limit : limits) {
            if (appliesToSex(limit.getGender(), sex)) {
                applicable.add(limit);
            }
        }
        if (applicable.isEmpty()) {
            coverage.status = Status.EMPTY;
            return coverage;
        }

        applicable.sort(Comparator.comparingDouble(ResultLimit::getMinAge));
        // Walk the sorted windows tracking the frontier covered from age 0.
        double coveredTo = 0d;
        for (ResultLimit limit : applicable) {
            double min = limit.getMinAge();
            double max = limit.getMaxAge();
            if (min > coveredTo + EPSILON) {
                coverage.gaps.add(new AgeInterval(coveredTo, min));
            } else if (min < coveredTo - EPSILON) {
                coverage.overlaps.add(new AgeInterval(min, Math.min(coveredTo, max)));
            }
            coveredTo = Math.max(coveredTo, max);
        }

        // Tail gap (FR-20/21): only an open-ended top band (max = +Infinity) covers
        // to the top of the reportable lifetime. If the highest covered age is
        // finite, everything above it is uncovered — e.g. bands 0–15 + 15–30 leave
        // 30+ uncovered — which the frontier walk above would otherwise miss.
        if (Double.isFinite(coveredTo)) {
            coverage.gaps.add(new AgeInterval(coveredTo, Double.POSITIVE_INFINITY));
        }

        // Gaps drive the safety gate, so they outrank overlaps for the status.
        if (!coverage.gaps.isEmpty()) {
            coverage.status = Status.GAP;
        } else if (!coverage.overlaps.isEmpty()) {
            coverage.status = Status.OVERLAP;
        } else {
            coverage.status = Status.COMPLETE;
        }
        return coverage;
    }

    /**
     * A blank / "A" / "ALL" / "B" gender applies to every sex (M-08); otherwise the
     * range applies only to the matching sex.
     */
    private boolean appliesToSex(String gender, String sex) {
        if (gender == null || gender.isBlank()) {
            return true;
        }
        String g = gender.trim().toUpperCase();
        if (g.equals("A") || g.equals("ALL") || g.equals("B") || g.equals("BOTH")) {
            return true;
        }
        return g.equals(sex);
    }
}
