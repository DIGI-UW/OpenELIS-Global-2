package org.openelisglobal.addons.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * KDIGO 2012/2024 staging computed by the program — the only source of stages
 * for the assistant: it goes into the context as a fact, and {@link EgfrCheck}
 * checks answers against it. The model never has to work out a range or a
 * stage itself.
 *
 * <p>
 * eGFR (mL/min/1.73 m²) uses CONTINUOUS thresholds on the number exactly as
 * shown: G1 ≥ 90 · G2 ≥ 60 and &lt; 90 · G3a ≥ 45 and &lt; 60 · G3b ≥ 30 and
 * &lt; 45 · G4 ≥ 15 and &lt; 30 · G5 &lt; 15. The printed "45–59" means [45,
 * 60): 59.5 → G3a, 60.0 → G2, 44.9 → G3b. No rounding before staging (rounding
 * 59.5 up to 60 would change the stage while the screen still shows 59.5).
 * "&lt;5" → G5, "&gt;150" → G1.
 *
 * <p>
 * Albuminuria (ACR): A1 &lt; 30 · A2 30–300 · A3 &gt; 300 mg/g (= &lt; 3 · 3–30 ·
 * &gt; 30 mg/mmol). Another or unknown unit → not staged (null).
 */
public final class Kdigo {

    private Kdigo() {
    }

    /** A stage: code, printed range, lower bound (included), upper bound (excluded). */
    public record Band(String code, String range, double lo, double hiExcl, String meaningVi, String meaningEn) {
        public String meaning(Lang lang) {
            return lang.vi() ? meaningVi : meaningEn;
        }
    }

    public static final List<Band> GFR = List.of( //
            new Band("G1", "≥ 90", 90, Double.POSITIVE_INFINITY, "bình thường hoặc cao", "normal or high"),
            new Band("G2", "60–89", 60, 90, "giảm nhẹ", "mildly decreased"),
            new Band("G3a", "45–59", 45, 60, "giảm nhẹ đến trung bình", "mildly to moderately decreased"),
            new Band("G3b", "30–44", 30, 45, "giảm trung bình đến nặng", "moderately to severely decreased"),
            new Band("G4", "15–29", 15, 30, "giảm nặng", "severely decreased"),
            new Band("G5", "< 15", Double.NEGATIVE_INFINITY, 15, "suy thận", "kidney failure"));

    /** "G3" (no a/b): only to accept the grouped way of saying it. */
    static final Band G3 = new Band("G3", "30–59", 30, 60, "", "");

    public static final List<Band> ALB_MG_G = List.of( //
            new Band("A1", "< 30 mg/g", Double.NEGATIVE_INFINITY, 30, "bình thường đến tăng nhẹ",
                    "normal to mildly increased"),
            new Band("A2", "30–300 mg/g", 30, 300.000001, "tăng vừa", "moderately increased"),
            new Band("A3", "> 300 mg/g", 300.000001, Double.POSITIVE_INFINITY, "tăng nặng", "severely increased"));

    public static final List<Band> ALB_MG_MMOL = List.of( //
            new Band("A1", "< 3 mg/mmol", Double.NEGATIVE_INFINITY, 3, "bình thường đến tăng nhẹ",
                    "normal to mildly increased"),
            new Band("A2", "3–30 mg/mmol", 3, 30.000001, "tăng vừa", "moderately increased"),
            new Band("A3", "> 30 mg/mmol", 30.000001, Double.POSITIVE_INFINITY, "tăng nặng", "severely increased"));

    /** G stage of an eGFR (continuous thresholds, no rounding). */
    public static String gfrCategory(double egfr) {
        for (Band b : GFR) {
            if (egfr >= b.lo() && egfr < b.hiExcl()) {
                return b.code();
            }
        }
        return Double.isNaN(egfr) ? null : "G5";
    }

    public static Band gfrBand(String code) {
        if ("G3".equals(code)) {
            return G3;
        }
        for (Band b : GFR) {
            if (b.code().equals(code)) {
                return b;
            }
        }
        return null;
    }

    public static String gfrRange(String code) {
        Band b = gfrBand(code);
        return b == null ? "" : b.range();
    }

    /** "mg/g" | "mg/mmol" | null (not recognised). */
    public static String acrUnit(String unit) {
        if (unit == null) {
            return null;
        }
        String u = unit.toLowerCase(Locale.ROOT).replaceAll("\\s", "");
        if (u.startsWith("mg/mmol")) {
            return "mg/mmol";
        }
        if (u.startsWith("mg/g") || u.startsWith("µg/mg") || u.startsWith("ug/mg") || u.startsWith("mcg/mg")) {
            return "mg/g";
        }
        return null;
    }

    public static List<Band> albBands(String unit) {
        String u = acrUnit(unit);
        return u == null ? null : u.equals("mg/g") ? ALB_MG_G : ALB_MG_MMOL;
    }

