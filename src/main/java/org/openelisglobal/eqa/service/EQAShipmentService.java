package org.openelisglobal.eqa.service;

import java.sql.Date;
import java.util.List;
import java.util.Map;

/**
 * The provider prep and shipment workbenches (T-25, FR-V2.5-12 / FR-V2.5-13).
 *
 * <p>
 * Panel material travels as ordinary {@code shipping_box} + {@code shipment}
 * rows (AC-V2.5-12) — this service is the orchestration over the existing
 * shipment module, not a second shipping implementation. One box per
 * participant per cycle, keyed by a deterministic box id, so re-saving a
 * participant's courier details updates rather than duplicates.
 */
public interface EQAShipmentService {

    /**
     * Provider cycles: those whose scheme has at least one active participant
     * enrollment, which is what makes this lab the provider (FR-V2.5-01).
     *
     * <p>
     * ponytail: the provider cycle list T-24 will own properly; this is the minimum
     * that makes the workbench reachable. It walks every cycle and asks each scheme
     * for its participant count, which is fine at a few hundred cycles — T-24
     * should replace it with a query that filters and counts in the database.
     */
    List<Map<String, Object>> getProviderCycles();

    /**
     * Prep state for one cycle: participant count, and per panel the produced /
     * reserved / shipped counts against what FR-V2.5-12 requires, plus whether the
     * ready-to-ship gate would pass and why not.
     */
    Map<String, Object> getPrepStatus(Long cycleId);

    /**
     * Record prep progress against a panel: aliquot counts and the homogeneity QC
     * verdict (FR-V2.5-12). Refuses counts that break the produced >= reserved +
     * shipped invariant before the DB CHECK does, so the workbench gets a message
     * rather than a constraint error.
     *
     * <p>
     * Answers with the panel's cycle prep state, as {@link #getPrepStatus} builds
     * it: the needed/shortfall arithmetic and the gate verdict stay server-side.
     */
    Map<String, Object> savePrep(Long panelId, Integer aliquotsProduced, Integer aliquotsReserved,
            Boolean homogeneityQcPassed, String homogeneityQcNotes, String sysUserId);

    /** One row per active participant of the cycle's scheme (FR-V2.5-13). */
    List<Map<String, Object>> getShipmentRows(Long cycleId);

    /**
     * Create or update this participant's box and shipment details. Idempotent: the
     * box id is derived from cycle + organization.
     */
    Map<String, Object> saveShipmentDetails(Long cycleId, Long organizationId, String courier, String trackingNumber,
            Date estimatedDeliveryDate, String sysUserId);

    /**
     * Dispatch the named participants' boxes (bulk mark-shipped). The first
     * dispatch of a cycle moves it ready_to_ship → shipped automatically, so the
     * gate is what stands between prep and dispatch — not a UI decision. Repeated
     * ids dispatch once.
     *
     * @throws IllegalStateException    when the cycle has not been cleared to ship,
     *                                  a box is in no state to leave, or a panel
     *                                  holds too few aliquots for the batch
     * @throws IllegalArgumentException when a named participant is not enrolled,
     *                                  has no box, or has no courier recorded
     */
    List<Map<String, Object>> markShipped(Long cycleId, List<Long> organizationIds, String sysUserId);
}
