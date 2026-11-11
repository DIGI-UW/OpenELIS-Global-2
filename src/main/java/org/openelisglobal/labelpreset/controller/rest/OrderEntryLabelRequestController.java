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
 * REST controller for the Order Entry Labels-section aggregation (OGC-285 M5).
 * Implements {@code POST /api/orderEntry/labelRequest} per
 * {@code contracts/openapi.yaml} §8.1.
 *
 * <p>
 * Pure read; idempotent; no persistence. Returns the dynamic two-table column
 * set + per-cell default/max/source/locked computed by
 * {@link OrderEntryLabelRequestService}.
 *
 * <p>
 * Secured with {@code @PreAuthorize("isAuthenticated()")}. This is the
 * {@code order.create} surface (FRS §8.1; tasks.md T140) consumed by the Order
 * Entry Labels section — i.e. by reception/technician users creating an order,
 * NOT by admins. The admin CRUD controllers (LabelPresetRestController,
 * TestLabelConfigRestController) correctly use {@code hasRole('ADMIN')}, but
 * applying that here would 403 every order-entry user and break the dynamic
 * Labels section for everyone but admins. The order-save controller this pairs
 * with ({@code SamplePatientEntryRestController}) carries no method-level role
 * gate; order entry is gated only by the authenticated filter chain, so
 * {@code isAuthenticated()} matches that existing gate exactly. The openapi
 * {@code order.create} scope is not wired as a granular Spring authority in
 * this codebase.
 */
@RestController
@RequestMapping("/api/orderEntry")
@PreAuthorize("isAuthenticated()")
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
