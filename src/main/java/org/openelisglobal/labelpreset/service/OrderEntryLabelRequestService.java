package org.openelisglobal.labelpreset.service;

import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestPayload;
import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestResponse;

/**
 * Order Entry Labels-section aggregation service (OGC-285 M5, T139). Computes
 * the dynamic two-table column set + per-cell default/max/locked/source for a
 * candidate order (a set of tests + samples), implementing the FRS §4.4.1
 * conflict-resolution rules (data-model.md §6.1).
 *
 * <p>
 * Pure read; the implementation is {@code @Transactional(readOnly = true)} and
 * deterministic: the same inputs (and same underlying preset/link/config rows)
 * always produce the same output. No persistence — the JSONB snapshot is
 * written separately by {@link OrderLabelRequestService} at order-save time.
 */
public interface OrderEntryLabelRequestService {

    /**
     * Aggregate the Order Entry Labels section for the given candidate order.
     *
     * @param payload the candidate order's test ids + samples
     * @return the computed column set + per-cell resolution
     */
    OrderEntryLabelRequestResponse computeLabelRequest(OrderEntryLabelRequestPayload payload);
}
