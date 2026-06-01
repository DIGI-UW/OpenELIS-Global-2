package org.openelisglobal.labelpreset.controller.rest;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.openelisglobal.labelpreset.dto.OrderLabelRequestView;
import org.openelisglobal.labelpreset.service.OrderLabelReprintService;
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
 * snapshots (OGC-285 M6, T168/T169).
 *
 * <ul>
 * <li>{@code GET /api/orders/{id}/labels} — the persisted
 * {@code order_label_request} rows for an order, each with its frozen
 * {@code preset_snapshot} JSON.</li>
 * <li>{@code GET /api/barcode/print/{orderId}/{presetId}} — an
 * {@code application/pdf} rendered <em>from the rows' snapshots</em> (AC-20: no
 * live {@code label_preset} lookup).</li>
 * </ul>
 *
 * <p>
 * Security: {@code hasRole('ADMIN')} (consistent with the rest of the label
 * preset REST surface). No {@code @Transactional} on the controller — the
 * read-only transaction and all LAZY resolution live in the service.
 */
@RestController
@PreAuthorize("hasRole('ADMIN')")
public class OrderLabelReprintController {

    @Autowired
    private OrderLabelReprintService orderLabelReprintService;

    // ── GET /api/orders/{id}/labels ───────────────────────────────────────────

    @GetMapping("/api/orders/{id}/labels")
    public ResponseEntity<List<OrderLabelRequestView>> getOrderLabels(@PathVariable("id") String orderId) {
        return ResponseEntity.ok(orderLabelReprintService.listByOrder(orderId));
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
