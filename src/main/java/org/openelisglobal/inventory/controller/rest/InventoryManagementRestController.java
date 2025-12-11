package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.service.InventoryManagementService.ConsumptionRecord;
import org.openelisglobal.inventory.service.InventoryManagementService.InventoryAlerts;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory/management")
public class InventoryManagementRestController extends BaseRestController {

    @Autowired
    private InventoryManagementService inventoryManagementService;

    @PostMapping(value = "/consume", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> consumeInventory(@RequestBody ConsumeRequest request, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            // Convert testResultId and analysisId from String to Long
            Long testResultIdLong = request.getTestResultId() != null && !request.getTestResultId().isEmpty()
                    ? Long.valueOf(request.getTestResultId())
                    : null;
            Long analysisIdLong = request.getAnalysisId() != null && !request.getAnalysisId().isEmpty()
                    ? Long.valueOf(request.getAnalysisId())
                    : null;

            List<ConsumptionRecord> records = inventoryManagementService.consumeInventoryFEFO(
                    Long.valueOf(request.getItemId()), request.getQuantity(), testResultIdLong, analysisIdLong,
                    sysUserId);

            return ResponseEntity.ok(new ConsumeResponse(records));
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error"));
        }
    }

    @PostMapping(value = "/receive", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> receiveInventory(@RequestBody InventoryLot lot, HttpServletRequest httpRequest) {
        try {
            UserSessionData usd = (UserSessionData) httpRequest.getSession().getAttribute(USER_SESSION_DATA);
            String sysUserId = String.valueOf(usd.getSystemUserId());

            InventoryLot receivedLot = inventoryManagementService.receiveInventory(lot, sysUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(receivedLot);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error"));
        }
    }

    @GetMapping(value = "/check-availability", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AvailabilityResponse> checkAvailability(@RequestParam String itemId,
            @RequestParam Double quantity) {
        try {
            boolean isAvailable = inventoryManagementService.isSufficientInventoryAvailable(Long.valueOf(itemId),
                    quantity);
            return ResponseEntity.ok(new AvailabilityResponse(isAvailable, itemId, quantity));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryAlerts> getAlerts(@RequestParam(defaultValue = "30") int expirationWarningDays) {
        try {
            InventoryAlerts alerts = inventoryManagementService.getInventoryAlerts(expirationWarningDays);
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class ConsumeRequest {
        private String itemId;
        private Double quantity;
        private String testResultId;
        private String analysisId;

    }

    @Setter
    @Getter
    public static class ConsumeResponse {
        private List<ConsumptionRecord> consumedLots;

        public ConsumeResponse(List<ConsumptionRecord> consumedLots) {
            this.consumedLots = consumedLots;
        }

    }

    @Setter
    @Getter
    public static class AvailabilityResponse {
        private Boolean isAvailable;
        private String itemId;
        private Double requestedQuantity;

        public AvailabilityResponse(Boolean isAvailable, String itemId, Double requestedQuantity) {
            this.isAvailable = isAvailable;
            this.itemId = itemId;
            this.requestedQuantity = requestedQuantity;
        }

    }

    @Setter
    @Getter
    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

    }

}
