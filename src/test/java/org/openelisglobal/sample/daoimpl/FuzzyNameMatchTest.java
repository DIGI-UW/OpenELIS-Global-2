package org.openelisglobal.sample.daoimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Every name search generated all adjacent swaps of the term with no length
 * limit, so a few very long names cost quadratic work (SCAN-101).
 */
public class FuzzyNameMatchTest {

    @Test
    public void aTermUpToTheLimit_isFuzzyMatched() {
        assertTrue(FuzzyNameMatch.isFuzzyMatchable("a".repeat(FuzzyNameMatch.MAX_FUZZY_LENGTH - 1) + "b"));
        assertEquals(1, FuzzyNameMatch.adjacentSwaps("ab").size());
    }

    @Test
    public void aTermBeyondTheLimit_isMatchedBySubstringOnly() {
        assertFalse(FuzzyNameMatch.isFuzzyMatchable("ab".repeat(FuzzyNameMatch.MAX_FUZZY_LENGTH)));
    }
}
