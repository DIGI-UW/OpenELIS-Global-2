package org.openelisglobal.addons.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The post-answer check with the English rule set: the same guarantees as the Vietnamese one. */
class EnglishCheckTest {

    private static EgfrCheck.Outcome check(String answer, double egfr) {
        return EgfrCheck.check(answer, List.of(egfr), Lang.EN);
    }

    @Test
    void aWrongStageForThePatientIsReplacedWithItsRange() {
        EgfrCheck.Outcome o = check("eGFR 59 → G2 (60–89), mildly decreased.", 59);
        assertTrue(o.corrected());
        assertTrue(o.text().startsWith("eGFR 59 → G3a (45–59), mildly decreased."), o.text());
        assertTrue(o.text().contains("⚠️ The system corrected the kidney staging: eGFR 59 is G3a"), o.text());
        assertTrue(check("The patient is in CKD stage 2.", 55.6).corrected());
    }

    /** The 19/10 regression in English: a mentioned neighbour stage is not the patient's stage. */
    @Test
    void aNeighbourStageIsNotRewritten() {
        for (String ok : List.of("- **eGFR 59 → G3a (45–59)**, close to the G2 (60–89) boundary.",
                "eGFR 89 → G2 (60–89); the stage below is G3a (45–59).", "eGFR 44 → G3b (30–44), near G3a (45–59).",
                "eGFR 60 → G2 (60–89). This is exactly the lower limit of G2 (≥ 60 is G2, no rounding).")) {
            EgfrCheck.Outcome o = EgfrCheck.check(ok, List.of(Double.parseDouble(ok.replaceAll("^\\D*(\\d+).*$", "$1"))),
                    Lang.EN);
            assertFalse(o.corrected(), o.text());
            assertEquals(ok, o.text());
        }
    }

    @Test
    void aWrongRangeNextToAStageIsFixed() {
        EgfrCheck.Outcome o = check("eGFR 59 → G3a (60–89).", 59);
        assertTrue(o.corrected());
        String body = o.text().substring(0, o.text().indexOf("⚠️"));
        assertFalse(body.contains("60–89"), o.text());
        assertTrue(body.contains("G3a (45–59)"), o.text());
    }

    @Test
    void boundaries() {
        assertFalse(check("eGFR 59.5 → G3a (45–59).", 59.5).corrected());
        assertTrue(check("eGFR 59.5 → G2 (60–89).", 59.5).corrected());
        assertFalse(check("eGFR 60 → G2 (60–89).", 60).corrected());
        assertTrue(check("eGFR 60 → G3a (45–59).", 60).corrected());
        assertFalse(check("eGFR 44 → G3b (30–44).", 44).corrected());
        assertTrue(check("eGFR 44 → G3a (45–59).", 44).corrected());
        assertFalse(check("eGFR 89 → G2 (60–89).", 89).corrected());
        assertTrue(check("eGFR 89 → G1 (≥ 90).", 89).corrected());
    }

    @Test
    void negationAndNeutralSentencesAreLeftAlone() {
        for (String ok : List.of("eGFR 55.6 is not stage 2 but G3a.", "Stage 2 hypertension needs a repeat reading.",
                "Glucose 6.38 mmol/L, consider HbA1c.")) {
            assertEquals(ok, check(ok, 55.6).text());
        }
    }

    @Test
    void casesUseTheirOwnValuesAndTheEnglishLabel() {
        List<Kdigo.Fact> facts = List.of(Kdigo.egfrFact(Lang.EN, "Case 1", "59", null),
                Kdigo.egfrFact(Lang.EN, "Case 2", "75", null));
        assertFalse(EgfrCheck.checkFacts("**Case 1**: eGFR 59 → G3a (45–59).\n**Case 2**: eGFR 75 → G2 (60–89).",
                facts, Lang.EN).corrected());
        EgfrCheck.Outcome o = EgfrCheck.checkFacts("**Case 2**\n- Kidney function: G3a.", facts, Lang.EN);
        assertTrue(o.text().contains("Kidney function: G2."), o.text());
    }

    @Test
    void theEnglishDisclaimerEndsEveryAnswerOnce() {
        AnswerPostProcessor.Processed p = AnswerPostProcessor.process(
                "All fine.\n\n*Suggestion for reference only - not a substitute for a physician's diagnosis*",
                List.of(), null, Lang.EN);
        assertEquals("All fine.\n\n" + Lang.EN.disclaimer, p.text());
    }

    /** Seen on the test lab: quoting the albuminuria table without an ACR result was flagged. */
    @Test
    void aQuotedAlbuminuriaTableIsNotAStage() {
        String a = "- No albuminuria (ACR) result, which would help complete the staging (A1 < 30, A2 30–300, A3 > 300 mg/g).";
        assertFalse(check(a, 59).corrected(), check(a, 59).text());
        assertTrue(check("Albuminuria A1.", 59).corrected(), "a stage claimed without ACR is still flagged");
        String vi = "- Chưa có ACR (albumin niệu) để phân độ (A1 < 30, A2 30–300, A3 > 300 mg/g).";
        assertFalse(EgfrCheck.check(vi, List.of(59.0)).corrected());
    }
}

