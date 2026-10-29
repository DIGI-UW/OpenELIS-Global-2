package org.openelisglobal.testcalculated.egfr;

import java.util.Locale;

/**
 * What to do with an ordered eGFR when its creatinine result, or the patient's
 * sex or birth date, is saved. Pure logic, no database access.
 *
 * <ul>
 * <li>An eGFR the calculation did not write (entered by hand, or sent by an
 * analyzer) is never replaced or cleared.</li>
 * <li>A validated eGFR is never changed.</li>
 * <li>A calculated eGFR follows its creatinine and its patient: it is
 * recalculated when either changes, and cleared when it can no longer be
 * calculated.</li>
 * <li>When no eGFR is calculated, a note says why.</li>
 * </ul>
 *
 * <p>
 * The first value written into an empty eGFR is a plain calculation. Changing a
 * stored value, clearing it, or finding a validated value the equation no
 * longer agrees with is a {@linkplain #isCorrection() correction}, which people
 * have to hear about.
 */
public final class EgfrDecision {

    public enum Action {
        NONE, WRITE, CLEAR
    }

    /** What was saved that made the eGFR be looked at again. */
    public enum Cause {
        CREATININE_SAVED("the creatinine was corrected"), PATIENT_CHANGED("patient sex/birth date changed");

        private final String text;

