package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.util.Map;
import org.openelisglobal.eqa.valueholder.EQAResult;

/**
 * The target a provider-side result is shown against, wherever it is shown: the
 * Performance Report's Scoring detail and the follow-up register both read it
 * here, so the two cannot disagree (OGC-1665).
 *
 * <p>
 * A numeric target is copied onto the result when it is scored, so that is the
 * value it was actually judged against. A qualitative target is a word that
 * {@code target_value}, numeric, cannot hold, so it is read from the sealed
 * panel sample the result answers. The sealed targets are keyed by result, so
 * each of two samples of one analyte shows its own.
 */
public final class EQAResultTarget {

    private EQAResultTarget() {
    }

    public static String of(EQAResult result, Map<Long, String> sealedTargetsByResult) {
        if (result.getResultText() != null) {
            return sealedTargetsByResult.get(result.getId());
        }
        return number(result.getTargetValue());
    }

    public static String number(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
}
