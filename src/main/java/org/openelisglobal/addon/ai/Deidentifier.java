package org.openelisglobal.addon.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes identifiers from free text before it leaves OpenELIS for an add-on
 * with the {@code ai.context} scope: the known identifiers of the patients in
 * context (names and their parts, national and other ids, phone numbers,
 * address, e-mail, lab numbers), then generic patterns (lab-number-like codes,
 * phone numbers, e-mail addresses, absolute dates, long digit runs).
 *
 * <p>
 * Multi-word identifiers (a full name, an address) and identifiers with digits
 * are matched ignoring case and accents, so "nguyen thi lan" is removed for
 * "Nguyễn Thị Lan". A single name part ("Lan") is matched ignoring case only:
 * without accents it would also match ordinary words ("lần"). Matches are whole
 * words only. Result values such as {@code 176.36} are never touched.
 */
public final class Deidentifier {

    public static final String MASK = "[removed]";
    public static final String DATE_MASK = "[date]";

    // lab numbers like DEV01260000000000063, codes like BN-12345
    private static final Pattern ACCESSION = Pattern.compile("\\b[A-Za-z]{2,}[-_/]?\\d{5,}\\b");
    // Vietnamese 0xxxxxxxxx / +84xxxxxxxxx, any +<country> number, 0xx.xxx.xxxx
    private static final Pattern PHONE = Pattern.compile("(?<![\\d.,])(?:\\+?84|0)(?:[ -]?\\d){8,10}(?![\\d.,]*\\d)"
            + "|(?<![\\d.,])\\+\\d(?:[ -]?\\d){7,13}(?![\\d.,]*\\d)"
            + "|(?<![\\d.,])0\\d{2,3}\\.\\d{3}\\.\\d{3,4}(?![\\d.,]*\\d)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final String MONTHS = "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?"
            + "|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?";
    // 01/02/1957, 1-2-57, 1957-01-02, 02.01.1957, "ngày 2 tháng 1 năm 1957", "2
    // January 1957", "January 2, 1957"
    private static final Pattern DATE = Pattern.compile(
            "\\b(?:\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}|\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2})\\b"
                    + "|\\bngày\\s+\\d{1,2}\\s+tháng\\s+\\d{1,2}(?:\\s+năm\\s+\\d{2,4})?" + "|\\b\\d{1,2}\\s+(?:"
                    + MONTHS + ")\\.?\\s+\\d{4}\\b" + "|\\b(?:" + MONTHS + ")\\.?\\s+\\d{1,2},?\\s+\\d{4}\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    // digit runs of 7+ without a decimal point (national ids, record numbers)
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<![\\d.,])\\d{7,}(?![\\d.,]*\\d)");

    /**
     * Words that are not identifiers on their own even when they are part of a name
     * or an address (Vietnamese middle names, address words, words of test names),
     * compared without accents.
     */
    private static final Set<String> COMMON = Set.of("thi", "van", "phuong", "xa", "huyen", "tinh", "duong", "quan",
            "thanh", "pho", "ap", "khom", "to", "so", "nha", "ngo", "hem", "dong", "thap", "can", "tho", "viet", "nam",
            "test", "cao", "duc", "dinh", "luong", "mau", "nuoc", "tieu", "anti", "glucose", "protein", "total",
            "street", "road", "city", "district", "ward", "the", "and");

    private final List<String> known;

    /**
     * @param identifiers known identifiers of the patients in context; may be empty
     */
    public Deidentifier(Collection<String> identifiers) {
        Set<String> set = new LinkedHashSet<>();
        if (identifiers != null) {
            for (String raw : identifiers) {
                if (raw == null) {
                    continue;
                }
                String s = raw.trim();
                if (s.length() >= 3) {
                    set.add(s);
                }
                for (String part : s.split("[\\s,]+")) {
                    if (part.length() >= 3 && !part.chars().allMatch(Character::isDigit)
                            && !COMMON.contains(fold(part))) {
                        set.add(part);
                    }
                }
            }
        }
        List<String> list = new ArrayList<>(set);
        list.sort(Comparator.comparingInt(String::length).reversed()); // "Nguyễn Thị Lan" before "Lan"
        this.known = list;
    }

    /** The text without known identifiers and identifier-like patterns. */
    public String scrub(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String out = text;
        for (String id : known) {
            out = replace(out, id, isPhrase(id));
        }
        out = EMAIL.matcher(out).replaceAll(MASK);
        out = ACCESSION.matcher(out).replaceAll(MASK);
        out = DATE.matcher(out).replaceAll(DATE_MASK);
        out = PHONE.matcher(out).replaceAll(MASK);
        out = LONG_NUMBER.matcher(out).replaceAll(MASK);
        return out;
    }

    /**
     * True if the text still contains a known identifier or a forbidden pattern.
     */
    public boolean leaks(String text) {
        if (text == null) {
            return false;
        }
        String folded = foldKeepLength(text);
        String lower = lowerKeepLength(text);
        for (String id : known) {
            boolean phrase = isPhrase(id);
            Matcher m = Pattern.compile("(?<![\\p{L}\\d])"
                    + Pattern.quote(phrase ? foldKeepLength(id) : lowerKeepLength(id)) + "(?![\\p{L}\\d])")
                    .matcher(phrase ? folded : lower);
            if (m.find()) {
                return true;
            }
        }
        return ACCESSION.matcher(text).find() || EMAIL.matcher(text).find() || DATE.matcher(text).find()
                || PHONE.matcher(text).find();
    }

    static boolean isPhrase(String id) {
        return id.trim().contains(" ") || id.chars().anyMatch(Character::isDigit);
    }

    /**
     * Replaces whole-word occurrences of {@code needle}, ignoring case, and accents
     * when {@code fold}.
     */
    static String replace(String text, String needle, boolean fold) {
        String hay = fold ? foldKeepLength(text) : lowerKeepLength(text);
        String pin = fold ? foldKeepLength(needle) : lowerKeepLength(needle);
        if (pin.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int from = 0;
        int idx;
        while ((idx = hay.indexOf(pin, from)) >= 0) {
            int end = idx + pin.length();
            boolean wordStart = idx == 0 || !Character.isLetterOrDigit(hay.charAt(idx - 1));
            boolean wordEnd = end == hay.length() || !Character.isLetterOrDigit(hay.charAt(end));
            if (wordStart && wordEnd) {
                sb.append(text, from, idx).append(MASK);
            } else {
                sb.append(text, from, end);
            }
            from = end;
        }
        sb.append(text.substring(from));
        return sb.toString();
    }

    static String lowerKeepLength(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            sb.append(Character.toLowerCase(s.charAt(i)));
        }
        return sb.toString();
    }

    /**
     * Lowercase without accents, one character for one character (positions stay
     * aligned).
     */
    static String foldKeepLength(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            char b = base.isEmpty() ? c : base.charAt(0);
            if (b == 'đ' || b == 'Đ') {
                b = 'd';
            }
            sb.append(Character.toLowerCase(b));
        }
        return sb.toString();
    }

    static String fold(String s) {
        return foldKeepLength(s).trim();
    }
}
