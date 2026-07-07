package org.openelisglobal.testcatalog.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService.CoverageReport;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService.Status;

/**
 * Pure-logic unit tests for the activation safety gate (no Spring, no DB). Ages
 * are fractional years: 1 day ≈ 0.00274, 3 days ≈ 0.00822, 30 days ≈ 0.0822.
 *
 * Every case asserts a concrete outcome, so a regression in the gap/overlap
 * walk fails the test — this is the contract for the H-03 activation gate.
 */
public class RangeCoverageValidationServiceTest {

    private final RangeCoverageValidationService service = new RangeCoverageValidationService();

    private static ResultLimit limit(String gender, double minAge, double maxAge) {
        ResultLimit l = new ResultLimit();
        l.setGender(gender);
        l.setMinAge(minAge);
        l.setMaxAge(maxAge);
        return l;
    }

    @Test
    public void contiguousRangesFromZero_areComplete() {
        CoverageReport r = service.validate(
                Arrays.asList(limit("M", 0d, 1d), limit("M", 1d, 18d), limit("M", 18d, Double.POSITIVE_INFINITY)));
        assertEquals(Status.COMPLETE, r.male.status);
        assertTrue(r.male.gaps.isEmpty());
        assertFalse(r.hasGaps());
    }

    @Test
    public void rangesNotStartingAtZero_haveALeadingGap() {
        // Open-ended top so the ONLY gap under test is the leading [0,1).
        CoverageReport r = service.validate(Arrays.asList(limit("M", 1d, Double.POSITIVE_INFINITY)));
        assertEquals(Status.GAP, r.male.status);
        assertEquals(1, r.male.gaps.size());
        assertEquals(0d, r.male.gaps.get(0).fromAge, 1e-9);
        assertEquals(1d, r.male.gaps.get(0).toAge, 1e-9);
        assertTrue(r.hasGaps());
    }

    @Test
    public void neonatalWindowGap_isDetected() {
        // Day 0–1 covered, day 3–∞ covered, days 1–3 left UNCOVERED — the
        // bilirubin safety gap a 2-day-old's result would fall into.
        CoverageReport r = service
                .validate(Arrays.asList(limit("M", 0d, 1d / 365d), limit("M", 3d / 365d, Double.POSITIVE_INFINITY)));
        assertEquals(Status.GAP, r.male.status);
        assertEquals(1, r.male.gaps.size());
        assertEquals(1d / 365d, r.male.gaps.get(0).fromAge, 1e-9);
        assertEquals(3d / 365d, r.male.gaps.get(0).toAge, 1e-9);
    }

    @Test
    public void openEndedTopMissing_reportsTailGap() {
        // FR-20: bands 0–15 + 15–30 cover from birth but leave 30+ UNCOVERED —
        // without an open-ended top band the tail is a gap (this must NOT read as
        // "fully covered").
        CoverageReport r = service.validate(Arrays.asList(limit("M", 0d, 15d), limit("M", 15d, 30d)));
        assertEquals(Status.GAP, r.male.status);
        assertEquals(1, r.male.gaps.size());
        assertEquals(30d, r.male.gaps.get(0).fromAge, 1e-9);
        assertTrue(r.hasGaps());
    }

    @Test
    public void overlappingRanges_areDetected() {
        // Open-ended top so the only finding is the [3,5) overlap, not a tail gap.
        CoverageReport r = service
                .validate(Arrays.asList(limit("M", 0d, 5d), limit("M", 3d, Double.POSITIVE_INFINITY)));
        assertEquals(Status.OVERLAP, r.male.status);
        assertEquals(1, r.male.overlaps.size());
        assertTrue(r.male.gaps.isEmpty());
        assertFalse(r.hasGaps()); // an overlap is not a gap — it doesn't block activation
    }

    @Test
    public void genderlessRange_coversBothSexes() {
        CoverageReport r = service.validate(Arrays.asList(limit(null, 0d, Double.POSITIVE_INFINITY)));
        assertEquals(Status.COMPLETE, r.male.status);
        assertEquals(Status.COMPLETE, r.female.status);
    }

    @Test
    public void rangesForOneSexLeaveTheOtherEmpty() {
        CoverageReport r = service.validate(Arrays.asList(limit("M", 0d, Double.POSITIVE_INFINITY)));
        assertEquals(Status.COMPLETE, r.male.status);
        assertEquals(Status.EMPTY, r.female.status);
    }

    @Test
    public void allSexRange_fillsAGapForASpecificSex() {
        // A male-specific 0–1 plus an all-sex 1–∞ → male is fully covered (the
        // all-sex range counts toward male coverage, M-08).
        CoverageReport r = service
                .validate(Arrays.asList(limit("M", 0d, 1d), limit(null, 1d, Double.POSITIVE_INFINITY)));
        assertEquals(Status.COMPLETE, r.male.status);
        assertTrue(r.male.gaps.isEmpty());
    }
}
