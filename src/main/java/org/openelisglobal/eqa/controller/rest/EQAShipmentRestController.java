package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQAShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provider prep and shipment workbenches (T-25, FR-V2.5-12 / FR-V2.5-13).
 *
 * <p>
 * There is no ready-to-ship endpoint here: clearing a cycle to ship is the
 * existing provider transition (PATCH /rest/eqa/cycles/{id}/transition), whose
 * gate T-25 completes — one gate, one place, and a stale client gets 409 rather
 * than a second opinion.
 *
 * <p>
 * Reads sit under the {@link EQAGuards#READ} umbrella; prep and dispatch are
 * provider-lane writes, so they declare {@link EQAGuards#PROVIDER} — running a
 * cycle for other laboratories is the provider's job, not the bench's.
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize(EQAGuards.READ)
public class EQAShipmentRestController extends BaseRestController {

    private final EQAShipmentService shipmentService;

    public EQAShipmentRestController(EQAShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping(value = "/provider/cycles", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> providerCycles() {
        return shipmentService.getProviderCycles();
    }

    @GetMapping(value = "/cycles/{cycleId}/prep", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> prepStatus(@PathVariable Long cycleId) {
        return shipmentService.getPrepStatus(cycleId);
    }

    @PatchMapping(value = "/panels/{panelId}/prep", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public Map<String, Object> savePrep(HttpServletRequest request, @PathVariable Long panelId,
            @RequestBody Map<String, Object> body) {
        // The response is the whole cycle's prep state: needed/shortfall arithmetic
        // and the gate verdict stay where the gate reads them.
        return shipmentService.savePrep(panelId, integerField(body, "aliquotsProduced"),
                integerField(body, "aliquotsReserved"), booleanField(body, "homogeneityQcPassed"),
                stringField(body, "homogeneityQcNotes"), getSysUserId(request));
    }

    @GetMapping(value = "/cycles/{cycleId}/shipments", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> shipments(@PathVariable Long cycleId) {
        return shipmentService.getShipmentRows(cycleId);
    }

    @PostMapping(value = "/cycles/{cycleId}/shipments", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public Map<String, Object> saveShipment(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestBody Map<String, Object> body) {
        Long organizationId = longField(body, "organizationId");
        if (organizationId == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
        return shipmentService.saveShipmentDetails(cycleId, organizationId, stringField(body, "courier"),
                stringField(body, "trackingNumber"), dateField(body, "estimatedDeliveryDate"), getSysUserId(request));
    }

    /** Bulk dispatch: {"organizationIds": [1, 2, 3]}. */
    @PostMapping(value = "/cycles/{cycleId}/shipments/ship", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public List<Map<String, Object>> ship(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestBody Map<String, Object> body) {
        return shipmentService.markShipped(cycleId, longListField(body, "organizationIds"), getSysUserId(request));
    }

    private Boolean booleanField(Map<String, Object> body, String key) {
        String value = stringField(body, key);
        return value == null ? null : Boolean.valueOf(value);
    }

    private Date dateField(Map<String, Object> body, String key) {
        String value = stringField(body, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Date.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(key + " must be an ISO date (yyyy-MM-dd)");
        }
    }

    private List<Long> longListField(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof List<?> raw)) {
            throw new IllegalArgumentException(key + " must be a list of ids");
        }
        List<Long> ids = new ArrayList<>();
        for (Object element : raw) {
            try {
                ids.add(Long.valueOf(String.valueOf(element).trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " must be a list of ids");
            }
        }
        return ids;
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ObjectNotFoundException e) {
        return Map.of("error", "EQA cycle or panel not found");
    }

    /** Shipping before the gate cleared the cycle is a conflict with its state. */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(IllegalStateException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleBadInput(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
