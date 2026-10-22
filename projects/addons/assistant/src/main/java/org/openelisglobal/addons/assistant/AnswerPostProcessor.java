package org.openelisglobal.addons.assistant;

import java.util.List;
import java.util.regex.Pattern;

/**
 * What every answer goes through before the user sees it or it is logged: (1)
 * the KDIGO staging check, (2) the disclaimer as the last line, exactly once.
 * The disclaimer cannot be turned off.
 */
public final class AnswerPostProcessor {

    /** The Vietnamese disclaimer (the first deployment's wording). */
    public static final String DISCLAIMER = Lang.VI.disclaimer;

    // models write variants: plain hyphen, no final period, italics/bold
    private static final Pattern VARIANTS_VI = Pattern.compile(
            "[*_\\s>]*Gợi ý tham khảo\\s*[—–-]+\\s*không thay (?:thế )?chẩn đoán của bác sĩ\\.?[*_]*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern VARIANTS_EN = Pattern.compile(
            "[*_\\s>]*Suggestion for reference only\\s*[—–-]+\\s*not a substitute for a physician['’]s diagnosis\\.?[*_]*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private AnswerPostProcessor() {
    }

    public record Processed(String text, boolean egfrCorrected, List<String> notes) {
    }

    public static Processed process(String raw, List<Double> egfrValues) {
        return process(raw, egfrValues, null);
    }

    /** Vietnamese. */
    public static Processed process(String raw, List<Double> egfrValues, List<Kdigo.Fact> facts) {
        return process(raw, egfrValues, facts, Lang.VI);
    }

    /**
     * @param facts KDIGO facts computed by the service; null = derived from
     *              {@code egfrValues}
     */
    public static Processed process(String raw, List<Double> egfrValues, List<Kdigo.Fact> facts, Lang lang) {
        String text = raw == null ? "" : raw.trim();
        EgfrCheck.Outcome egfr = facts != null ? EgfrCheck.checkFacts(text, facts, lang)
                : EgfrCheck.check(text, egfrValues, lang);
        Pattern variants = lang.vi() ? VARIANTS_VI : VARIANTS_EN;
        text = variants.matcher(egfr.text()).replaceAll("").stripTrailing();
        text = (text.isEmpty() ? "" : text + "\n\n") + lang.disclaimer;
        return new Processed(text, egfr.corrected(), egfr.notes());
    }
}