        Cause(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    private static final String METHOD_NOTE = CkdEpi2021.METHOD + " (race-free)";

    private final Action action;
    private final String value;
    private final String note;
    private final String previousValue;
    private final boolean correction;

    private EgfrDecision(Action action, String value, String note) {
        this(action, value, note, null, false);
    }

    private EgfrDecision(Action action, String value, String note, String previousValue, boolean correction) {
        this.action = action;
        this.value = value;
        this.note = note;
        this.previousValue = previousValue;
        this.correction = correction;
    }

    public Action getAction() {
        return action;
    }

    /**
     * The eGFR to write, for {@link Action#WRITE}; for a correction left alone
     * because the eGFR is validated, what the equation now gives.
     */
    public String getValue() {
        return value;
    }

    /** Internal note for the eGFR test, or null when there is nothing to say. */
    public String getNote() {
        return note;
    }

    /** The stored eGFR, for a {@linkplain #isCorrection() correction}. */
    public String getPreviousValue() {
        return previousValue;
    }

    /**
     * True when a stored eGFR value is replaced or cleared, or when a validated
     * eGFR no longer matches the equation. False for the first calculation and when
     * nothing changes.
     */
    public boolean isCorrection() {
        return correction;
    }

    /**
     * After a creatinine result is saved; see
     * {@link #decide(Cause, CkdEpi2021.Outcome, String, String, String, boolean, boolean)}.
     */
    public static EgfrDecision decide(CkdEpi2021.Outcome outcome, String creatinineValue, String creatinineUnit,
            String currentEgfr, boolean calculatedEgfr, boolean egfrFinalized) {
        return decide(Cause.CREATININE_SAVED, outcome, creatinineValue, creatinineUnit, currentEgfr, calculatedEgfr,
                egfrFinalized);
    }

    /**
     * @param cause           what was saved: the creatinine, or the patient's sex
     *                        or birth date
     * @param outcome         the calculation with the creatinine and patient as now
     *                        stored
     * @param creatinineValue the creatinine result, for the note
     * @param creatinineUnit  the creatinine unit, for the note
     * @param currentEgfr     the eGFR result currently stored, null or blank if
     *                        none
     * @param calculatedEgfr  true if the stored eGFR was written by this
     *                        calculation, false if it was entered by hand or by an
     *                        analyzer
     * @param egfrFinalized   true if the eGFR test is already validated
     */
    public static EgfrDecision decide(Cause cause, CkdEpi2021.Outcome outcome, String creatinineValue,
            String creatinineUnit, String currentEgfr, boolean calculatedEgfr, boolean egfrFinalized) {
        boolean hasValue = currentEgfr != null && !currentEgfr.isBlank();
        String current = hasValue ? currentEgfr.trim() : null;
        String calculated = outcome.isCalculated() ? String.valueOf(outcome.getEgfr()) : null;
        boolean differs = calculated != null && !calculated.equals(current);
        boolean patientChanged = cause == Cause.PATIENT_CHANGED;

        if (egfrFinalized) {
            if (patientChanged && hasValue && calculatedEgfr && !outcome.isCalculated()) {
                return new EgfrDecision(Action.NONE, null,
                        "eGFR is already validated and was not removed after " + cause.getText()
                                + ", although it can no longer be calculated: " + outcome.getSkipReason().getText()
                                + ".",
                        current, true);
            }
            if (!differs || (patientChanged && hasValue && !calculatedEgfr)) {
                return new EgfrDecision(Action.NONE, null, null);
            }
            String note = patientChanged
                    ? "eGFR is already validated and was not recalculated after " + cause.getText() + ". " + METHOD_NOTE
                            + " now gives " + calculated + " " + CkdEpi2021.UNIT + " instead of " + current + "."
                    : "eGFR is already validated and was not recalculated. " + METHOD_NOTE + " now gives " + calculated
                            + " " + CkdEpi2021.UNIT + ".";
            return new EgfrDecision(Action.NONE, calculated, note, current, hasValue && calculatedEgfr);
        }
        if (hasValue && !calculatedEgfr) {
            // entered by hand or by an analyzer: a creatinine save notes a
            // disagreement; a patient change leaves it entirely alone
            return new EgfrDecision(Action.NONE, null,
                    differs && !patientChanged
                            ? "eGFR " + current + " was entered by hand and was not replaced. "
                                    + detail(outcome, creatinineValue, creatinineUnit)
                            : null);
        }
        if (!outcome.isCalculated()) {
            String reason = outcome.getSkipReason().getText();
            if (hasValue) {
                return new EgfrDecision(
                        Action.CLEAR, null, "Calculated eGFR " + current + " removed"
                                + (patientChanged ? " after " + cause.getText() : "") + ": " + reason + ".",
                        current, true);
            }
            // an empty creatinine (not entered yet, or cleared) needs no explanation
            boolean worthNoting = outcome.getSkipReason() != CkdEpi2021.SkipReason.NO_CREATININE_VALUE;
            return new EgfrDecision(Action.NONE, null,
                    worthNoting ? "eGFR not calculated (" + CkdEpi2021.METHOD + "): " + reason + "." : null);
        }
        if (!differs) {
            return new EgfrDecision(Action.NONE, null, null);
        }
        String detail = detail(outcome, creatinineValue, creatinineUnit);
        if (!hasValue) {
            // the first calculation
            return new EgfrDecision(Action.WRITE, calculated, detail);
        }
        return new EgfrDecision(Action.WRITE, calculated, "Recalculated after " + cause.getText() + ": " + current
                + " \u2192 " + calculated + " " + CkdEpi2021.UNIT + ". " + detail, current, true);
    }

    /**
     * "Calculated with CKD-EPI 2021 (race-free) from creatinine 0.62 mg/dL, female,
     * 27 years at collection: 125 mL/min/1.73 m2."
     */
    static String detail(CkdEpi2021.Outcome outcome, String creatinineValue, String creatinineUnit) {
        String creatinine = (creatinineValue == null ? "?" : creatinineValue.trim())
                + (creatinineUnit == null ? "" : " " + creatinineUnit.trim());
        if (CkdEpi2021.isUmolL(creatinineUnit)) {
            creatinine += String.format(Locale.ROOT, " (%.2f mg/dL)", outcome.getCreatinineMgDl());
        }
        return "Calculated with " + METHOD_NOTE + " from creatinine " + creatinine + ", "
                + (outcome.isFemale() ? "female" : "male") + ", " + outcome.getAge() + " years at collection: "
                + outcome.getEgfr() + " " + CkdEpi2021.UNIT + ".";
    }
}
