package org.openelisglobal.labelpreset.controller.rest;

import jakarta.validation.Valid;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestPayload;
import org.openelisglobal.labelpreset.dto.OrderEntryLabelRequestResponse;
import org.openelisglobal.labelpreset.service.OrderEntryLabelRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Order Entry Labels-section aggregation (OGC-285 M5,
 * T140). Implements {@code POST /api/orderEntry/labelRequest} per
 * {@code contracts/openapi.yaml} §8.1.
 *
 * <p>
 * Pure read; idempotent; no persistence. Returns the dynamic two-table column
 * set + per-cell default/max/source/locked computed by
 * {@link OrderEntryLabelRequestService}.
 *
 * <p>
 * Secured with {@code @PreAuthorize("hasRole('ADMIN')")} — consistent with the
 * other labelpreset REST controllers (LabelPresetRestController,
 * TestLabelConfigRestController). The openapi {@code order.create} scope is not
 * wired as a granular Spring authority in this codebase; role-based gating is
 * the repo convention.
 */
@RestController
@RequestMapping("/api/orderEntry")
@PreAuthorize("hasRole('ADMIN')")
public class OrderEntryLabelRequestController extends BaseRestController {

    @Autowired
    private OrderEntryLabelRequestService orderEntryLabelRequestService;

    /**
     * POST /api/orderEntry/labelRequest — compute the Order Entry Labels section
     * column set + per-cell defaults for the candidate order (tests + samples).
     */
    @PostMapping(value = "/labelRequest")
    public ResponseEntity<OrderEntryLabelRequestResponse> computeLabelRequest(
            @RequestBody @Valid OrderEntryLabelRequestPayload payload) {
        return ResponseEntity.ok(orderEntryLabelRequestService.computeLabelRequest(payload));
    }
}
