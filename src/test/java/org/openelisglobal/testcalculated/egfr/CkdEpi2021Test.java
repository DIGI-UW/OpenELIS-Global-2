package org.openelisglobal.testcalculated.egfr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Test;
import org.openelisglobal.testcalculated.egfr.CkdEpi2021.Outcome;
import org.openelisglobal.testcalculated.egfr.CkdEpi2021.SkipReason;

/**
 * The race-free CKD-EPI 2021 creatinine equation: Inker LA, Eneanya ND, Coresh
 * J, et al. New creatinine- and cystatin C-based equations to estimate GFR
 * without race. N Engl J Med 2021;385(19):1737-49. doi:10.1056/NEJMoa2102953.
 *
 * <p>
 * Reference values are the "age, sex and creatinine combinations to use for
 * testing" published by the National Kidney Foundation with its example IT
 * request for implementing the equation: each side of the kappa break point
 * (0.90/0.91 mg/dL male, 0.70/0.71 mg/dL female) at age 18, and both sides of
 * it at age 90.
 */
public class CkdEpi2021Test {

    private static final LocalDate COLLECTED = LocalDate.of(2026, 10, 15);

    private static Outcome calculate(String creatinine, String unit, String sex, int age) {
        return CkdEpi2021.calculate(creatinine, unit, sex, COLLECTED.minusYears(age), COLLECTED);
    }

    private static Integer egfr(String sex, int age, String creatinineMgDl) {
        return calculate(creatinineMgDl, "mg/dL", sex, age).getEgfr();
    }

    @Test
    public void matchesTheNkfReferenceValues() {
        assertEquals(Integer.valueOf(127), egfr("M", 18, "0.90"));
        assertEquals(Integer.valueOf(125), egfr("M", 18, "0.91"));
        assertEquals(Integer.valueOf(128), egfr("F", 18, "0.70"));
        assertEquals(Integer.valueOf(126), egfr("F", 18, "0.71"));
        assertEquals(Integer.valueOf(97), egfr("M", 90, "0.50"));
        assertEquals(Integer.valueOf(44), egfr("M", 90, "1.50"));
        assertEquals(Integer.valueOf(89), egfr("F", 90, "0.50"));
        assertEquals(Integer.valueOf(33), egfr("F", 90, "1.50"));
    }

    @Test
    public void matchesThePiecewiseFormOfTheEquation() {
        // The paper's equation written out per branch, as an independent check
        for (int age = 18; age <= 100; age += 7) {
            for (double scr = 0.30; scr <= 8.0; scr += 0.07) {
                double female = scr <= 0.7 ? 142 * Math.pow(scr / 0.7, -0.241) * Math.pow(0.9938, age) * 1.012
                        : 142 * Math.pow(scr / 0.7, -1.200) * Math.pow(0.9938, age) * 1.012;
                double male = scr <= 0.9 ? 142 * Math.pow(scr / 0.9, -0.302) * Math.pow(0.9938, age)
                        : 142 * Math.pow(scr / 0.9, -1.200) * Math.pow(0.9938, age);
                assertEquals(female, CkdEpi2021.estimate(scr, age, true), 1e-9);
                assertEquals(male, CkdEpi2021.estimate(scr, age, false), 1e-9);
            }
        }
    }

    @Test
    public void isContinuousAtTheKappaBreakPoint() {
        double male = 142 * Math.pow(0.9938, 50);
        assertEquals(male, CkdEpi2021.estimate(0.9, 50, false), 1e-9);
        assertEquals(male, CkdEpi2021.estimate(0.9 - 1e-9, 50, false), 1e-6);
        assertEquals(male, CkdEpi2021.estimate(0.9 + 1e-9, 50, false), 1e-6);
        double female = 142 * Math.pow(0.9938, 50) * 1.012;
        assertEquals(female, CkdEpi2021.estimate(0.7, 50, true), 1e-9);
        assertEquals(female, CkdEpi2021.estimate(0.7 - 1e-9, 50, true), 1e-6);
        assertEquals(female, CkdEpi2021.estimate(0.7 + 1e-9, 50, true), 1e-6);
    }

    @Test
    public void roundsToAWholeNumber() {
        // 142 x (1.00/0.9)^-1.200 x 0.9938^50 = 91.69
        assertEquals(Integer.valueOf(92), egfr("M", 50, "1.00"));
        // 142 x (0.62/0.7)^-0.241 x 0.9938^27 x 1.012 = 125.10
        assertEquals(Integer.valueOf(125), egfr("F", 27, "0.62"));
    }

