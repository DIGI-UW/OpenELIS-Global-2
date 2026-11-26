package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import org.junit.Test;
import org.openelisglobal.qc.service.QcItemRules.Evaluation;

/**
 * OGC-1200 FR-C10, FR-C11, FR-C13: one set of blank and duplicate rules for
 * field items (S-08) and lab items recorded on a run.
 */
public class QcItemRulesTest {

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    @Test
    public void aBlankPassesAtOrBelowTheCeilingAndFailsAbove() {
        assertEquals(Evaluation.PASS, QcItemRules.blank(n("0.5"), n("0.5")).evaluation());
        assertEquals(Evaluation.FAIL, QcItemRules.blank(n("0.6"), n("0.5")).evaluation());
        assertEquals("Result = 0.6 (threshold 0.5)", QcItemRules.blank(n("0.6"), n("0.5")).detail());
        assertEquals(Evaluation.NOT_EVALUATED, QcItemRules.blank(n("0.6"), null).evaluation());
        assertEquals("a stored ceiling reads as written", "Result = 0.9 (threshold 0.5)",
                QcItemRules.blank(n("0.90000"), n("0.50000")).detail());
    }

    @Test
    public void aQualitativeBlankPassesWhenNegativeOrNotDetected() {
        assertEquals(Evaluation.PASS, QcItemRules.qualitativeBlank("Not detected").evaluation());
        assertEquals(Evaluation.PASS, QcItemRules.qualitativeBlank("negative").evaluation());
        assertEquals(Evaluation.FAIL, QcItemRules.qualitativeBlank("Positive").evaluation());
    }

    @Test
    public void aDuplicatePassesWithinTheRpdLimitAndFailsBeyondIt() {
        assertEquals(Evaluation.PASS, QcItemRules.duplicate(n("100"), n("110"), n("10"), null).evaluation());
        assertEquals(Evaluation.FAIL, QcItemRules.duplicate(n("100"), n("130"), n("20"), null).evaluation());
        assertEquals("RPD = 26.1% (threshold 20%)", QcItemRules.duplicate(n("100"), n("130"), n("20"), null).detail());
    }

    @Test
    public void aDuplicatePairBelowTheLoqIsNeitherPassedNorFailedAndNoLimitIsNotEvaluated() {
        assertEquals(Evaluation.BELOW_LOQ, QcItemRules.duplicate(n("0.2"), n("0.9"), n("20"), n("1")).evaluation());
        assertEquals("one value at the LOQ is evaluated", Evaluation.FAIL,
                QcItemRules.duplicate(n("0.2"), n("1"), n("20"), n("1")).evaluation());
        assertEquals(Evaluation.NOT_EVALUATED, QcItemRules.duplicate(n("10"), n("12"), null, null).evaluation());
        assertEquals(Evaluation.NOT_EVALUATED, QcItemRules.duplicate(null, n("12"), n("20"), null).evaluation());
        assertEquals("Its sample has no result yet", QcItemRules.duplicate(null, n("12"), n("20"), null).detail());
        assertEquals("Its sample has no result yet", QcItemRules.qualitativeDuplicate(null, "Positive").detail());
    }

    @Test
    public void aQualitativeDuplicatePassesWhenBothOutcomesAgree() {
        assertEquals(Evaluation.PASS, QcItemRules.qualitativeDuplicate("Positive", "positive").evaluation());
        assertEquals(Evaluation.FAIL, QcItemRules.qualitativeDuplicate("Positive", "Negative").evaluation());
    }
}
