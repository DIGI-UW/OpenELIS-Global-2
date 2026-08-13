package org.openelisglobal.labelpreset.controller.rest;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.openelisglobal.labelpreset.dto.OrderLabelRequestView;
import org.openelisglobal.labelpreset.service.OrderLabelReprintService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for reprinting persisted order labels from their frozen
 * snapshots (OGC-285 M6).
 *
 * <ul>
 * <li>{@code GET /api/orders/{id}/labels} — the persisted
 * {@code order_label_request} rows for an order, keyed by the internal
 * {@code Sample} PK, each with its frozen {@code preset_snapshot} JSON.</li>
 * <li>{@code GET /api/orders/by-accession/{accessionNumber}/labels} — the same
 * rows, but resolved from the human-facing accession number that order-entry
 * frontends actually hold (they never see the internal PK). Resolves the
 * accession to a {@code Sample} then delegates to the PK path; {@code 404} when
 * the accession matches no sample, {@code 200 []} when the sample exists but
 * has no persisted label requests.</li>
 * <li>{@code GET /api/barcode/print/{orderId}/{presetId}} — an
 * {@code application/pdf} rendered <em>from the rows' snapshots</em> (AC-20: no
 * live {@code label_preset} lookup).</li>
 * </ul>
 *
 * <p>
 * Security: {@code isAuthenticated()}. This is the {@code order.read} surface
 * (FRS §8; tasks.md T169) — User Story 5 has a laboratory <em>technician</em>
 * (not an admin) reprint a saved order's labels. The order-save controller it
 * pairs with ({@code SamplePatientEntryRestController}) carries no method-level
 * role gate at all; order entry is gated only by the authenticated
 * {@code /rest} /{@code /api} filter chain. Using {@code hasRole('ADMIN')} here
 * (as the admin CRUD controllers correctly do) would 403 every
 * reception/technician user and break the M6 reprint + post-save dialog for
 * everyone but admins — a regression versus the legacy
 * {@code /LabelMakerServlet} path, which any authenticated user can reach.
 * {@code isAuthenticated()} matches that existing order-entry gate without
 * inventing a new role requirement.
 *
 * <p>
 * No {@code @Transactional} on the controller — the read-only transaction and
 * all LAZY resolution live in the service.
 */
@RestController
@PreAuthorize("isAuthenticated()")
public class OrderLabelReprintController {

    @Autowired
    private OrderLabelReprintService orderLabelReprintService;

    @Autowired
    private SampleService sampleService;

    // ── GET /api/orders/{id}/labels ───────────────────────────────────────────

    @GetMapping("/api/orders/{id}/labels")
    public ResponseEntity<List<OrderLabelRequestView>> getOrderLabels(@PathVariable("id") String orderId) {
        return ResponseEntity.ok(orderLabelReprintService.listByOrder(orderId));
    }

    // ── GET /api/orders/by-accession/{accessionNumber}/labels ─────────────────

    /**
     * Accession-keyed twin of {@link #getOrderLabels(String)} for frontends that
     * hold only the accession number, not the internal {@code Sample} PK. Resolves
     * the accession to a {@code Sample} (the id is non-LAZY, so this is safe
     * outside a transaction) and reuses the same {@code listByOrder} read path.
     *
     * @param accessionNumber the human-facing accession of the order's parent
     *                        sample
     * @return {@code 200} with the rows (possibly empty) when the accession
     *         resolves; {@code 404} when no sample matches the accession
     */
    @GetMapping("/api/orders/by-accession/{accessionNumber}/labels")
    public ResponseEntity<List<OrderLabelRequestView>> getOrderLabelsByAccession(
            @PathVariable("accessionNumber") String accessionNumber) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(orderLabelReprintService.listByOrder(sample.getId()));
    }

    // ── GET /api/barcode/print/{orderId}/{presetId} ───────────────────────────

    @GetMapping("/api/barcode/print/{orderId}/{presetId}")
    public ResponseEntity<byte[]> printFromSnapshot(@PathVariable("orderId") String orderId,
            @PathVariable("presetId") Integer presetId) {
        ByteArrayOutputStream pdf = orderLabelReprintService.renderFromSnapshot(orderId, presetId);
        if (pdf == null || pdf.size() == 0) {
            return ResponseEntity.notFound().build();
        }
        byte[] body = pdf.toByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=order-labels.pdf");
        headers.setContentLength(body.length);
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
