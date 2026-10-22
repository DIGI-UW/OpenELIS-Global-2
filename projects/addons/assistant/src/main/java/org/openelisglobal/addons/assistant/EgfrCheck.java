package org.openelisglobal.addons.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Post-answer check of KDIGO chronic kidney disease staging (G by eGFR, A by
 * ACR). The RIGHT stage always comes from {@link Kdigo} (computed by the
 * service), never from the model.
 *
 * <p>
 * Two kinds of error are caught:
 * <ol>
 * <li><b>A wrong stage given to a value of the patient</b> ("eGFR 59 → G2",
 * "stage 2 kidney disease") → the whole "stage (range)" is replaced by the right
 * stage and range.</li>
 * <li><b>A wrong range next to a stage</b> ("G3a (60–89)") — also when that
 * stage is only MENTIONED ("close to the G2 (60–89) boundary") → the range is
 * fixed from the table.</li>
 * </ol>
 * A stage after a relation word ("close to", "near", "than", "if"…) is a
 * MENTION of another stage, not the patient's stage: the stage is kept, only
 * its range is checked. (Regression 2026-10-19: an earlier version took "close
 * to the G2 (60–89) boundary" as the stage of eGFR 59 and rewrote it to "G3a
 * (60–89)" — the checker itself produced the wrong sentence.) When the value
 * cannot be identified (several cases, no number, no case label) and the stage
 * matches no case, the sentence is FLAGGED, not rewritten. Every correction or
 * flag adds a warning line at the end of the answer — never a silent rewrite.
 *
 * <p>
 * Patterns and notes exist per {@link Lang}; the Vietnamese ones are those of
 * the assistant's first deployment, unchanged.
 */
public final class EgfrCheck {

    private EgfrCheck() {
    }

    /** Độ KDIGO theo eGFR (mL/ph/1,73 m²) — xem {@link Kdigo#gfrCategory}. */
    public static String stage(double egfr) {
        return Kdigo.gfrCategory(egfr);
    }

    public static String stageRange(String stage) {
        return Kdigo.gfrRange(stage);
    }

    public record Outcome(String text, boolean corrected, List<String> notes) {
    }

    private static final int CI = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    /** "G3a", "G 2": specific enough to check a sentence without a kidney word. */
    private static final Pattern G_TOKEN = Pattern.compile("(?<![\\p{L}\\d])G\\s?[1-5][ab]?(?![\\p{L}\\d])");

    // "A1", "A 2" (not "HbA1c": a letter comes before)
    private static final Pattern A_MENTION = Pattern.compile("(?<![\\p{L}\\d])A\\s?([1-3])(?![\\p{L}\\d])");

    private static final Pattern NUMBER = Pattern.compile("(?<![\\d.,])\\d{1,4}(?:[.,]\\d{1,2})?(?![\\d])");

    /** The words of one language. The Vietnamese set is the original one, unchanged. */
    static final class Words {
        final Lang lang;
        final Pattern kidney;
        final Pattern albumin;
        /** "G3a", "G 2", "stage 3b", "CKD 3a", "stage II"… */
        final Pattern mention;
        /** A range right after a stage: " (45–59)", ": 60–89", " (≥ 90)", " (< 15 mg/g)". */
        final Pattern attached;
        /** Numbers of a listed range "45–59": not a value of the patient. */
        final Pattern numericRange;
        final Pattern negation;
        /** Relation words: the stage after them is MENTIONED, not the patient's. */
        final Pattern reference;
        /** An assigning word right before the stage ("→ G2", "is G2"). */
        final Pattern assign;
        /** A mention that carries its subject: "CKD stage 2". */
        final Pattern subjectPrefix;
        final Pattern patient;
        final Pattern caseLabel;
        final String caseWord;
        /** Range words between two ends of a listed range ("G1–G5"). */
        final String rangeWords;
        final String stageWords;

        Words(Lang lang, String kidney, String albumin, String mention, String aboveBelow, String rangeWords,
                String negation, String reference, String assign, String subjectPrefix, String patient,
                String caseWord, String stageWords) {
            this.lang = lang;
            this.kidney = Pattern.compile(kidney, CI);
            this.albumin = Pattern.compile(albumin, CI);
            this.mention = Pattern.compile(mention, CI);
            this.attached = Pattern.compile("^\\s*(\\(\\s*|:\\s*)?(?:" //
                    + "(≥|>=|>|≤|<=|<|" + aboveBelow + ")\\s*(\\d+(?:[.,]\\d+)?)" //
                    + "|(\\d+(?:[.,]\\d+)?)\\s*(?:[–—-]|" + rangeWords + ")\\s*(\\d+(?:[.,]\\d+)?))" //
                    + "(\\s*(?:" + (lang.vi() ? "mL/ph[^)\\n]*" : "mL/min[^)\\n]*|mL/ph[^)\\n]*")
                    + "|mg/g|mg/mmol))?(\\s*\\))?", CI);
            this.numericRange = Pattern.compile("\\d+(?:[.,]\\d+)?\\s*(?:[–—-]|" + rangeWords
                    + ")\\s*\\d+(?:[.,]\\d+)?|[≥≤<>]=?\\s*\\d+(?:[.,]\\d+)?");
            this.negation = Pattern.compile("(?<![\\p{L}])(" + negation + ")(?![\\p{L}])", CI);
            this.reference = Pattern.compile("(?<![\\p{L}])(" + reference + ")(?![\\p{L}])", CI);
            this.assign = Pattern.compile("(?:→|->|=|:|(?<![\\p{L}])(?:" + assign + "))[\\s*_]*$", CI);
            this.subjectPrefix = Pattern.compile("(?:" + subjectPrefix + ")", CI);
            this.patient = Pattern.compile("(?<![\\p{L}])(?:" + patient + ")(?![\\p{L}])", CI);
            this.caseLabel = Pattern.compile("(?<![\\p{L}])" + caseWord + "\\s*(\\d{1,3})(?![\\d])");
            this.caseWord = caseWord;
            this.rangeWords = rangeWords;
            this.stageWords = stageWords;
        }

        static Words of(Lang lang) {
            return lang == Lang.VI ? VI : EN;
        }

        String correctionMark() {
            return lang.vi() ? "⚠️ Hệ thống đã tự sửa phân độ thận" : "⚠️ The system corrected the kidney staging";
        }

        String noAcr() {
            return lang.vi() ? "Không có ACR trong dữ liệu nên không phân độ albumin niệu (A1–A3) được"
                    : "There is no ACR in the data, so albuminuria (A1–A3) cannot be staged";
        }
    }

    static final Words VI = new Words(Lang.VI,
            "egfr|gfr|mức lọc|độ lọc|lọc cầu thận|cầu thận|ckd|kdigo|bệnh thận|suy thận|chức năng thận|thận mạn|"
                    + "albumin|acr|đạm niệu|protein niệu",
            "albumin|acr|đạm niệu|protein niệu|alb/cre",
            "(?<![\\p{L}\\d])(?:(?:ckd|bệnh thận mạn|suy thận)\\s*(?:giai đoạn|độ|mức|stage|g)?\\s*|"
                    + "(?:giai\\s*đoạn|độ|stage|mức|phân độ|nhóm)\\s*(?:g\\s*)?|g\\s?)"
                    + "(3a|3b|IIIa|IIIb|[1-5]|III|II|IV|V|I)(?![\\p{L}\\d])",
            "trên|dưới", "đến|tới", "không phải|không|chưa|loại trừ|not",
            "ranh giới|giáp|sát|gần|ngưỡng|chuyển|"
                    + "tiến triển|lên|xuống|sang|so với|khác với|nếu|vượt|chạm|hơn|độ trên|độ dưới|lân cận|kế tiếp|tiếp theo|cách|xa|tính lại|giả sử|"
                    + "border|near|boundary|above|below|toward|into|from|than|if",
            "thuộc|là|xếp(?:\\s+(?:loại|vào))?|tương ứng(?:\\s+với)?|nằm ở|nằm trong|ở mức|"
                    + "phân độ|is|falls in|corresponds to",
            "ckd|bệnh thận mạn|suy thận",
            "bệnh nhân|người bệnh|BN|kết quả này|kết quả hiện tại|giá trị này|giá trị hiện tại|"
                    + "hiện tại|lần này|phiếu này|ca này|patient|this egfr|current",
            "Ca", "độ\\s*|giai đoạn\\s*");

    static final Words EN = new Words(Lang.EN,
            "egfr|gfr|filtration|kidney|renal|ckd|kdigo|nephropathy|albumin|acr|albuminuria|proteinuria",
            "albumin|acr|albuminuria|proteinuria|alb/cre",
            "(?<![\\p{L}\\d])(?:(?:ckd|chronic kidney disease|kidney disease)\\s*(?:stage|category|g)?\\s*|"
                    + "(?:stage|category|grade)\\s*(?:g\\s*)?|g\\s?)"
                    + "(3a|3b|IIIa|IIIb|[1-5]|III|II|IV|V|I)(?![\\p{L}\\d])",
            "above|below|over|under", "to", "not|no|isn't|rule out|ruled out|excluding",
            "border|borderline|near|close to|boundary|threshold|above|below|toward|towards|into|from|than|if|"
                    + "next|previous|lower|higher|adjacent|neighbouring|neighboring|cut-off|cutoff|would be|"
                    + "progress|progression|move|moves|drop|drops|rise|rises|instead of|compared with|versus|vs",
            "is|are|was|in|falls in|falls into|corresponds to|classified as|consistent with|stage|category",
            "ckd|chronic kidney disease|kidney disease",
            "patient|this result|current result|this value|current value|currently|this order|this case|"
                    + "this egfr|current|now",
            "Case", "stage\\s*|category\\s*");

    /** Only a list of eGFR values (the current one first); Vietnamese. */
    public static Outcome check(String answer, List<Double> egfrValues) {
        return check(answer, egfrValues, Lang.VI);
    }

    public static Outcome check(String answer, List<Double> egfrValues, Lang lang) {
        List<Kdigo.Fact> facts = new ArrayList<>();
        if (egfrValues != null) {
            boolean first = true;
            for (Double v : egfrValues) {
                if (v != null) {
                    facts.add(new Kdigo.Fact("egfr", null, Values.num(v), v, lang.egfrUnit, Kdigo.gfrCategory(v),
                            first ? null : lang.vi() ? "trước" : "earlier"));
                    first = false;
                }
            }
        }
        return checkFacts(answer, facts, lang);
    }

    /** Vietnamese. */
    public static Outcome checkFacts(String answer, List<Kdigo.Fact> facts) {
        return checkFacts(answer, facts, Lang.VI);
    }

    /**
     * @param answer the model's answer
     * @param facts  KDIGO facts computed by the service (eGFR/ACR, current order first)
     */
    public static Outcome checkFacts(String answer, List<Kdigo.Fact> facts, Lang lang) {
        Words w = Words.of(lang);
        if (answer == null || answer.isEmpty()) {
            return new Outcome(answer, false, List.of());
        }
        List<Kdigo.Fact> egfr = new ArrayList<>();
        List<Kdigo.Fact> acr = new ArrayList<>();
        for (Kdigo.Fact f : facts == null ? List.<Kdigo.Fact>of() : facts) {
            ("egfr".equals(f.kind()) ? egfr : acr).add(f);
        }
        List<String> notes = new ArrayList<>();
        StringBuilder out = new StringBuilder();
        // Cắt câu: xuống dòng, ". " , ";" — GIỮ dấu chấm thập phân (55.6).
        Matcher sentenceEnd = Pattern.compile("\\n|(?<=[.!?;])\\s+").matcher(answer);
        int start = 0;
        List<int[]> spans = new ArrayList<>();
        while (sentenceEnd.find()) {
            spans.add(new int[] { start, sentenceEnd.end() });
            start = sentenceEnd.end();
        }
        spans.add(new int[] { start, answer.length() });
        for (int[] span : spans) {
            String sentence = answer.substring(span[0], span[1]);
            if (!w.kidney.matcher(sentence).find() && !G_TOKEN.matcher(sentence).find()) {
                out.append(sentence);
                continue;
            }
            String caseLabel = lastCaseLabel(answer.substring(0, span[0]), w);
            List<Edit> edits = new ArrayList<>();
            gMentions(sentence, caseLabel, egfr, edits, notes, w);
            if (w.albumin.matcher(sentence).find()) {
                aMentions(sentence, caseLabel, acr, edits, notes, w);
            }
            edits.sort((a, b) -> Integer.compare(a.start, b.start));
            StringBuilder fixed = new StringBuilder();
            int last = 0;
            for (Edit e : edits) {
                if (e.start < last) {
                    continue; // chồng lấn: giữ sửa trước
                }
                fixed.append(sentence, last, e.start).append(e.replacement);
                last = e.end;
            }
            fixed.append(sentence.substring(last));
            out.append(fixed);
        }
        String text = out.toString();
        boolean flagged = !notes.isEmpty();
        if (flagged) {
            text = text.stripTrailing() + "\n\n" + w.correctionMark() + ": " + String.join("; ", notes) + ".";
        }
        return new Outcome(text, flagged, notes);
    }

    private record Edit(int start, int end, String replacement) {
    }

    /**
     * Khoảng đi kèm: vị trí kết thúc + cận NGUYÊN gồm hai đầu [lo, hi] (±∞ khi để
     * ngỏ); {@code openParen} = mở "(" mà chưa đóng ngay sau khoảng.
     */
    record Attached(int end, double lo, double hi, boolean openParen, String unit) {
        String wrap(String code, String range) {
            return code + " (" + range + (openParen ? "" : ")");
        }
    }

    // ------------------------------------------------------------------ G (eGFR)

    private static void gMentions(String sentence, String caseLabel, List<Kdigo.Fact> facts, List<Edit> edits,
            List<String> notes, Words w) {
        Matcher m = w.mention.matcher(sentence);
        List<int[]> ranges = rangeSpans(sentence, w);
        int prevEnd = 0;
        while (m.find()) {
            int windowFrom = prevEnd;
            prevEnd = m.end();
            String said = normalize(m.group(1));
            if (said == null || inRange(sentence, m, w) || negated(sentence, m.start(), w)) {
                continue;
            }
            Attached att = attached(sentence, m.end(), w);
            Kdigo.Band saidBand = Kdigo.gfrBand(said);
            boolean rangeOk = att == null || matches(att, saidBand);
            ValueRef ref = valueFor(sentence, m.start(), facts, ranges, caseLabel, w);
            Role role = role(sentence, windowFrom, m.start(), w.subjectPrefix.matcher(m.group()).lookingAt(), w.kidney,
                    facts, ranges, w);
            boolean reference = role != Role.CLAIM;
            int end = att == null ? m.end() : att.end();
            prevEnd = end;
            String quoted = sentence.substring(m.start(), end).trim();
            if (role == Role.AMBIGUOUS && att == null && ref != null && ref.fact != null
                    && !consistent(said, ref.fact.category())) {
                addNote(notes, w.lang.vi()
                        ? "\"" + quoted + "\" — máy chủ tính eGFR " + Kdigo.vn(ref.fact.shown()) + " thuộc "
                                + ref.fact.category() + " (" + Kdigo.gfrRange(ref.fact.category())
                                + "), cần đối chiếu câu này"
                        : "\"" + quoted + "\" — the server stages eGFR " + ref.fact.shown() + " as "
                                + ref.fact.category() + " (" + Kdigo.gfrRange(ref.fact.category())
                                + "), check this sentence");
            }
            if (!reference && ref != null && ref.fact != null && !consistent(said, ref.fact.category())) {
                Kdigo.Fact f = ref.fact;
                String right = f.category();
                edits.add(new Edit(m.start(), end, att == null ? right : att.wrap(right, Kdigo.gfrRange(right))));
                addNote(notes, w.lang.vi()
                        ? (f.label() == null ? "" : f.label() + ": ") + "eGFR " + Kdigo.vn(f.shown()) + " thuộc "
                                + right + " (" + Kdigo.gfrRange(right) + " mL/ph/1,73 m², KDIGO), không phải \"" + quoted
                                + "\""
                        : (f.label() == null ? "" : f.label() + ": ") + "eGFR " + f.shown() + " is " + right + " ("
                                + Kdigo.gfrRange(right) + " " + w.lang.egfrUnit + ", KDIGO), not \"" + quoted + "\"");
                continue;
            }
            if (!reference && ref != null && ref.fact == null && !ref.candidates.isEmpty()
                    && ref.candidates.stream().noneMatch(f -> consistent(said, f.category()))) {
                addNote(notes, w.lang.vi()
                        ? "\"" + quoted + "\" không khớp phân độ máy chủ tính cho dữ liệu đang xem ("
                                + describe(ref.candidates, w) + ") — cần đối chiếu"
                        : "\"" + quoted + "\" does not match the server's staging of the data on screen ("
                                + describe(ref.candidates, w) + ") — check it");
            }
            if (!rangeOk) {
                String fixedRange = saidBand.range();
                if (!att.openParen()) { // ngoặc chưa đóng = câu giải thích dài: chỉ gắn cờ, không cắt ghép
                    edits.add(new Edit(m.start(), end, att.wrap(said, fixedRange)));
                }
                addNote(notes, w.lang.vi()
                        ? said + " là " + fixedRange + " mL/ph/1,73 m² theo KDIGO, không phải \"" + quoted + "\""
                        : said + " is " + fixedRange + " " + w.lang.egfrUnit + " per KDIGO, not \"" + quoted + "\"");
            }
        }
    }

    // ------------------------------------------------------------------ A (albumin niệu)

    private static void aMentions(String sentence, String caseLabel, List<Kdigo.Fact> facts, List<Edit> edits,
            List<String> notes, Words w) {
        Matcher m = A_MENTION.matcher(sentence);
        List<int[]> ranges = rangeSpans(sentence, w);
        int prevEnd = 0;
        while (m.find()) {
            int windowFrom = prevEnd;
            prevEnd = m.end();
            String said = "A" + m.group(1);
            if (inRange(sentence, m, w) || negated(sentence, m.start(), w)) {
                continue;
            }
            Attached att = attached(sentence, m.end(), w);
            ValueRef ref = valueFor(sentence, m.start(), facts, ranges, caseLabel, w);
            boolean reference = role(sentence, windowFrom, m.start(), false, w.albumin, facts, ranges, w) != Role.CLAIM;
            int end = att == null ? m.end() : att.end();
            prevEnd = end;
            String quoted = sentence.substring(m.start(), end).trim();
            String unit = ref != null && ref.fact != null ? ref.fact.unit()
                    : facts.isEmpty() ? null : facts.get(0).unit();
            if (facts.isEmpty()) {
                // "A1 < 30, A2 30–300, A3 > 300" is the table being quoted, not a stage given to the patient
                if (!reference && !(att != null && listsSeveralStages(sentence))) {
                    addNote(notes, w.noAcr() + (w.lang.vi() ? " (câu trả lời ghi \"" : " (the answer says \"") + quoted
                            + "\")");
                }
            } else if (!reference && ref != null && ref.fact != null && !said.equals(ref.fact.category())) {
                Kdigo.Fact f = ref.fact;
                Kdigo.Band b = Kdigo.albBand(f.category(), f.unit());
                edits.add(new Edit(m.start(), end, att == null ? f.category() : att.wrap(f.category(), b.range())));
                addNote(notes, w.lang.vi()
                        ? (f.label() == null ? "" : f.label() + ": ") + "ACR " + Kdigo.vn(f.shown()) + " " + f.unit()
                                + " thuộc " + f.category() + " (" + b.range() + ", KDIGO), không phải \"" + quoted + "\""
                        : (f.label() == null ? "" : f.label() + ": ") + "ACR " + f.shown() + " " + f.unit() + " is "
                                + f.category() + " (" + b.range() + ", KDIGO), not \"" + quoted + "\"");
                continue;
            } else if (!reference && ref != null && ref.fact == null && !ref.candidates.isEmpty()
                    && ref.candidates.stream().noneMatch(f -> said.equals(f.category()))) {
                addNote(notes, w.lang.vi()
                        ? "\"" + quoted + "\" không khớp phân độ máy chủ tính (" + describe(ref.candidates, w)
                                + ") — cần đối chiếu"
                        : "\"" + quoted + "\" does not match the server's staging (" + describe(ref.candidates, w)
                                + ") — check it");
            }
            if (att != null) {
                // khoảng đúng theo một trong hai đơn vị là chấp nhận
                Kdigo.Band g = "mg/mmol".equals(att.unit()) ? null : Kdigo.albBand(said, "mg/g");
                Kdigo.Band mm = "mg/g".equals(att.unit()) ? null : Kdigo.albBand(said, "mg/mmol");
                if (!matches(att, g) && !matches(att, mm)) {
                    Kdigo.Band b = Kdigo.albBand(said, att.unit() != null ? att.unit() : unit == null ? "mg/g" : unit);
                    edits.add(new Edit(m.start(), end, att.wrap(said, b.range())));
                    addNote(notes, w.lang.vi() ? said + " là " + b.range() + " theo KDIGO, không phải \"" + quoted + "\""
                            : said + " is " + b.range() + " per KDIGO, not \"" + quoted + "\"");
                }
            }
        }
    }

    // ------------------------------------------------------------------ gán giá trị

    /** Giá trị một lần nêu độ nói tới: {@code fact} xác định, hoặc các ứng viên khi không xác định được. */
    record ValueRef(Kdigo.Fact fact, int numberEnd, List<Kdigo.Fact> candidates) {
    }

    /**
     * Số trùng giá trị gần nhất ĐỨNG TRƯỚC (bỏ số nằm trong liệt kê khoảng); không
     * có thì số đầu tiên đứng sau; rồi nhãn "Ca N" (trong câu, hoặc gần nhất phía
     * trên); rồi giá trị phiếu đang xem nếu chỉ có MỘT. Không xác định được → ứng
     * viên = mọi giá trị phiếu đang xem. Không có dữ kiện → null.
     */
    static ValueRef valueFor(String sentence, int mentionStart, List<Kdigo.Fact> facts, List<int[]> ranges,
            String caseLabelAbove, Words w) {
        if (facts == null || facts.isEmpty()) {
            return null;
        }
        Kdigo.Fact before = null;
        int beforeEnd = -1;
        Kdigo.Fact after = null;
        Matcher n = NUMBER.matcher(sentence);
        while (n.find()) {
            if (inside(n.start(), ranges) || isCaseNumber(sentence, n.start(), w)) {
                continue;
            }
            Kdigo.Fact match = null;
            double d;
            try {
                d = Double.parseDouble(n.group().replace(',', '.'));
            } catch (NumberFormatException e) {
                continue;
            }
            String label = labelBefore(sentence, n.start(), caseLabelAbove, w);
            for (Kdigo.Fact f : facts) {
                if (Math.abs(f.value() - d) < 0.051 && (label == null || f.label() == null || label.equals(f.label()))) {
                    match = f;
                    break;
                }
            }
            if (match == null) {
                continue;
            }
            if (n.start() < mentionStart) {
                before = match;
                beforeEnd = n.end();
            } else if (after == null) {
                after = match;
            }
        }
        if (before != null) {
            return new ValueRef(before, beforeEnd, List.of());
        }
        if (after != null) {
            return new ValueRef(after, -1, List.of());
        }
        String label = labelBefore(sentence, mentionStart, caseLabelAbove, w);
        List<Kdigo.Fact> current = facts.stream().filter(Kdigo.Fact::current).toList();
        if (current.isEmpty()) {
            current = facts;
        }
        if (label != null) {
            List<Kdigo.Fact> mine = current.stream().filter(f -> label.equals(f.label())).toList();
            if (mine.size() == 1) {
                return new ValueRef(mine.get(0), -1, List.of());
            }
        }
        if (current.size() == 1) {
            return new ValueRef(current.get(0), -1, List.of());
        }
        return new ValueRef(null, -1, current);
    }

    /** Vai của một lần nêu độ trong câu. */
    enum Role {
        /** phân độ CỦA giá trị bệnh nhân → kiểm độ + khoảng, sai thì sửa */
        CLAIM,
        /** nhắc tới một độ khác ("sát ranh giới G2") → chỉ kiểm khoảng */
        REFERENCE,
        /** có cả từ quan hệ lẫn từ gán ("ngưỡng kế tiếp là G2") → không sửa độ; độ lệch mà không kèm khoảng thì gắn cờ */
        AMBIGUOUS
    }

    /**
     * Một lần nêu độ là PHÂN ĐỘ của bệnh nhân chỉ khi nó gắn với một CHỦ THỂ đứng
     * trước nó trong câu, sau lần nêu độ trước đó: giá trị eGFR/ACR của bệnh nhân,
     * từ chỉ thận/albumin ("eGFR", "chức năng thận"…), từ chỉ bệnh nhân ("bệnh
     * nhân", "kết quả này"…), hoặc chính cụm "suy thận độ …". Không có chủ thể
     * ("Độ dưới: G3a (45–59)", "…, sát ranh giới G2 (60–89)" sau khi eGFR đã được
     * phân độ) → NHẮC TỚI. Có chủ thể mà giữa chủ thể và độ có từ quan hệ → NHẮC
     * TỚI (thêm từ gán ngay trước độ → MƠ HỒ); không có → PHÂN ĐỘ.
     */
    static Role role(String sentence, int windowFrom, int mentionStart, boolean mentionIsSubject, Pattern subjectWords,
            List<Kdigo.Fact> facts, List<int[]> ranges, Words w) {
        if (mentionIsSubject) {
            return Role.CLAIM;
        }
        int from = Math.min(windowFrom, mentionStart);
        String window = sentence.substring(from, mentionStart);
        int cueEnd = -1;
        for (Pattern p : List.of(subjectWords, w.patient)) {
            Matcher c = p.matcher(window);
            while (c.find()) {
                cueEnd = Math.max(cueEnd, c.end());
            }
        }
        Matcher n = NUMBER.matcher(window);
        while (n.find()) {
            if (inside(from + n.start(), ranges)) {
                continue;
            }
            double d = num(n.group());
            for (Kdigo.Fact f : facts == null ? List.<Kdigo.Fact>of() : facts) {
                if (Math.abs(f.value() - d) < 0.051) {
                    cueEnd = Math.max(cueEnd, n.end());
                }
            }
        }
        if (cueEnd < 0) {
            return Role.REFERENCE;
        }
        // khoảng của độ trước ("G3a (45–59), …") không phải từ quan hệ
        String between = w.numericRange.matcher(window.substring(cueEnd)).replaceAll(" ");
        if (!w.reference.matcher(between).find()) {
            return Role.CLAIM;
        }
        boolean assigned = w.assign.matcher(sentence.substring(Math.max(from, mentionStart - 30), mentionStart)).find();
        return assigned ? Role.AMBIGUOUS : Role.REFERENCE;
    }

    private static String labelBefore(String sentence, int pos, String above, Words w) {
        Matcher c = w.caseLabel.matcher(sentence.substring(0, pos));
        String label = above;
        while (c.find()) {
            label = w.caseWord + " " + c.group(1);
        }
        return label;
    }

    private static String lastCaseLabel(String text, Words w) {
        Matcher c = w.caseLabel.matcher(text);
        String label = null;
        while (c.find()) {
            label = w.caseWord + " " + c.group(1);
        }
        return label;
    }

    private static boolean isCaseNumber(String sentence, int numberStart, Words w) {
        String before = sentence.substring(Math.max(0, numberStart - w.caseWord.length() - 2), numberStart);
        return before.matches("(?s).*(?<![\\p{L}])" + w.caseWord + "\\s*$");
    }

    private static List<int[]> rangeSpans(String sentence, Words w) {
        List<int[]> out = new ArrayList<>();
        Matcher r = w.numericRange.matcher(sentence);
        while (r.find()) {
            out.add(new int[] { r.start(), r.end() });
        }
        return out;
    }

    private static boolean inside(int pos, List<int[]> spans) {
        for (int[] s : spans) {
            if (pos >= s[0] && pos < s[1]) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ khoảng đi kèm

    static Attached attached(String sentence, int from, Words w) {
        Matcher a = w.attached.matcher(sentence.substring(from));
        if (!a.find()) {
            return null;
        }
        boolean paren = a.group(1) != null && a.group(1).startsWith("(");
        double lo;
        double hi;
        if (a.group(2) != null) {
            String op = a.group(2).toLowerCase(Locale.ROOT);
            double v = num(a.group(3));
            switch (op) {
            case "≥", ">=", "trên", "above", "over" -> {
                lo = Math.ceil(v);
                hi = Double.POSITIVE_INFINITY;
            }
            case ">" -> {
                lo = Math.floor(v) + 1;
                hi = Double.POSITIVE_INFINITY;
            }
            case "<", "dưới", "below", "under" -> {
                lo = Double.NEGATIVE_INFINITY;
                hi = Math.ceil(v) - 1;
            }
            default -> { // ≤ <=
                lo = Double.NEGATIVE_INFINITY;
                hi = Math.floor(v);
            }
            }
        } else {
            lo = Math.ceil(num(a.group(4)));
            hi = Math.floor(num(a.group(5)));
        }
        int end = from + a.end();
        boolean closed = a.group(7) != null;
        if (!paren && closed) {
            end -= a.group(7).length(); // ")" của ngoặc bên ngoài, không thuộc cụm này
        }
        String unit = a.group(6) == null ? null : Kdigo.acrUnit(a.group(6).trim());
        return new Attached(end, lo, hi, paren && !closed, unit);
    }

    /**
     * Khoảng đi kèm có đúng với độ đó không (so cận nguyên; cận dưới 0 ≡ −∞).
     * Một ngưỡng một phía ("G2 (≥ 60 …)", "G3a (< 60)") đúng nếu trùng cận tương
     * ứng của độ.
     */
    static boolean matches(Attached att, Kdigo.Band band) {
        if (band == null) {
            return false;
        }
        double lo = att.lo() <= 0 ? Double.NEGATIVE_INFINITY : att.lo();
        double bandLo = Double.isInfinite(band.lo()) ? band.lo() : Math.ceil(band.lo());
        double bandHi = Double.isInfinite(band.hiExcl()) ? band.hiExcl() : Math.ceil(band.hiExcl()) - 1;
        if (Double.isInfinite(att.hi()) && !Double.isInfinite(lo)) {
            return same(lo, bandLo);
        }
        if (Double.isInfinite(lo) && !Double.isInfinite(att.hi())) {
            return same(att.hi(), bandHi);
        }
        return same(lo, bandLo) && same(att.hi(), bandHi);
    }

    private static boolean same(double a, double b) {
        return a == b || Math.abs(a - b) < 1e-6;
    }

    private static double num(String s) {
        return Double.parseDouble(s.replace(',', '.'));
    }

    // ------------------------------------------------------------------ tiện ích

    private static void addNote(List<String> notes, String note) {
        if (!notes.contains(note)) {
            notes.add(note);
        }
    }

    private static String describe(List<Kdigo.Fact> facts, Words w) {
        List<String> parts = new ArrayList<>();
        for (Kdigo.Fact f : facts) {
            parts.add((f.label() == null ? "" : f.label() + " ") + ("egfr".equals(f.kind()) ? "eGFR " : "ACR ")
                    + w.lang.number(f.shown()) + " → " + f.category());
        }
        return String.join(", ", parts);
    }

    /** Two or more different A stages in the sentence, each followed by its range: a quoted table. */
    static boolean listsSeveralStages(String sentence) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        Matcher m = A_MENTION.matcher(sentence);
        while (m.find()) {
            String rest = sentence.substring(m.end()).stripLeading();
            if (rest.matches("(?s)^[(:]?\\s*(?:[<>≥≤]|\\d).*")) {
                seen.add(m.group(1));
            }
        }
        return seen.size() >= 2;
    }

    /** Chuẩn hoá cách gọi độ → "G1".."G5"/"G3a"/"G3b"/"G3". */
    static String normalize(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        return switch (s) {
        case "1", "i" -> "G1";
        case "2", "ii" -> "G2";
        case "3", "iii" -> "G3";
        case "3a", "iiia" -> "G3a";
        case "3b", "iiib" -> "G3b";
        case "4", "iv" -> "G4";
        case "5", "v" -> "G5";
        default -> null;
        };
    }

    static boolean consistent(String said, String right) {
        return said.equals(right) || ("G3".equals(said) && right != null && right.startsWith("G3"));
    }

    /** Một đầu của liệt kê khoảng "G1–G5", "độ 1 đến 5", "G3a-G3b", "A1–A3". */
    private static boolean inRange(String sentence, Matcher m, Words w) {
        String after = sentence.substring(m.end());
        String before = sentence.substring(0, m.start());
        return after.matches("(?s)^\\s*(?:[–—-]|" + w.rangeWords + "|->|→)\\s*(?:G\\s?|A\\s?|" + w.stageWords
                + ")?(?:[1-5]|I|V).*")
                || before.matches("(?s).*(?:[1-5][ab]?|I|V)\\s*(?:[–—-]|" + w.rangeWords + ")\\s*$") // not a bullet
                || after.matches("(?s)^\\s*/\\s*(?:G\\s?|A\\s?)?[1-5].*") || before.matches("(?s).*\\d[ab]?\\s*/\\s*$");
    }

    /** Có từ phủ định trong 25 ký tự trước mention (cùng câu). */
    private static boolean negated(String sentence, int mentionStart, Words w) {
        int from = Math.max(0, mentionStart - 25);
        return w.negation.matcher(sentence.substring(from, mentionStart)).find();
    }

    static String fmt(double d) {
        String s = d == Math.rint(d) ? String.valueOf((long) d) : String.format(Locale.ROOT, "%.2f", d);
        s = s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
        return s.replace('.', ',');
    }
}
