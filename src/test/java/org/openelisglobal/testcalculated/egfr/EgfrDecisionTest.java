package org.openelisglobal.testcalculated.egfr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Test;
import org.openelisglobal.testcalculated.egfr.CkdEpi2021.Outcome;
import org.openelisglobal.testcalculated.egfr.CkdEpi2021.SkipReason;
import org.openelisglobal.testcalculated.egfr.EgfrDecision.Action;
import org.openelisglobal.testcalculated.egfr.EgfrDecision.Cause;

/**
 * What happens to an ordered eGFR when its creatinine is saved: calculated
 * values follow the creatinine, values entered by hand or validated are never
 * touched, and a skipped calculation says why.
 */
public class EgfrDecisionTest {

    private static final LocalDate COLLECTED = LocalDate.of(2026, 10, 15);

    /** Female, 27 at collection: 0.62 mg/dL gives 125. */
    private static Outcome female27(String creatinine) {
        return CkdEpi2021.calculate(creatinine, "mg/dL", "F", COLLECTED.minusYears(27), COLLECTED);
    }

    private static EgfrDecision decide(Outcome outcome, String currentEgfr, boolean calculatedEgfr, boolean finalized) {
        return EgfrDecision.decide(outcome, "0.62", "mg/dL", currentEgfr, calculatedEgfr, finalized);
    }

    @Test
    public void fillsAnEmptyEgfr() {
        EgfrDecision decision = decide(female27("0.62"), null, false, false);
        assertEquals(Action.WRITE, decision.getAction());
        assertEquals("125", decision.getValue());
        assertEquals("Calculated with CKD-EPI 2021 (race-free) from creatinine 0.62 mg/dL, female, 27 years at"
                + " collection: 125 mL/min/1.73 m2.", decision.getNote());

        assertEquals("a blank value counts as empty", Action.WRITE,
                decide(female27("0.62"), "  ", false, false).getAction());
    }

    @Test
    public void recalculatesItsOwnValueWhenTheCreatinineIsCorrected() {
        EgfrDecision decision = decide(female27("1.20"), "125", true, false);
        assertEquals(Action.WRITE, decision.getAction());
        assertEquals(String.valueOf(female27("1.20").getEgfr()), decision.getValue());
    }

    @Test
    public void leavesItsOwnValueAloneWhenNothingChanged() {
        EgfrDecision decision = decide(female27("0.62"), "125", true, false);
        assertEquals(Action.NONE, decision.getAction());
        assertNull(decision.getNote());
    }

    @Test
    public void neverOverwritesAnEgfrEnteredByHand() {
        EgfrDecision differs = decide(female27("0.62"), "118", false, false);
        assertEquals(Action.NONE, differs.getAction());
        assertNull(differs.getValue());
        assertTrue(differs.getNote(),
                differs.getNote().startsWith("eGFR 118 was entered by hand and was not replaced."));
        assertTrue(differs.getNote(), differs.getNote().contains(": 125 mL/min/1.73 m2."));

        EgfrDecision agrees = decide(female27("0.62"), "125", false, false);
        assertEquals(Action.NONE, agrees.getAction());
        assertNull("nothing to point out when the hand-entered value agrees", agrees.getNote());
    }

    @Test
    public void neverClearsAnEgfrEnteredByHand() {
        Outcome underAge = CkdEpi2021.calculate("0.62", "mg/dL", "F", COLLECTED.minusYears(16), COLLECTED);
        EgfrDecision decision = decide(underAge, "140", false, false);
        assertEquals(Action.NONE, decision.getAction());
        assertNull(decision.getNote());
    }

    @Test
    public void neverChangesAValidatedEgfr() {
        EgfrDecision ownValue = decide(female27("1.20"), "125", true, true);
        assertEquals(Action.NONE, ownValue.getAction());
        assertTrue(ownValue.getNote(), ownValue.getNote().startsWith("eGFR is already validated"));

        assertEquals(Action.NONE, decide(female27("1.20"), "125", false, true).getAction());
        assertEquals(Action.NONE,
                decide(Outcome.skipped(SkipReason.CREATININE_NOT_NUMERIC), "125", true, true).getAction());
    }

    @Test
    public void clearsItsOwnValueWhenItCanNoLongerBeCalculated() {
        EgfrDecision decision = decide(female27("<0.20"), "125", true, false);
        assertEquals(Action.CLEAR, decision.getAction());
        assertEquals("Calculated eGFR 125 removed: the creatinine result is not a plain number.", decision.getNote());

        assertEquals("creatinine cleared", Action.CLEAR, decide(female27(""), "125", true, false).getAction());
    }

    @Test
    public void notesWhyAnEgfrWasNotCalculated() {
        Outcome underAge = CkdEpi2021.calculate("0.62", "mg/dL", "F", COLLECTED.minusYears(16), COLLECTED);
        EgfrDecision decision = decide(underAge, null, false, false);
        assertEquals(Action.NONE, decision.getAction());
        assertEquals("eGFR not calculated (CKD-EPI 2021): the patient was under 18 years old at collection.",
                decision.getNote());

        assertEquals("eGFR not calculated (CKD-EPI 2021): the patient's sex is not recorded as male or female.",
                decide(Outcome.skipped(SkipReason.MISSING_SEX), null, false, false).getNote());
        assertEquals("eGFR not calculated (CKD-EPI 2021): the patient's birth date is missing.",
                decide(Outcome.skipped(SkipReason.MISSING_BIRTH_DATE), null, false, false).getNote());
    }

