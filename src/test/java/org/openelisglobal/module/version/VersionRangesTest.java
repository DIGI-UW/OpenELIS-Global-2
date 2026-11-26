package org.openelisglobal.module.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionRangesTest {

    @Test
    public void snapshotCountsAsItsReleaseVersion() {
        assertEquals("3.4.0", VersionRanges.coreVersion("3.4.0-SNAPSHOT").get().getVersion());
        assertEquals("3.4.0", VersionRanges.coreVersion("v3.4").get().getVersion());
        assertTrue(VersionRanges.satisfies("3.4.0-SNAPSHOT", ">=3.4.0"));
        assertFalse(VersionRanges.satisfies("3.3.2", ">=3.4.0"));
    }

    @Test
    public void npmRangesAreUnderstood() {
        assertTrue(VersionRanges.satisfies("3.5.1", ">=3.4.0 <4"));
        assertFalse(VersionRanges.satisfies("4.0.0", ">=3.4.0 <4"));
        assertTrue(VersionRanges.satisfies("1.3.0", "^1.0"));
        assertFalse(VersionRanges.satisfies("2.0.0", "^1.0"));
        assertTrue(VersionRanges.satisfies("1.2.9", "~1.2.0"));
        assertFalse(VersionRanges.satisfies("1.3.0", "~1.2.0"));
        assertTrue(VersionRanges.satisfies("9.0.0", "*"));
    }

    @Test
    public void lowerBoundIsRequiredForTheOpenElisRange() {
        assertTrue(VersionRanges.hasLowerBound(">=3.4.0"));
        assertTrue(VersionRanges.hasLowerBound("^3.4"));
        assertTrue(VersionRanges.hasLowerBound("3.4.0"));
        assertTrue(VersionRanges.hasLowerBound(">=3.4.0 <4"));
        assertFalse(VersionRanges.hasLowerBound("<4"));
        assertFalse(VersionRanges.hasLowerBound("*"));
        assertFalse(VersionRanges.hasLowerBound("x.x.x"));
        assertFalse(VersionRanges.hasLowerBound("*.*"));
        assertFalse(VersionRanges.hasLowerBound("X"));
        assertFalse(VersionRanges.hasLowerBound(""));
        assertFalse(VersionRanges.hasLowerBound("not a range"));
    }

    @Test
    public void invalidInputIsReported() {
        assertFalse(VersionRanges.isValidRange("abc"));
        assertFalse(VersionRanges.isValidRange(null));
        assertTrue(VersionRanges.coreVersion("latest").isEmpty());
        assertTrue(VersionRanges.isLowerThan("1.0.0", "1.0.1"));
        assertFalse(VersionRanges.isLowerThan("1.0.1-SNAPSHOT", "1.0.1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void satisfiesRefusesANonVersion() {
        VersionRanges.satisfies("latest", ">=1");
    }
}
