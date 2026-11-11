package org.openelisglobal.qaevent.qiconfig.breach;

/**
 * OGC-712 — evaluates QI indicators against their qi_config action thresholds
 * and fires an auto-NCE on breach. Polls on a fixed rate via {@code @Scheduled}
 * on the impl (10s after startup, then every 2 minutes by default —
 * {@code org.openelisglobal.qi.breach.poll.frequency}, ms).
 */
public interface QiBreachEvaluatorService {

    void evaluateBreaches();
}
