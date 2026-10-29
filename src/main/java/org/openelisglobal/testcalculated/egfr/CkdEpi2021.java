package org.openelisglobal.testcalculated.egfr;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Estimated GFR from serum creatinine with the race-free CKD-EPI 2021 equation.
 * Pure calculation, no database access.
 *
 * <p>
 * Inker LA, Eneanya ND, Coresh J, et al. New creatinine- and cystatin C-based
 * equations to estimate GFR without race. N Engl J Med 2021;385(19):1737-49.
 * doi:10.1056/NEJMoa2102953
 *
 * <pre>
 * eGFRcr = 142 x min(Scr/k, 1)^a x max(Scr/k, 1)^-1.200 x 0.9938^age x 1.012 [if female]
 *   k = 0.7 (female) / 0.9 (male);  a = -0.241 (female) / -0.302 (male)
 *   Scr = standardized serum creatinine in mg/dL;  age in years;  result in mL/min/1.73 m2
 * </pre>
 *
 * The equation is validated for adults only, so patients under 18 are not
 * calculated. Creatinine in umol/L is converted with 1 mg/dL = 88.4 umol/L.
 * Results that are not a plain number (for example "&lt;0.20" or a text result)
 * are not calculated.
 */
public final class CkdEpi2021 {

    public static final String METHOD = "CKD-EPI 2021";
    public static final String UNIT = "mL/min/1.73 m2";
    public static final double UMOL_PER_MG_DL = 88.4;
    public static final int MINIMUM_AGE = 18;

    /** Why an eGFR was not calculated. */
    public enum SkipReason {
        NO_CREATININE_VALUE("the creatinine result is empty"),
        CREATININE_NOT_NUMERIC("the creatinine result is not a plain number"),
        UNSUPPORTED_UNIT("the creatinine unit is not mg/dL or µmol/L"), NO_PATIENT("the sample has no patient"),
        MISSING_SEX("the patient's sex is not recorded as male or female"),
        MISSING_BIRTH_DATE("the patient's birth date is missing"),
        MISSING_COLLECTION_DATE("the sample has no collection or received date"),
        BIRTH_AFTER_COLLECTION("the patient's birth date is after the collection date"),
        UNDER_18("the patient was under 18 years old at collection");

        private final String text;

        SkipReason(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    /** Either an eGFR value or the reason it was not calculated. */
    public static final class Outcome {
        private final Integer egfr;
        private final double creatinineMgDl;
        private final int age;
        private final boolean female;
        private final SkipReason skipReason;

        private Outcome(Integer egfr, double creatinineMgDl, int age, boolean female, SkipReason skipReason) {
            this.egfr = egfr;
            this.creatinineMgDl = creatinineMgDl;
            this.age = age;
            this.female = female;
            this.skipReason = skipReason;
        }

        public static Outcome skipped(SkipReason reason) {
            return new Outcome(null, Double.NaN, -1, false, reason);
        }

        public boolean isCalculated() {
            return egfr != null;
        }

        /** eGFR in mL/min/1.73 m2, rounded to a whole number. */
        public Integer getEgfr() {
            return egfr;
        }

        public double getCreatinineMgDl() {
            return creatinineMgDl;
        }

        public int getAge() {
            return age;
        }

        public boolean isFemale() {
            return female;
        }

        public SkipReason getSkipReason() {
            return skipReason;
        }
    }

    private static final Pattern NUMBER = Pattern.compile("^\\s*(\\d+(?:[.,]\\d+)?)\\s*$");

    private CkdEpi2021() {
    }

    /**
     * The equation itself.
     *
     * @param creatinineMgDl standardized serum creatinine, mg/dL, greater than 0
     * @param age            age in whole years
     * @param female         true for female, false for male
     * @return eGFR in mL/min/1.73 m2, not rounded
     */
    public static double estimate(double creatinineMgDl, int age, boolean female) {
        double kappa = female ? 0.7 : 0.9;
        double alpha = female ? -0.241 : -0.302;
        double ratio = creatinineMgDl / kappa;
        return 142 * Math.pow(Math.min(ratio, 1), alpha) * Math.pow(Math.max(ratio, 1), -1.200) * Math.pow(0.9938, age)
                * (female ? 1.012 : 1.0);
    }

    /**
     * Creatinine converted to mg/dL, or null when the value is not a positive plain
     * number or the unit is neither mg/dL nor µmol/L.
     */
    public static Double creatinineMgDl(String value, String unit) {
        if (value == null) {
            return null;
        }
        Matcher matcher = NUMBER.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        double number = Double.parseDouble(matcher.group(1).replace(',', '.'));
        if (number <= 0) {
            return null;
        }
        if (isMgDl(unit)) {
            return number;
        }
        if (isUmolL(unit)) {
            return number / UMOL_PER_MG_DL;
        }
        return null;
    }

    static boolean isMgDl(String unit) {
        return "mg/dl".equals(normalizeUnit(unit));
    }

    static boolean isUmolL(String unit) {
        String normalized = normalizeUnit(unit);
        return "µmol/l".equals(normalized) || "umol/l".equals(normalized);
    }

    private static String normalizeUnit(String unit) {
        if (unit == null) {
            return null;
        }
        // U+03BC (Greek mu) and U+00B5 (micro sign) are both used for "micro"
        return unit.replace(" ", "").replace('μ', 'µ').toLowerCase(Locale.ROOT);
    }

    /** Age in completed years on the given day, or null if it can't be known. */
    public static Integer ageOn(LocalDate birthDate, LocalDate day) {
        if (birthDate == null || day == null || birthDate.isAfter(day)) {
            return null;
        }
        return Period.between(birthDate, day).getYears();
    }

    /**
     * Calculate the eGFR for one creatinine result, or say why it can't be.
     *
     * @param creatinineValue the creatinine result as stored
     * @param creatinineUnit  the unit of measure of the creatinine test
     * @param sex             patient gender code, "M" or "F"
     * @param birthDate       patient birth date
     * @param collectionDate  the day the sample was collected
     */
    public static Outcome calculate(String creatinineValue, String creatinineUnit, String sex, LocalDate birthDate,
            LocalDate collectionDate) {
        if (creatinineValue == null || creatinineValue.isBlank()) {
            return Outcome.skipped(SkipReason.NO_CREATININE_VALUE);
        }
        if (!isMgDl(creatinineUnit) && !isUmolL(creatinineUnit)) {
            return Outcome.skipped(SkipReason.UNSUPPORTED_UNIT);
        }
        Double creatinine = creatinineMgDl(creatinineValue, creatinineUnit);
        if (creatinine == null) {
            return Outcome.skipped(SkipReason.CREATININE_NOT_NUMERIC);
        }
        String gender = sex == null ? "" : sex.trim().toUpperCase(Locale.ROOT);
        if (!"M".equals(gender) && !"F".equals(gender)) {
            return Outcome.skipped(SkipReason.MISSING_SEX);
        }
        if (birthDate == null) {
            return Outcome.skipped(SkipReason.MISSING_BIRTH_DATE);
        }
        if (collectionDate == null) {
            return Outcome.skipped(SkipReason.MISSING_COLLECTION_DATE);
        }
        Integer age = ageOn(birthDate, collectionDate);
        if (age == null) {
            return Outcome.skipped(SkipReason.BIRTH_AFTER_COLLECTION);
        }
        if (age < MINIMUM_AGE) {
            return Outcome.skipped(SkipReason.UNDER_18);
        }
        boolean female = "F".equals(gender);
        int egfr = (int) Math.round(estimate(creatinine, age, female));
        return new Outcome(egfr, creatinine, age, female, null);
    }
}
