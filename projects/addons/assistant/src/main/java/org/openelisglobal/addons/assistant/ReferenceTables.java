package org.openelisglobal.addons.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Picks the reference tables sent WITH the context so that the model looks them
 * up instead of remembering — only tables related to the tests in the context.
 * The tables themselves are in the rules file ({@code <!-- assistant:table <key> -->}).
 * Matching uses test codes, names (Vietnamese and English) and LOINC codes.
 */
public final class ReferenceTables {

    private ReferenceTables() {
    }

    /** Table keys (ckd, glucose, lipid, liver, cbc, uric, urine, hepatitis) from test codes, names and LOINC codes. */
    public static List<String> select(Collection<String> codesAndNames) {
        Set<String> picked = new LinkedHashSet<>();
        for (String raw : codesAndNames) {
            if (raw == null) {
                continue;
            }
            String s = fold(raw);
            if (s.contains("egfr") || s.contains("creatinin") || s.contains("cau than") || s.equals("crea")
                    || s.matches("(?s).*\\bure(a)?\\b.*") || s.matches("(?s).*\\bacr\\b.*")
                    || s.contains("alb/cre") || s.contains("micro alb") || s.contains("albumin/creatinin")
                    || s.contains("kidney") || s.contains("glomerular") || LOINC_CKD.contains(s)) {
                picked.add("ckd");
            }
            if (s.contains("gluc") || s.contains("hba1c") || s.contains("duong huyet") || s.equals("glu")
                    || LOINC_GLUCOSE.contains(s)) {
                picked.add("glucose");
            }
            if (s.contains("chol") || s.contains("triglycerid") || s.equals("tg") || s.contains("hdl")
                    || s.contains("ldl")) {
                picked.add("lipid");
            }
            if (s.matches("(?s).*\\b(ast|alt|sgot|sgpt|ggt)\\b.*")) {
                picked.add("liver");
            }
            if (s.matches("(?s).*\\b(wbc|rbc|hgb|hb|hct|mcv|mch|mchc|plt|neu|lym|eos|mono|baso|rdw)\\b.*")
                    || s.contains("cong thuc mau") || s.contains("huyet sac to") || s.contains("tieu cau")
                    || s.contains("bach cau") || s.contains("hong cau")) {
                picked.add("cbc");
            }
            if (s.contains("uric")) {
                picked.add("uric");
            }
            if (s.startsWith("u-") || s.contains("nuoc tieu") || s.matches("(leu|nit|ubg|pro|blo|sg|ket|bil|ph)")) {
                picked.add("urine");
            }
            if (s.contains("hbsag") || s.contains("anti hbs") || s.contains("anti-hbs") || s.contains("hcv")) {
                picked.add("hepatitis");
            }
        }
        return new ArrayList<>(picked);
    }

    // eGFR (several equations), creatinine, urea, urine albumin/creatinine ratio
    private static final java.util.Set<String> LOINC_CKD = java.util.Set.of("62238-1", "98979-8", "33914-3", "48642-3",
            "48643-1", "50044-7", "88293-6", "88294-4", "2160-0", "38483-4", "3094-0", "6299-2", "9318-7", "14959-1",
            "14958-3", "32294-1");
    private static final java.util.Set<String> LOINC_GLUCOSE = java.util.Set.of("2345-7", "1558-6", "2339-0", "4548-4",
            "17856-6");

    private static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replace('đ', 'd')
                .replace('Đ', 'D').toLowerCase(Locale.ROOT).trim();
    }
}
