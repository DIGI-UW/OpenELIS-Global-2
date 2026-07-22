package org.openelisglobal.qaevent.qiconfig.breach;

/**
 * OGC-712 — evaluates QI indicators against their qi_config action thresholds
 * and fires an auto-NCE on breach. Runs daily via {@code @Scheduled} on the
 * impl.
 */
public interface QiBreachEvaluatorService {

    void evaluateBreaches();
}
