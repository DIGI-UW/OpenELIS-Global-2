package org.openelisglobal.addons.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns the de-identified context OpenELIS returns for the {@code ai.context}
 * scope ({@code GET /ai/context}) into the text the model reads, in the
 * language of the rules, plus the facts the service needs for suggestions and
 * the post-answer check.
 *
 * <p>
 * Pages: {@code lookup} (one patient: the current order, then earlier results
 * of the same tests), {@code validation} ("Case 1…N", only abnormal results and
 * large changes), {@code other} (only the screen name). Nothing identifying
 * reaches this class: OpenELIS removed it.
 */
public final class ContextText {

    public static final String LOOKUP = "lookup";
    public static final String VALIDATION = "validation";
    public static final String OTHER = "other";

    /** The context ready for the model, plus facts for suggestions and checks. */
    public record Built(String page, String text, String hash, List<Double> egfrValues, List<String> testKeys,
            List<Map<String, String>> abnormal, List<Map<String, String>> caseLabels, String summary, String subjectKey,
            String subjectKind, List<Kdigo.Fact> kidneyFacts, String userId) {

        public Built(String page, String text, String hash, List<Double> egfrValues, List<String> testKeys,
                List<Map<String, String>> abnormal, List<Map<String, String>> caseLabels, String summary,
                String subjectKey, String subjectKind) {
            this(page, text, hash, egfrValues, testKeys, abnormal, caseLabels, summary, subjectKey, subjectKind, null,
                    null);
        }
    }

    private static final int TEXT_RESULT_MAX = 60;
    private static final Set<String> LOINC_EGFR = Set.of("62238-1", "98979-8", "33914-3", "48642-3", "48643-1",
            "50044-7", "88293-6", "88294-4", "69405-9", "77147-7");
    private static final Set<String> LOINC_ACR = Set.of("9318-7", "14959-1", "14958-3", "32294-1");

    private ContextText() {
    }

    /**
     * @param ctx       the JSON from {@code /ai/context}; null for a page without
     *                  orders
     * @param page      {@link #LOOKUP}, {@link #VALIDATION} or {@link #OTHER}
     * @param pageName  for {@link #OTHER}: the screen the user is on
     *                  ("result-validation")
     */
    public static Built build(JsonNode ctx, String page, String pageName, Lang lang) {
        String user = ctx == null ? null : ctx.path("user").asText(null);
        String subject = ctx == null ? "none" : ctx.path("subject").asText("none");
        if (LOOKUP.equals(page)) {
            return lookup(ctx, subject, user, lang);
        }
        if (VALIDATION.equals(page)) {
            return validation(ctx, user, lang);
        }
        String name = pageName == null || pageName.isBlank() ? "" : pageName.replaceAll("[^a-z0-9-]", "");
        String text = screenLine(lang, name.isEmpty() ? null : name);
        return built(OTHER, text, List.of(), List.of(), List.of(), List.of(), "", "route:" + name, "page",
                List.of(), user);
    }

    // ------------------------------------------------------------------ lookup (one patient)

