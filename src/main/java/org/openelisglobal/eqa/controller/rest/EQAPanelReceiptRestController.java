package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQAPanelReceiptService;
import org.openelisglobal.eqa.valueholder.EQAPanelReceipt;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel-receipt intake (T-11, FR-V2.1-20). Reception records receipts, so the
 * class-level roles are the real gate here, not a placeholder.
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS')")
public class EQAPanelReceiptRestController extends BaseRestController {

    private final EQAPanelReceiptService receiptService;

    public EQAPanelReceiptRestController(EQAPanelReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    /** Idempotent: 201 on first record, 200 with the existing row afterwards. */
    @PostMapping(value = "/cycles/{cycleId}/receipt", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> recordReceipt(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestBody Map<String, Object> body) {
        Long labEnrollmentId = longField(body, "labEnrollmentId");
        if (labEnrollmentId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "labEnrollmentId is required"));
        }

        String sysUserId = getSysUserId(request);
        Long receivedBy = longField(body, "receivedBy");
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

        BigDecimal receivedTempC = null;
        Object rawTemp = body.get("receivedTempC");
        if (rawTemp != null && !String.valueOf(rawTemp).isBlank()) {
            receivedTempC = new BigDecimal(String.valueOf(rawTemp));
        }

        Boolean integrityOk = body.get("integrityOk") == null ? null
                : Boolean.valueOf(String.valueOf(body.get("integrityOk")));

        boolean existedBefore = !receiptService
                .getAllMatching(Map.of("cycle.id", cycleId, "labEnrollmentId", labEnrollmentId)).isEmpty();

        EQAPanelReceipt receipt = receiptService.recordReceipt(cycleId, labEnrollmentId, shipmentId, receivedTempC,
                integrityOk, stringField(body, "integrityNotes"), receivedBy, sysUserId);

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", receipt.getId());
        dto.put("cycleId", cycleId);
        dto.put("labEnrollmentId", receipt.getLabEnrollmentId());
        dto.put("shipmentId", receipt.getShipmentId());
        dto.put("receivedDate", receipt.getReceivedDate() == null ? null : receipt.getReceivedDate().toString());
        dto.put("integrityOk", receipt.getIntegrityOk());

        return ResponseEntity.status(existedBefore ? HttpStatus.OK : HttpStatus.CREATED).body(dto);
    }

    private String stringField(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long longField(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
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