    @Test
    public void saysNothingWhileTheCreatinineIsEmpty() {
        EgfrDecision decision = decide(Outcome.skipped(SkipReason.NO_CREATININE_VALUE), null, false, false);
        assertEquals(Action.NONE, decision.getAction());
        assertNull(decision.getNote());
    }

    @Test
    public void showsTheConvertedCreatinineInTheNote() {
        Outcome outcome = CkdEpi2021.calculate("54.8", "µmol/L", "F", COLLECTED.minusYears(27), COLLECTED);
        EgfrDecision decision = EgfrDecision.decide(outcome, "54.8", "µmol/L", null, false, false);
        assertTrue(decision.getNote(), decision.getNote().contains("creatinine 54.8 µmol/L (0.62 mg/dL)"));
    }

    private static EgfrDecision afterPatientChange(Outcome outcome, String currentEgfr, boolean calculatedEgfr,
            boolean finalized) {
        return EgfrDecision.decide(Cause.PATIENT_CHANGED, outcome, "0.62", "mg/dL", currentEgfr, calculatedEgfr,
                finalized);
    }

    /** Male, 27 at collection, same creatinine: what a sex correction gives. */
    private static Outcome male27(String creatinine) {
        return CkdEpi2021.calculate(creatinine, "mg/dL", "M", COLLECTED.minusYears(27), COLLECTED);
    }

    @Test
    public void theFirstCalculationIsNotACorrection() {
        EgfrDecision decision = decide(female27("0.62"), null, false, false);
        assertFalse(decision.isCorrection());
        assertNull(decision.getPreviousValue());

        EgfrDecision afterSexAdded = afterPatientChange(female27("0.62"), null, false, false);
        assertEquals(Action.WRITE, afterSexAdded.getAction());
        assertFalse("filling an empty eGFR once the sex is known is a first calculation", afterSexAdded.isCorrection());
    }

    @Test
    public void aCreatinineCorrectionThatChangesTheEgfrIsACorrection() {
        String recalculated = String.valueOf(female27("1.20").getEgfr());
        EgfrDecision decision = decide(female27("1.20"), "125", true, false);
        assertTrue(decision.isCorrection());
        assertEquals("125", decision.getPreviousValue());
        assertTrue(decision.getNote(), decision.getNote().startsWith(
                "Recalculated after the creatinine was corrected: 125 \u2192 " + recalculated + " mL/min/1.73 m2."));

        assertFalse("unchanged value", decide(female27("0.62"), "125", true, false).isCorrection());
        assertTrue("cleared value", decide(female27("<0.20"), "125", true, false).isCorrection());
        assertFalse("hand-entered value", decide(female27("1.20"), "118", false, false).isCorrection());
    }

    @Test
    public void recalculatesItsOwnValueWhenThePatientSexChanges() {
        String male = String.valueOf(male27("0.62").getEgfr());
        EgfrDecision decision = afterPatientChange(male27("0.62"), "125", true, false);
        assertEquals(Action.WRITE, decision.getAction());
        assertEquals(male, decision.getValue());
        assertTrue(decision.isCorrection());
        assertEquals("125", decision.getPreviousValue());
        assertTrue(decision.getNote(), decision.getNote().startsWith(
                "Recalculated after patient sex/birth date changed: 125 \u2192 " + male + " mL/min/1.73 m2. "));
        assertTrue(decision.getNote(), decision.getNote().contains("male, 27 years at collection"));
    }

    @Test
    public void clearsItsOwnValueWhenThePatientBecomesUnder18() {
        Outcome underAge = CkdEpi2021.calculate("0.62", "mg/dL", "F", COLLECTED.minusYears(16), COLLECTED);
        EgfrDecision decision = afterPatientChange(underAge, "125", true, false);
        assertEquals(Action.CLEAR, decision.getAction());
        assertTrue(decision.isCorrection());
        assertEquals("Calculated eGFR 125 removed after patient sex/birth date changed: the patient was under 18"
                + " years old at collection.", decision.getNote());
    }

    @Test
    public void aPatientChangeNeverTouchesAnEgfrEnteredByHand() {
        EgfrDecision decision = afterPatientChange(male27("0.62"), "118", false, false);
        assertEquals(Action.NONE, decision.getAction());
        assertNull(decision.getNote());
        assertFalse(decision.isCorrection());

        EgfrDecision validated = afterPatientChange(male27("0.62"), "118", false, true);
        assertEquals(Action.NONE, validated.getAction());
        assertNull(validated.getNote());
        assertFalse(validated.isCorrection());
    }

    @Test
    public void aPatientChangeLeavesAValidatedEgfrButSaysSo() {
        String male = String.valueOf(male27("0.62").getEgfr());
        EgfrDecision decision = afterPatientChange(male27("0.62"), "125", true, true);
        assertEquals(Action.NONE, decision.getAction());
        assertTrue(decision.isCorrection());
        assertEquals("125", decision.getPreviousValue());
        assertEquals(
                "eGFR is already validated and was not recalculated after patient sex/birth date changed."
                        + " CKD-EPI 2021 (race-free) now gives " + male + " mL/min/1.73 m2 instead of 125.",
                decision.getNote());

        Outcome underAge = CkdEpi2021.calculate("0.62", "mg/dL", "F", COLLECTED.minusYears(16), COLLECTED);
        EgfrDecision noLonger = afterPatientChange(underAge, "125", true, true);
        assertEquals(Action.NONE, noLonger.getAction());
        assertTrue(noLonger.isCorrection());
        assertTrue(noLonger.getNote(), noLonger.getNote().contains("can no longer be calculated"));

        assertFalse("the validated value still agrees",
                afterPatientChange(female27("0.62"), "125", true, true).isCorrection());
    }
}
