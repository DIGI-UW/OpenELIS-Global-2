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
     * The provider scheme list (FR-V2.5-01): one row per scheme this lab provides —
     * meaning at least one other laboratory is actively enrolled — each carrying
     * its cycles with their participant and panel counts.
     *
     * <p>
     * Four grouped queries regardless of how many schemes or cycles there are: the
     * schemes with their enrollment counts, their cycles, panel counts per cycle,
     * roster rows per cycle. This is what replaced T-25's walk over every cycle in
     * the database asking each scheme for its count.
     */
    Map<String, Object> getProviderSchemes();

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

    /** One row per laboratory on the cycle's participant roster (FR-V2.5-13). */
    List<Map<String, Object>> getShipmentRows(Long cycleId);

    /**
     * T-41: a partner lab's receipt reached this provider over FHIR (the box's
     * own-store SupplyDelivery reads completed), so record the delivery here. When
     * the box is the participant's current shipment this is exactly the Receipt
     * Monitor's "Mark delivered" — cycle auto-advance and audit included; an
     * outdated box (a repeat superseded it) is closed off quietly without touching
     * the cycle.
     *
     * @param shippingBoxId the delivered box
     * @param sysUserId     the automated user the delivery is attributed to
     */
    void applyRemoteDelivery(Integer shippingBoxId, String sysUserId);

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

    /**
     * Receipt monitor rows (T-26, FR-V2.5-14): the shipment rows plus what
     * receiving adds — received date and the overdue verdict. Overdue = not
     * delivered and today is past the expected delivery plus two business days.
     */
    List<Map<String, Object>> getReceiptRows(Long cycleId);

    /**
     * Provider-side manual receipt fallback (FR-V2.5-14): the participant lab
     * confirmed arrival out of band, so the provider records the delivery. When the
     * last active participant's box is delivered, the cycle advances shipped →
     * delivered → submissions_open automatically. Idempotent on a delivered box.
     */
    Map<String, Object> markDelivered(Long cycleId, Long organizationId, String sysUserId);

    /**
     * Reprovision a participant (T-26, FR-V2.5-15): a new shipment recording which
     * one it replaces, consuming one aliquot per panel sample — from the reserve
     * first; dipping into unreserved production requires a written override note.
     *
     * @throws IllegalStateException when the inventory cannot cover the repeat, or
     *                               the reserve is short and no note was given
     */
    Map<String, Object> sendRepeat(Long cycleId, Long organizationId, String overrideNote, String sysUserId);
}
