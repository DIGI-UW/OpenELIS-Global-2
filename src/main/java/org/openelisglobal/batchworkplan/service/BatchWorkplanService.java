package org.openelisglobal.batchworkplan.service;

import java.util.List;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;
import org.openelisglobal.batchworkplan.form.BatchReagentRequest;
import org.openelisglobal.batchworkplan.form.BatchSetupOptionsResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;

public interface BatchWorkplanService {

    /**
     * Pending tests the given user is allowed to work, scoped to the lab units
     * their Results role covers. A user with no lab unit assignment sees nothing,
     * which is how every other workplan screen behaves.
     */
    List<PendingBatchTestResponse> getPendingTests(Integer limit, String sysUserId);

    /**
     * The caller's own batches that are still work in hand. Archived batches are
     * kept for audit but drop out of the working view, and their analyses return to
     * the pending pool.
     */
    List<BatchWorkplanResponse> getBatches(String sysUserId);

    BatchWorkplanResponse createBatch(BatchWorkplanRequest request, String sysUserId);

    BatchWorkplanResponse transitionBatch(Long id, BatchWorkplanStatus nextStatus, String sysUserId);

    /** One of the caller's batches, with its reagent, QC standing and releases. */
    BatchWorkplanResponse getBatch(Long id, String sysUserId);

    /**
     * Rename a draft batch and replace its tests (FR-BL-002). Tests already in the
     * batch stay; added ones must be pending, in the caller's lab units and in no
     * other open batch.
     */
    BatchWorkplanResponse updateBatch(Long id, BatchWorkplanRequest request, String sysUserId);

    /**
     * The reagents and analyzers a draft batch can be set up with (FR-RA-001/002).
     */
    BatchSetupOptionsResponse getSetupOptions(Long id, String sysUserId);

    /**
     * Phase 2: the reagent, lot and analyzer a draft batch runs with
     * (FR-RA-001-004). The lot must be one the Results page would offer for that
     * reagent today.
     */
    BatchWorkplanResponse assignReagent(Long id, BatchReagentRequest request, String sysUserId);

    /** The batch's current QC standing (FR-QC-003). */
    BatchQcStatusResponse getQcStatus(Long id, String sysUserId);

    /**
     * Release a draft batch for printing (FR-WG-001-003). QC Pass releases it. Any
     * other overridable status releases it only when {@code override} is set and
     * the caller {@code mayOverride}, and then raises an NCE linked to the batch
     * (FR-NCE-002). No lot, or a retired lot, never releases (BR-012).
     *
     * @throws BatchReleaseRefusedException when the release is refused
     */
    BatchWorkplanResponse generate(Long id, boolean override, boolean mayOverride, String sysUserId);

    /** Archive every batch the caller still has in hand (FR-BL-009). */
    int archiveAll(String sysUserId);

    /**
     * Move the caller's active batches whose every test is finalized to COMPLETED
     * (BR-009), and archive completed batches older than 30 days (FR-BL-010).
     */
    int refreshLifecycle(String sysUserId);
}
