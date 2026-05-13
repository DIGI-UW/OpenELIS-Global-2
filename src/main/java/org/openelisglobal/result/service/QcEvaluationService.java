package org.openelisglobal.result.service;

import org.openelisglobal.result.valueholder.Result;

/**
 * Evaluates QC acceptance criteria for results whose sample items have a QC
 * profile (Blank / Duplicate / Control).
 *
 * Called synchronously at result save time. Sets qcEvaluation and
 * qcEvaluationDetail on the Result entity in-place.
 *
 * Parallel pattern to Westgard rule evaluation, but for environmental QC
 * acceptance checks rather than statistical process control.
 */
public interface QcEvaluationService {

    /**
     * Evaluate a result against QC acceptance criteria.
     *
     * If the result's sample item has no QC profile, this is a no-op. Otherwise,
     * sets result.qcEvaluation (PASS/FAIL/N_A) and result.qcEvaluationDetail with
     * the computed metric.
     *
     * @param result the result to evaluate (must have analysis loaded)
     */
    void evaluateQc(Result result);
}
