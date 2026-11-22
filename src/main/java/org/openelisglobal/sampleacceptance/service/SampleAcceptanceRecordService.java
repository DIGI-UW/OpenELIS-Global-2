package org.openelisglobal.sampleacceptance.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord.Answer;

/**
 * Runtime service for the per-specimen Sample Acceptance Record (S-09 /
 * OGC-580): records assessments (append-only) keyed to a {@code sample_item},
 * resolves the parent order's effective domain, and evaluates whether a
 * specimen is blocked under the per-domain enforcement mode.
 */
public interface SampleAcceptanceRecordService extends BaseObjectService<SampleAcceptanceRecord, Integer> {

    /**
     * Record a new acceptance assessment for a specimen. Append-only — always
     * inserts a new immutable row; the overall status is computed from the answers
     * against the specimen's currently-resolved checklist and snapshotted on the
     * row.
     */
    SampleAcceptanceRecord recordAssessment(String sampleItemId, List<Answer> answers, Integer userId);

    /**
     * Cascade a single acceptance assessment to every live member
     * {@code sample_item} of a vector pool — the pool is the unit of acceptance for
     * vector, but the decision commits to its individual specimens. Records the
     * same answers per member via {@link #recordAssessment} (voided/rejected
     * members skipped), so the existing per-specimen gate evaluates the order
     * correctly with no gate change. Returns one record per member.
     */
    List<SampleAcceptanceRecord> recordAssessmentForPool(Integer vectorPoolId, List<Answer> answers, Integer userId);

    /** The current (latest) acceptance record for a specimen, or null. */
    SampleAcceptanceRecord getLatest(String sampleItemId);

    /**
     * The append-only history of acceptance records for a specimen, newest first.
     */
    List<SampleAcceptanceRecord> getHistory(String sampleItemId);

    /**
     * The specimen's effective testing domain as CLINICAL / ENVIRONMENTAL / VECTOR
     * (mapped from the parent {@code sample.domain} H/E/V), or null = lab-wide /
     * unknown.
     */
    String resolveDomain(String sampleItemId);

    /**
     * Combined runtime view for one specimen: resolved checklist, enforcement,
     * latest decision, blocked.
     */
    SampleAcceptanceEvaluation evaluate(String sampleItemId);

    /**
     * Per-specimen evaluations for every non-voided {@code sample_item} of an
     * order, in specimen sort order — backs the QA-step intake-acceptance table
     * (one row per specimen with its eligibility status).
     */
    List<SampleAcceptanceEvaluation> evaluateOrder(String sampleId);

    /**
     * FR-08 enforcement gate for one specimen. Throws
     * {@link SampleAcceptanceBlockedException} when the specimen is blocked — i.e.
     * its domain enforcement is MANDATORY and its resolved checklist is not
     * satisfied (unanswered items, or a FAIL). No-op under OPTIONAL/OFF, or when
     * the checklist is satisfied/empty.
     */
    void enforceAcceptanceGate(String sampleItemId);

    /**
     * FR-08 enforcement gate aggregated over a whole order: throws
     * {@link SampleAcceptanceBlockedException} if any non-voided, non-rejected
     * {@code sample_item} of the sample is blocked. This is the server-side
     * backstop the Collect → Label &amp; Store transition calls so the mandatory
     * gate cannot be bypassed client-side. Voided/rejected specimens are resolved
     * (rejected, resampled away, or removed) and do not block.
     */
    void enforceAcceptanceGateForOrder(String sampleId);

    /**
     * A suggested NCE reason pre-filled from the specimen's latest record's failed
     * items (+ notes), or null when there is no record / no failures.
     */
    String buildNcePrefillReason(String sampleItemId);
}
