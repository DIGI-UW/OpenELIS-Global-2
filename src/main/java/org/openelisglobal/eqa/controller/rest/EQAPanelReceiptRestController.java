package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQAPanelReceiptService;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.openelisglobal.shipment.service.ShippingBoxService;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel-receipt intake (OGC-609, FR-V2.1-20). Reception records receipts, so
 * the class-level roles are the real gate here, not a placeholder.
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize(EQAGuards.READ)
public class EQAPanelReceiptRestController extends BaseRestController {

    private final EQAPanelReceiptService receiptService;
    private final ShippingBoxService shippingBoxService;

    public EQAPanelReceiptRestController(EQAPanelReceiptService receiptService, ShippingBoxService shippingBoxService) {
        this.receiptService = receiptService;
        this.shippingBoxService = shippingBoxService;
    }

    /** Idempotent: 201 on first record, 200 with the existing row afterwards. */
    @PostMapping(value = "/cycles/{cycleId}/receipt", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PARTICIPANT)
    public ResponseEntity<Map<String, Object>> recordReceipt(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestBody Map<String, Object> body) {
        Long labEnrollmentId = strictLong(body, "labEnrollmentId");
        if (labEnrollmentId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "labEnrollmentId is required"));
        }

        String sysUserId = getSysUserId(request);
        Long receivedBy = strictLong(body, "receivedBy");
        if (receivedBy == null) {
            try {
                receivedBy = Long.valueOf(sysUserId);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot resolve the receiving user"));
            }
        }

        Integer shipmentId = null;
        Object rawShipment = body.get("shipmentId");
        if (rawShipment != null && !String.valueOf(rawShipment).isBlank()) {
            shipmentId = Integer.valueOf(String.valueOf(rawShipment));
        }
        Integer shippingBoxId = null;
        Object rawBox = body.get("shippingBoxId");
        if (rawBox != null && !String.valueOf(rawBox).isBlank()) {
            shippingBoxId = Integer.valueOf(String.valueOf(rawBox));
        }

        BigDecimal receivedTempC = null;
        Object rawTemp = body.get("receivedTempC");
        if (rawTemp != null && !String.valueOf(rawTemp).isBlank()) {
            receivedTempC = new BigDecimal(String.valueOf(rawTemp));
        }

        Boolean integrityOk = body.get("integrityOk") == null ? null
                : Boolean.valueOf(String.valueOf(body.get("integrityOk")));

        boolean existedBefore = !receiptService
                .getAllMatching(Map.of("cycle.id", cycleId, "labEnrollmentId", labEnrollmentId)).isEmpty();

        EQAPanelReceipt receipt = receiptService.recordReceipt(cycleId, labEnrollmentId, shipmentId, shippingBoxId,
                receivedTempC, integrityOk, stringField(body, "integrityNotes"), receivedBy, sysUserId);

        return ResponseEntity.status(existedBefore ? HttpStatus.OK : HttpStatus.CREATED).body(toDto(cycleId, receipt));
    }

    /**
     * The already-recorded receipt for this cycle and enrollment, so order entry
     * can render it read-only instead of re-offering the fields. 404 when none.
     */
    @GetMapping(value = "/cycles/{cycleId}/receipt", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getReceipt(@PathVariable Long cycleId,
            @RequestParam Long labEnrollmentId) {
        return receiptService.getAllMatching(Map.of("cycle.id", cycleId, "labEnrollmentId", labEnrollmentId)).stream()
                .findFirst().map(receipt -> ResponseEntity.ok(toDto(cycleId, receipt)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Map<String, Object> toDto(Long cycleId, EQAPanelReceipt receipt) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", receipt.getId());
        dto.put("cycleId", cycleId);
        dto.put("labEnrollmentId", receipt.getLabEnrollmentId());
        dto.put("shipmentId", receipt.getShipmentId());
        dto.put("shippingBoxId", receipt.getShippingBoxId());
        // The box code is the receipt's shipment reference (FR-V2.2-12).
        ShippingBox box = receipt.getShippingBoxId() == null ? null
                : shippingBoxService.getBoxById(receipt.getShippingBoxId());
        dto.put("boxCode", box == null ? null : box.getBoxId());
        dto.put("receivedDate", receipt.getReceivedDate() == null ? null : receipt.getReceivedDate().toString());
        dto.put("receivedTempC", receipt.getReceivedTempC());
        dto.put("integrityOk", receipt.getIntegrityOk());
        dto.put("integrityNotes", receipt.getIntegrityNotes());
        return dto;
    }

    /**
     * Deliberately not the inherited {@code longField}: a malformed id here escapes
     * as NumberFormatException, which this controller answers 400 — the same status
     * as its other malformed numeric fields.
     */
    private Long strictLong(Map<String, Object> body, String key) {
        String value = stringField(body, key);
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ObjectNotFoundException e) {
        return Map.of("error", "EQA cycle not found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleBadInput(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(NumberFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadNumber(NumberFormatException e) {
        return Map.of("error", "A numeric field could not be parsed");
    }

    /**
     * labEnrollmentId is a raw FK column — surface a bad reference as a conflict,
     * not a 500 (UAT finding, 2026-08-18). Narrowed to constraint violations so a
     * real DB failure still reads as a 500.
     */
    @ExceptionHandler(org.hibernate.exception.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConstraintViolation(org.hibernate.exception.ConstraintViolationException e) {
        return Map.of("error", "The receipt conflicts with existing data: a referenced record does not exist,"
                + " or a receipt for this cycle and enrollment already exists");
    }
}