    /** A stage of an ACR; null if the unit is unknown. */
    public static String albuminuriaCategory(double acr, String unit) {
        List<Band> bands = albBands(unit);
        if (bands == null || Double.isNaN(acr)) {
            return null;
        }
        for (Band b : bands) {
            if (acr >= b.lo() && acr < b.hiExcl()) {
                return b.code();
            }
        }
        return null;
    }

    public static Band albBand(String code, String unit) {
        List<Band> bands = albBands(unit);
        if (bands == null) {
            return null;
        }
        for (Band b : bands) {
            if (b.code().equals(code)) {
                return b;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ facts for the context

    /**
     * A computed fact: {@code kind} "egfr" | "acr"; {@code label} "Ca 2" / "Case 2"
     * on the validation page (null otherwise); {@code shown} = the number as the
     * context shows it; {@code when} null = the current order, otherwise "3 months
     * ago"…
     */
    public record Fact(String kind, String label, String shown, double value, String unit, String category,
            String when) {
        public boolean current() {
            return when == null;
        }

        public String range() {
            if ("egfr".equals(kind)) {
                return gfrRange(category);
            }
            Band b = albBand(category, unit);
            return b == null ? "" : b.range();
        }
    }

    /** eGFR fact (Vietnamese unit); null if not a number. */
    public static Fact egfrFact(String label, String shown, String when) {
        return egfrFact(Lang.VI, label, shown, when);
    }

    public static Fact egfrFact(Lang lang, String label, String shown, String when) {
        Double v = Values.number(shown);
        if (v == null) {
            return null;
        }
        String t = shown.trim();
        double value = t.startsWith("<") ? Math.min(v, 14.99) : v; // "<5" → G5, ">150" → G1
        return new Fact("egfr", label, t, value, lang.egfrUnit, gfrCategory(value), when);
    }

    /** ACR fact; null if not a number or the unit is unknown. */
    public static Fact acrFact(String label, String shown, String unit, String when) {
        Double v = Values.number(shown);
        if (v == null) {
            return null;
        }
        String cat = albuminuriaCategory(v, unit);
        return cat == null ? null : new Fact("acr", label, shown.trim(), v, acrUnit(unit), cat, when);
    }

    /** The KDIGO table of the server (same source as the computation). */
    public static String table() {
        return table(Lang.VI);
    }

    public static String table(Lang lang) {
        List<String> parts = new ArrayList<>();
        for (Band b : GFR) {
            parts.add(b.code() + " " + b.range());
        }
        if (lang.vi()) {
            return "BẢNG KDIGO CHUẨN CỦA MÁY CHỦ — eGFR (mL/ph/1,73 m²): " + String.join(" · ", parts)
                    + ". Ngưỡng liên tục, không làm tròn: < 60 là G3a (vd 59,5 → G3a), ≥ 60 là G2; < 45 là G3b (vd 44,9 → G3b)."
                    + " Albumin niệu ACR: A1 < 30 · A2 30–300 · A3 > 300 mg/g (= < 3 · 3–30 · > 30 mg/mmol).";
        }
        return "KDIGO TABLE OF THE SERVER — eGFR (mL/min/1.73 m²): " + String.join(" · ", parts)
                + ". Continuous thresholds, no rounding: < 60 is G3a (e.g. 59.5 → G3a), ≥ 60 is G2; < 45 is G3b"
                + " (e.g. 44.9 → G3b). Albuminuria ACR: A1 < 30 · A2 30–300 · A3 > 300 mg/g"
                + " (= < 3 · 3–30 · > 30 mg/mmol).";
    }

    /** The block of facts sent to the model; empty without facts. */
    public static String factsBlock(List<Fact> facts) {
        return factsBlock(facts, Lang.VI);
    }

    public static String factsBlock(List<Fact> facts, Lang lang) {
        if (facts == null || facts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(lang.vi()
                ? "PHÂN ĐỘ KDIGO DO MÁY CHỦ TÍNH SẴN (dữ kiện — dùng NGUYÊN VĂN độ và khoảng dưới đây, KHÔNG tự phân độ lại,"
                        + " KHÔNG tự viết khoảng khác bảng; nhắc tới độ khác thì ghi đúng khoảng của độ đó trong bảng):\n"
                : "KDIGO STAGES COMPUTED BY THE SERVER (facts — use the stage and range below VERBATIM, do NOT stage"
                        + " again, do NOT write another range; when you mention another stage, write that stage's own"
                        + " range from the table):\n");
        for (Fact f : facts) {
            sb.append("- ").append(f.label() == null ? "" : f.label() + ": ")
                    .append("egfr".equals(f.kind()) ? "eGFR " : "ACR ");
            if (lang.vi()) {
                sb.append(f.current() ? "phiếu đang xem " : "lần trước (" + f.when() + ") ");
            } else {
                sb.append(f.current() ? "current order " : "earlier (" + f.when() + ") ");
            }
            sb.append(lang.number(f.shown())).append(' ').append(f.unit()).append(" → **").append(f.category())
                    .append(" (").append(f.range()).append(")**\n");
        }
        sb.append(table(lang)).append('\n');
        return sb.toString();
    }

    /** Decimal comma. */
    static String vn(String s) {
        return Lang.VI.number(s);
    }
}
