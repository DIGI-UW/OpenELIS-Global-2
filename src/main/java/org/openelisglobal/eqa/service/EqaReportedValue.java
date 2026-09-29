package org.openelisglobal.eqa.service;

import org.apache.commons.text.StringEscapeUtils;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;

/**
 * The value an analyst reported, as data rather than as markup.
 *
 * <p>
 * {@code ResultService.getResultValue} renders a dictionary result for HTML
 * display and escapes it on the way out, so an entry such as "Négatif" arrives
 * as "N&amp;eacute;gatif". EQA does not display that string: it compares it
 * against a target a supervisor typed in the clear, stores it as the reported
 * value of record, and ships it to the provider over FHIR and CSV. Escaped, a
 * correct answer fails the comparison and a right result is scored
 * unacceptable.
 *
 * <p>
 * The escaping stays where it is, because the display callers rely on it; every
 * EQA reader comes through here instead.
 */
final class EqaReportedValue {

    private EqaReportedValue() {
    }

    /** The reported value of a single pipeline result, unescaped. */
    static String of(ResultService resultService, Result pipelineResult) {
        String printable = resultService.getResultValue(pipelineResult, ",", true, false);
        return printable == null ? null : StringEscapeUtils.unescapeHtml4(printable);
    }
}