    private static Built lookup(JsonNode ctx, String subject, String user, Lang lang) {
        JsonNode c = ctx == null ? null : ctx.path("cases").path(0);
        String subjectKey = "patient:" + subject;
        String screen = screenLine(lang, null, LOOKUP);
        if (c == null || c.isMissingNode() || c.path("results").size() == 0) {
            String text = screen + (lang.vi() ? "Chưa mở bệnh nhân nào (chọn một dòng trong danh sách để xem kết quả).\n"
                    : "No patient is open (select a row to see results).\n");
            return built(LOOKUP, text, List.of(), List.of(), List.of(), List.of(), "", subjectKey, "patient", List.of(),
                    user);
        }
        List<JsonNode> results = list(c.path("results"));
        List<JsonNode> previous = list(c.path("previous"));
        StringBuilder sb = new StringBuilder(screen);
        String sexAge = sexAge(c.path("patient"), lang);
        Long ago = c.path("receivedDaysAgo").isNumber() ? c.path("receivedDaysAgo").asLong() : null;
        if (lang.vi()) {
            sb.append("BỆNH NHÂN: ").append(sexAge).append(".\n");
            sb.append("PHIẾU ĐANG XEM: nhận mẫu ").append(relative(ago, lang).replace("cùng ngày", "hôm nay"))
                    .append(", ").append(results.size()).append(" kết quả.\n");
            sb.append("KẾT QUẢ:\n");
        } else {
            sb.append("PATIENT: ").append(sexAge).append(".\n");
            sb.append("CURRENT ORDER: received ").append(relative(ago, lang).replace("same day", "today"))
                    .append(", ").append(results.size()).append(" results.\n");
            sb.append("RESULTS:\n");
        }
        Map<String, List<JsonNode>> bySection = new LinkedHashMap<>();
        for (JsonNode r : results) {
            String section = text(r, "section");
            bySection.computeIfAbsent(section.isBlank() ? (lang.vi() ? "Khác" : "Other") : section,
                    k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<JsonNode>> e : bySection.entrySet()) {
            sb.append('[').append(section(e.getKey(), lang)).append("]\n");
            for (JsonNode r : e.getValue()) {
                sb.append(resultLine(r, lang)).append('\n');
            }
        }
        if (!previous.isEmpty()) {
            sb.append(lang.vi() ? "KẾT QUẢ CÁC LẦN TRƯỚC (cùng xét nghiệm, tính từ phiếu đang xem):\n"
                    : "EARLIER RESULTS (same tests, counted from the current order):\n");
            for (JsonNode p : previous) {
                sb.append(resultLine(p, lang)).append(" — ").append(relative(days(p, "daysBefore"), lang)).append('\n');
            }
        } else {
            sb.append(lang.vi() ? "KẾT QUẢ CÁC LẦN TRƯỚC: không có trong hệ thống.\n"
                    : "EARLIER RESULTS: none in the system.\n");
        }
        List<Kdigo.Fact> facts = kidneyFacts(null, results, previous, lang);
        sb.append(Kdigo.factsBlock(facts, lang));
        List<Map<String, String>> abnormal = abnormal(results, lang);
        String summary = sexAge + " · " + results.size() + (lang.vi() ? " kết quả · " : " results · ")
                + abnormal.size() + (lang.vi() ? " bất thường" : " abnormal");
        return built(LOOKUP, sb.toString(), egfr(results, previous), testKeys(results), abnormal, List.of(), summary,
                subjectKey, "patient", facts, user);
    }

    // ------------------------------------------------------------------ validation (cases)

    private static Built validation(JsonNode ctx, String user, Lang lang) {
        List<JsonNode> cases = ctx == null ? List.of() : list(ctx.path("cases"));
        StringBuilder sb = new StringBuilder(screenLine(lang, null, VALIDATION));
        List<Map<String, String>> labels = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        List<Kdigo.Fact> facts = new ArrayList<>();
        int abnormalTotal = 0;
        if (cases.isEmpty()) {
            sb.append(lang.vi() ? "Không có phiếu nào đang chờ duyệt.\n" : "No order is waiting for validation.\n");
        } else if (lang.vi()) {
            sb.append("DANH SÁCH CHỜ DUYỆT: ").append(cases.size())
                    .append(" ca. Mỗi ca chỉ liệt kê kết quả BẤT THƯỜNG và kết quả lệch nhiều so với lần trước.\n");
        } else {
            sb.append("WAITING FOR VALIDATION: ").append(cases.size())
                    .append(" cases. Each case lists only ABNORMAL results and large changes from the previous one.\n");
        }
        for (JsonNode c : cases) {
            String label = (lang.vi() ? "Ca " : "Case ") + c.path("label").asInt(labels.size() + 1);
            labels.add(Map.of("label", label, "index", String.valueOf(labels.size())));
            List<JsonNode> results = list(c.path("results"));
            List<JsonNode> previous = list(c.path("previous"));
            facts.addAll(kidneyFacts(label, results, List.of(), lang));
            Long ago = c.path("receivedDaysAgo").isNumber() ? c.path("receivedDaysAgo").asLong() : null;
            String when = relative(ago, lang);
            sb.append("\n").append(label).append(": ").append(sexAge(c.path("patient"), lang));
            if (lang.vi()) {
                sb.append("; nhận mẫu ").append(when.replace("cùng ngày", "hôm nay")).append("; ")
                        .append(results.size()).append(" kết quả");
            } else {
                sb.append("; received ").append(when.replace("same day", "today")).append("; ").append(results.size())
                        .append(" results");
            }
            List<String> lines = new ArrayList<>();
            int pending = 0;
            for (JsonNode r : results) {
                keys.add(text(r, "code"));
                keys.add(text(r, "name"));
                if (!text(r, "loinc").isBlank()) {
                    keys.add(text(r, "loinc"));
                }
                if ("TechnicalAcceptance".equals(text(r, "status"))) {
                    pending++;
                }
                boolean flagged = !flag(r).isEmpty();
                String delta = delta(r, previous, lang);
                if (flagged || !delta.isEmpty()) {
                    lines.add(resultLine(r, lang) + delta);
                }
                if (flagged) {
                    abnormalTotal++;
                }
            }
            sb.append(pending > 0 ? " (" + pending + (lang.vi() ? " chờ duyệt)" : " awaiting validation)") : "")
                    .append(".\n");
            if (lines.isEmpty()) {
                sb.append(lang.vi() ? "  Tất cả trong khoảng tham chiếu, không lệch nhiều so với lần trước.\n"
                        : "  All within reference ranges, no large change from the previous result.\n");
            } else {
                lines.forEach(l -> sb.append("  ").append(l).append('\n'));
            }
        }
        sb.append(Kdigo.factsBlock(facts, lang));
        String summary = cases.size() + (lang.vi() ? " ca · " : " cases · ") + abnormalTotal
                + (lang.vi() ? " kết quả bất thường" : " abnormal results");
        List<Double> egfrNow = facts.stream().filter(f -> "egfr".equals(f.kind())).map(Kdigo.Fact::value).toList();
        return built(VALIDATION, sb.toString(), egfrNow, keys, List.of(), labels, summary, "page:validation", "page",
                facts, user);
    }

    /** " | lần trước 61.2 (3 tháng trước), thay đổi −9%" when it changed by ≥ 20% or changed flag. */
    static String delta(JsonNode r, List<JsonNode> previous, Lang lang) {
        Double now = Values.number(text(r, "value"));
        for (JsonNode p : previous) {
            if (!sameTest(r, p)) {
                continue;
            }
            Double before = Values.number(text(p, "value"));
            if (now == null || before == null || before == 0) {
                if (now == null && before == null && !text(p, "value").equalsIgnoreCase(text(r, "value"))) {
                    return lang.vi() ? " | lần trước: " + text(p, "value") + " (đổi kết quả)"
                            : " | previous: " + text(p, "value") + " (result changed)";
                }
                return "";
            }
            double change = (now - before) / Math.abs(before) * 100;
            String when = relative(days(p, "daysBefore"), lang);
            boolean flagChanged = !flag(r).equals(flag(p));
            if (Math.abs(change) >= 20 || flagChanged) {
                return (lang.vi() ? " | lần trước " : " | previous ") + text(p, "value") + " (" + when + ")"
                        + (lang.vi() ? ", thay đổi " : ", change ") + (change >= 0 ? "+" : "−")
                        + Math.round(Math.abs(change)) + "%";
            }
            return "";
        }
        return "";
    }

    private static boolean sameTest(JsonNode a, JsonNode b) {
        String ca = text(a, "code");
        String cb = text(b, "code");
        if (!ca.isBlank() && !cb.isBlank()) {
            return ca.equals(cb);
        }
        return text(a, "name").equals(text(b, "name"));
    }

    // ------------------------------------------------------------------ lines

    static String screenLine(Lang lang, String pageName) {
        return screenLine(lang, pageName, OTHER);
    }

    private static String screenLine(Lang lang, String pageName, String page) {
        String title = switch (page) {
        case LOOKUP -> lang.vi() ? "Tra cứu kết quả bệnh nhân" : "Patient results";
        case VALIDATION -> lang.vi() ? "Duyệt kết quả" : "Result validation";
        default -> pageName == null ? (lang.vi() ? "(không rõ tiêu đề)" : "(unknown screen)") : pageName;
        };
        return (lang.vi() ? "MÀN HÌNH: " : "SCREEN: ") + title + "\n";
    }

    /** "- AST (SGOT): 40.61 u/L (tham chiếu 10–40) → CAO". */
    static String resultLine(JsonNode r, Lang lang) {
        String v = text(r, "value").trim();
        boolean numeric = Values.number(v) != null;
        if (!numeric) {
            v = masks(v, lang);
            if (v.length() > TEXT_RESULT_MAX) {
                v = v.substring(0, TEXT_RESULT_MAX) + "…";
            }
        }
        StringBuilder sb = new StringBuilder("- ").append(masks(text(r, "name"), lang)).append(": ");
        sb.append(v.isEmpty() ? (lang.vi() ? "(chưa có kết quả)" : "(no result yet)") : v);
        String unit = text(r, "unit");
        if (!unit.isBlank() && !v.isEmpty()) {
            sb.append(' ').append(masks(unit.trim(), lang));
        }
        String range = Values.range(dbl(r, "low"), dbl(r, "high"));
        if (!range.isEmpty()) {
            sb.append(lang.vi() ? " (tham chiếu " : " (reference ").append(range).append(')');
        }
        String f = Values.flag(v, dbl(r, "low"), dbl(r, "high"));
        if (!f.isEmpty()) {
            sb.append(" → ").append(lang.vi() ? ("H".equals(f) ? "CAO" : "THẤP") : ("H".equals(f) ? "HIGH" : "LOW"));
        }
        return sb.toString();
    }

    /** "Nữ, 69 tuổi" / "Female, 69 years". */
    static String sexAge(JsonNode p, Lang lang) {
        String sex = text(p, "sex");
        String s = lang.vi() ? ("M".equals(sex) ? "Nam" : "F".equals(sex) ? "Nữ" : "Không rõ giới")
                : ("M".equals(sex) ? "Male" : "F".equals(sex) ? "Female" : "Sex unknown");
        if (p.path("ageCapped").asBoolean(false)) {
            return s + (lang.vi() ? ", ≥ 90 tuổi" : ", 90 years or older");
        }
        if (p.path("ageYears").isNumber()) {
            return s + ", " + p.path("ageYears").asInt() + (lang.vi() ? " tuổi" : " years");
        }
        if (p.path("ageMonths").isNumber()) {
            return s + ", " + p.path("ageMonths").asInt() + (lang.vi() ? " tháng tuổi" : " months");
        }
        return s + (lang.vi() ? ", không rõ tuổi" : ", age unknown");
    }

    /** Days → "cùng ngày", "5 ngày trước", "3 tháng trước"… / "same day", "5 days ago"… */
    static String relative(Long days, Lang lang) {
        if (days == null) {
            return lang.vi() ? "không rõ thời điểm" : "unknown time";
        }
        if (days < 0) {
            String r = relative(-days, lang);
            return lang.vi() ? "sau đó " + r.replace(" trước", "") : r.replace(" ago", " later");
        }
        if (days == 0) {
            return lang.vi() ? "cùng ngày" : "same day";
        }
        long n;
        String unit;
        if (days < 14) {
            n = days;
            unit = lang.vi() ? "ngày" : "day";
        } else if (days < 60) {
            n = Math.round(days / 7.0);
            unit = lang.vi() ? "tuần" : "week";
        } else if (days < 730) {
            n = Math.round(days / 30.4);
            unit = lang.vi() ? "tháng" : "month";
        } else {
            n = Math.round(days / 365.25);
            unit = lang.vi() ? "năm" : "year";
        }
        return lang.vi() ? n + " " + unit + " trước" : n + " " + unit + (n == 1 ? "" : "s") + " ago";
    }

    /** OpenELIS section names (English) in Vietnamese, for readability. */
    static String section(String s, Lang lang) {
        String m = masks(s, lang);
        if (!lang.vi()) {
            return m;
        }
        return switch (m.toLowerCase(Locale.ROOT)) {
        case "biochemistry" -> "Sinh hoá";
        case "hematology" -> "Huyết học";
        case "serology", "serology-immunology", "immunology" -> "Miễn dịch – huyết thanh";
        case "microbiology", "bacteriology" -> "Vi sinh";
        case "parasitology" -> "Ký sinh trùng";
        case "virology" -> "Virus";
        default -> m;
        };
    }

    /** OpenELIS writes "[removed]" and "[date]" for what it took out; the Vietnamese rules call them otherwise. */
    static String masks(String s, Lang lang) {
        return lang.vi() ? s.replace("[removed]", "[đã ẩn]").replace("[date]", "[ngày]") : s;
    }

    // ------------------------------------------------------------------ facts

    static boolean isEgfr(String code, String name, String loinc) {
        if (loinc != null && LOINC_EGFR.contains(loinc)) {
            return true;
        }
        String s = (code + " " + name).toLowerCase(Locale.ROOT);
        return s.contains("egfr") || s.contains("gfr") || s.contains("lọc cầu thận") || s.contains("glomerular");
    }

    static boolean isAcr(String code, String name, String loinc) {
        if (loinc != null && LOINC_ACR.contains(loinc)) {
            return true;
        }
        String c = code == null ? "" : code.trim();
        String s = (c + " " + (name == null ? "" : name)).toLowerCase(Locale.ROOT);
        return c.equalsIgnoreCase("ACR") || s.contains("alb/cre") || s.contains("albumin/creatinin")
                || s.matches("(?s).*(?<![a-z])acr(?![a-z]).*");
    }

    /** KDIGO facts: the current order first, then earlier results. */
    static List<Kdigo.Fact> kidneyFacts(String label, List<JsonNode> results, List<JsonNode> previous, Lang lang) {
        List<Kdigo.Fact> out = new ArrayList<>();
        for (JsonNode r : results) {
            Kdigo.Fact f = fact(label, r, null, lang);
            if (f != null) {
                out.add(f);
            }
        }
        for (JsonNode p : previous) {
            String when = relative(days(p, "daysBefore"), lang);
            Kdigo.Fact f = fact(label, p, when, lang);
            if (f != null) {
                out.add(f);
            }
        }
        return out;
    }

    private static Kdigo.Fact fact(String label, JsonNode r, String when, Lang lang) {
        String code = text(r, "code");
        String name = text(r, "name");
        String loinc = text(r, "loinc");
        if (isEgfr(code, name, loinc)) {
            return Kdigo.egfrFact(lang, label, text(r, "value"), when);
        }
        if (isAcr(code, name, loinc)) {
            return Kdigo.acrFact(label, text(r, "value"), text(r, "unit"), when);
        }
        return null;
    }

    /** eGFR values: current first, then earlier. */
    static List<Double> egfr(List<JsonNode> results, List<JsonNode> previous) {
        List<Double> out = new ArrayList<>();
        for (List<JsonNode> list : List.of(results, previous)) {
            for (JsonNode r : list) {
                Double v = Values.number(text(r, "value"));
                if (v != null && isEgfr(text(r, "code"), text(r, "name"), text(r, "loinc"))) {
                    out.add(v);
                }
            }
        }
        return out;
    }

    private static List<String> testKeys(List<JsonNode> results) {
        List<String> keys = new ArrayList<>();
        for (JsonNode r : results) {
            keys.add(text(r, "code"));
            keys.add(text(r, "name"));
            if (!text(r, "loinc").isBlank()) {
                keys.add(text(r, "loinc"));
            }
        }
        return keys;
    }

    private static List<Map<String, String>> abnormal(List<JsonNode> results, Lang lang) {
        List<Map<String, String>> out = new ArrayList<>();
        for (JsonNode r : results) {
            String f = flag(r);
            if (!f.isEmpty()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("test", masks(text(r, "name"), lang));
                m.put("value", text(r, "value").trim());
                m.put("unit", text(r, "unit"));
                m.put("flag", f);
                m.put("egfr", isEgfr(text(r, "code"), text(r, "name"), text(r, "loinc")) ? "1" : "0");
                out.add(m);
            }
        }
        return out;
    }

    private static String flag(JsonNode r) {
        return Values.flag(text(r, "value"), dbl(r, "low"), dbl(r, "high"));
    }

    // ------------------------------------------------------------------ helpers

    private static Built built(String page, String text, List<Double> egfr, List<String> keys,
            List<Map<String, String>> abnormal, List<Map<String, String>> labels, String summary, String subjectKey,
            String subjectKind, List<Kdigo.Fact> facts, String user) {
        return new Built(page, text, sha256(text), egfr, keys, abnormal, labels, summary, subjectKey, subjectKind,
                facts, user);
    }

    private static List<JsonNode> list(JsonNode array) {
        List<JsonNode> out = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(out::add);
        }
        return out;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n == null ? null : n.get(field);
        return v == null || v.isNull() ? "" : v.asText();
    }

    private static Double dbl(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || !v.isNumber() ? null : v.asDouble();
    }

    private static Long days(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || !v.isNumber() ? null : v.asLong();
    }

    public static String sha256(String s) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
