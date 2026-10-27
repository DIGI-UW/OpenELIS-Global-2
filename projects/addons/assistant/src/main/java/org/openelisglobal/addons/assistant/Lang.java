package org.openelisglobal.addons.assistant;

import java.util.Locale;

/**
 * The language of the rule set, of the context the model reads, and of what the
 * service adds to an answer (correction notes, the disclaimer). English is the
 * default; Vietnamese is the rule set of the laboratory the assistant was first
 * built for.
 */
public enum Lang {
    EN("en", '.', "mL/min/1.73 m²", "Suggestion for reference only — not a substitute for a physician's diagnosis."),
    VI("vi", ',', "mL/ph/1,73 m²", "Gợi ý tham khảo — không thay chẩn đoán của bác sĩ.");

    public final String code;
    /** Decimal separator used when the service writes numbers. */
    public final char decimal;
    /** Unit of eGFR as the service writes it. */
    public final String egfrUnit;
    /** The line every answer ends with. */
    public final String disclaimer;

    Lang(String code, char decimal, String egfrUnit, String disclaimer) {
        this.code = code;
        this.decimal = decimal;
        this.egfrUnit = egfrUnit;
        this.disclaimer = disclaimer;
    }

    public static Lang of(String code) {
        return code != null && code.trim().toLowerCase(Locale.ROOT).startsWith("vi") ? VI : EN;
    }

    /** {@code 55.6} → "55,6" in Vietnamese; unchanged in English. */
    public String number(String s) {
        return s == null ? "" : decimal == ',' ? s.replaceAll("(\\d)\\.(\\d)", "$1,$2") : s;
    }

    public boolean vi() {
        return this == VI;
    }
}