    @Test
    public void convertsMicromolesPerLitre() {
        // 0.90 mg/dL x 88.4 = 79.56 umol/L, 1.50 mg/dL x 88.4 = 132.6 umol/L
        assertEquals(Integer.valueOf(127), calculate("79.56", "µmol/L", "M", 18).getEgfr());
        assertEquals(Integer.valueOf(44), calculate("132.6", "umol/L", "M", 90).getEgfr());
        assertEquals("Greek mu and a decimal comma", Integer.valueOf(33),
                calculate("132,6", "μmol/L", "F", 90).getEgfr());
        assertEquals(0.9, CkdEpi2021.creatinineMgDl("79.56", "µmol/L"), 1e-9);
        assertEquals(0.9, CkdEpi2021.creatinineMgDl("79.56", "UMOL/L"), 1e-9);
        assertEquals(1.2, CkdEpi2021.creatinineMgDl("1.2", "mg/dl"), 1e-9);
        assertEquals(1.2, CkdEpi2021.creatinineMgDl(" 1,2 ", "mg / dL"), 1e-9);
    }

    @Test
    public void usesTheAgeOnTheCollectionDay() {
        LocalDate birthDate = LocalDate.of(2008, 10, 16); // 18th birthday is the day after collection
        Outcome dayBefore = CkdEpi2021.calculate("0.90", "mg/dL", "M", birthDate, COLLECTED);
        assertFalse(dayBefore.isCalculated());
        assertEquals(SkipReason.UNDER_18, dayBefore.getSkipReason());

        Outcome onTheBirthday = CkdEpi2021.calculate("0.90", "mg/dL", "M", birthDate, COLLECTED.plusDays(1));
        assertEquals(Integer.valueOf(127), onTheBirthday.getEgfr());
        assertEquals(18, onTheBirthday.getAge());
    }

    @Test
    public void skipsPatientsUnder18() {
        assertSkipped(SkipReason.UNDER_18, calculate("0.90", "mg/dL", "M", 17));
        assertSkipped(SkipReason.UNDER_18, calculate("0.70", "mg/dL", "F", 0));
    }

    @Test
    public void skipsWhenSexIsMissingOrNeitherMaleNorFemale() {
        assertSkipped(SkipReason.MISSING_SEX, calculate("0.90", "mg/dL", null, 40));
        assertSkipped(SkipReason.MISSING_SEX, calculate("0.90", "mg/dL", "", 40));
        assertSkipped(SkipReason.MISSING_SEX, calculate("0.90", "mg/dL", "U", 40));
        assertEquals("lower case is accepted", egfr("F", 40, "0.90"), calculate("0.90", "mg/dL", "f", 40).getEgfr());
    }

    @Test
    public void skipsWhenBirthOrCollectionDateIsMissing() {
        assertSkipped(SkipReason.MISSING_BIRTH_DATE, CkdEpi2021.calculate("0.90", "mg/dL", "M", null, COLLECTED));
        assertSkipped(SkipReason.MISSING_COLLECTION_DATE,
                CkdEpi2021.calculate("0.90", "mg/dL", "M", COLLECTED.minusYears(40), null));
        assertSkipped(SkipReason.BIRTH_AFTER_COLLECTION,
                CkdEpi2021.calculate("0.90", "mg/dL", "M", COLLECTED.plusDays(1), COLLECTED));
    }

    @Test
    public void skipsCreatinineThatIsNotAPlainNumber() {
        assertSkipped(SkipReason.CREATININE_NOT_NUMERIC, calculate("<0.20", "mg/dL", "M", 40));
        assertSkipped(SkipReason.CREATININE_NOT_NUMERIC, calculate(">25", "mg/dL", "M", 40));
        assertSkipped(SkipReason.CREATININE_NOT_NUMERIC, calculate("Hemolysed", "mg/dL", "M", 40));
        assertSkipped(SkipReason.CREATININE_NOT_NUMERIC, calculate("0", "mg/dL", "M", 40));
        assertSkipped(SkipReason.CREATININE_NOT_NUMERIC, calculate("-1", "mg/dL", "M", 40));
        assertSkipped(SkipReason.NO_CREATININE_VALUE, calculate("", "mg/dL", "M", 40));
        assertSkipped(SkipReason.NO_CREATININE_VALUE, calculate(null, "mg/dL", "M", 40));
    }

    @Test
    public void doesNotGuessAnUnknownUnit() {
        assertSkipped(SkipReason.UNSUPPORTED_UNIT, calculate("9", "mg/L", "M", 40));
        assertSkipped(SkipReason.UNSUPPORTED_UNIT, calculate("0.9", null, "M", 40));
        assertNull(CkdEpi2021.creatinineMgDl("0.9", "mmol/L"));
    }

    private static void assertSkipped(SkipReason expected, Outcome outcome) {
        assertFalse(outcome.isCalculated());
        assertNull(outcome.getEgfr());
        assertEquals(expected, outcome.getSkipReason());
        assertTrue(outcome.getSkipReason().getText().length() > 0);
    }
}
