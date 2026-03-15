package org.openelisglobal.dataexchange.externalorders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.dataexchange.externalorders.dto.ValidationReport;
import org.openelisglobal.dataexchange.externalorders.dto.ValidationResult;
import org.openelisglobal.dataexchange.externalorders.service.ExternalOrderValidationService;
import org.openelisglobal.dataexchange.externalorders.service.IncomingOrderService;
import org.openelisglobal.dataexchange.externalorders.valueholder.IncomingOrder;
import org.openelisglobal.patient.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * New, minimal external-order endpoint that reuses the existing
 * SamplePatientEntry pipeline.
 *
 * This controller: - Accepts a focused ExternalOrderRequest JSON payload - Maps
 * it into SamplePatientEntryForm - Delegates to
 * SamplePatientEntryRestController.samplePatientEntrySave(...) so that orders
 * are created exactly as if they came from the UI.
 */
@Controller
@RequestMapping(value = "/rest/external-orders")
public class ExternalOrderRestController {

    @Autowired
    private IncomingOrderService incomingOrderService;

    @Autowired
    private ExternalOrderValidationService validationService;

    @Autowired
    private PatientService patientService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createExternalOrder(HttpServletRequest request,
            @Valid @RequestBody ExternalOrderRequest externalOrderRequest)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // Validate the order
        ValidationReport validationReport = validationService.validateOrder(externalOrderRequest);

        // If patient is invalid, reject without storing
        if (!validationReport.isPatientValid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildValidationResponse(null, validationReport, "REJECTED", "Patient not found."));
        }

        // If nothing is valid, reject without storing
        if (validationReport.isCompletelyInvalid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    buildValidationResponse(null, validationReport, "REJECTED", "No valid tests or panels found."));
        }

        // Filter to only valid items
        ExternalOrderRequest filteredRequest = validationService.filterValidItems(externalOrderRequest,
                validationReport);
        if (filteredRequest == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildValidationResponse(null, validationReport, "REJECTED", "No valid items to store."));
        }

        String payloadJson;
        try {
            payloadJson = new ObjectMapper().writeValueAsString(filteredRequest);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON payload");
        }

        String externalOrderNumber = filteredRequest.getExternalOrderNumber();

        IncomingOrder holding = incomingOrderService.receiveOrMergeOrder(filteredRequest, payloadJson, null);

        // Handle removal-only request for non-existent order (e.g., DISCONTINUE after
        // collection)
        if (holding == null) {
            return ResponseEntity.ok(buildValidationResponse(null, validationReport, "SKIPPED",
                    "Order already processed or does not exist."));
        }

        String status = holding.getLastupdated() == null ? "CREATED" : "MERGED";
        String message = validationReport.isFullyValid() ? "Order received successfully with all items validated."
                : "Order received with partial validation. Some items were not found.";

        return ResponseEntity.ok(buildValidationResponse(holding, validationReport, status, message));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateExternalOrder(HttpServletRequest request,
            @Valid @RequestBody ExternalOrderRequest externalOrderRequest)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        // Validate the order
        ValidationReport validationReport = validationService.validateOrder(externalOrderRequest);

        // If patient is invalid, reject without storing
        if (!validationReport.isPatientValid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildValidationResponse(null, validationReport, "REJECTED", "Patient not found."));
        }

        // If nothing is valid, reject without storing
        if (validationReport.isCompletelyInvalid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    buildValidationResponse(null, validationReport, "REJECTED", "No valid tests or panels found."));
        }

        // Filter to only valid items
        ExternalOrderRequest filteredRequest = validationService.filterValidItems(externalOrderRequest,
                validationReport);
        if (filteredRequest == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildValidationResponse(null, validationReport, "REJECTED", "No valid items to store."));
        }

        String payloadJson;
        try {
            payloadJson = new ObjectMapper().writeValueAsString(filteredRequest);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid JSON payload");
        }

        try {
            IncomingOrder holding = incomingOrderService.receiveOrMergeOrder(filteredRequest, payloadJson, null);

            // Handle removal-only request for non-existent order (e.g., DISCONTINUE after
            // collection)
            if (holding == null) {
                return ResponseEntity.ok(buildValidationResponse(null, validationReport, "SKIPPED",
                        "Order already processed or does not exist."));
            }

            String message = validationReport.isFullyValid() ? "Order updated successfully with all items validated."
                    : "Order updated with partial validation. Some items were not found.";

            return ResponseEntity.ok(buildValidationResponse(holding, validationReport, "MERGED", message));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Order does not exist or has already been collected");
        }
    }

    @DeleteMapping(value = "/{externalOrderNumber}")
    public ResponseEntity<?> deleteExternalOrder(@PathVariable("externalOrderNumber") String externalOrderNumber) {
        try {
            if (incomingOrderService.getOrderByExternalOrderNumber(externalOrderNumber).isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Order doesn't exist.");
            }

            incomingOrderService.deleteHoldingByExternalOrderNumber(externalOrderNumber);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Order can't be deleted after collection");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Minimal response wrapper for the external order endpoint.
     */
    public static class ExternalOrderReceivedResponse {
        private String externalOrderNumber;
        private Integer holdingId;
        private String status;
        private String message;
        private String accessionNumber;
        // Validation details
        private boolean patientValid;
        private String patientRejectionReason;
        private List<ValidationResult> validTests;
        private List<ValidationResult> invalidTests;
        private List<ValidationResult> validPanels;
        private List<ValidationResult> invalidPanels;
        private int totalTestsReceived;
        private int totalPanelsReceived;

        public String getExternalOrderNumber() {
            return externalOrderNumber;
        }

        public void setExternalOrderNumber(String externalOrderNumber) {
            this.externalOrderNumber = externalOrderNumber;
        }

        public Integer getHoldingId() {
            return holdingId;
        }

        public void setHoldingId(Integer holdingId) {
            this.holdingId = holdingId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getAccessionNumber() {
            return accessionNumber;
        }

        public void setAccessionNumber(String accessionNumber) {
            this.accessionNumber = accessionNumber;
        }

        public boolean isPatientValid() {
            return patientValid;
        }

        public void setPatientValid(boolean patientValid) {
            this.patientValid = patientValid;
        }

        public String getPatientRejectionReason() {
            return patientRejectionReason;
        }

        public void setPatientRejectionReason(String patientRejectionReason) {
            this.patientRejectionReason = patientRejectionReason;
        }

        public List<ValidationResult> getValidTests() {
            return validTests;
        }

        public void setValidTests(List<ValidationResult> validTests) {
            this.validTests = validTests;
        }

        public List<ValidationResult> getInvalidTests() {
            return invalidTests;
        }

        public void setInvalidTests(List<ValidationResult> invalidTests) {
            this.invalidTests = invalidTests;
        }

        public List<ValidationResult> getValidPanels() {
            return validPanels;
        }

        public void setValidPanels(List<ValidationResult> validPanels) {
            this.validPanels = validPanels;
        }

        public List<ValidationResult> getInvalidPanels() {
            return invalidPanels;
        }

        public void setInvalidPanels(List<ValidationResult> invalidPanels) {
            this.invalidPanels = invalidPanels;
        }

        public int getTotalTestsReceived() {
            return totalTestsReceived;
        }

        public void setTotalTestsReceived(int totalTestsReceived) {
            this.totalTestsReceived = totalTestsReceived;
        }

        public int getTotalPanelsReceived() {
            return totalPanelsReceived;
        }

        public void setTotalPanelsReceived(int totalPanelsReceived) {
            this.totalPanelsReceived = totalPanelsReceived;
        }
    }

    /**
     * Build validation response from validation report.
     */
    private ExternalOrderReceivedResponse buildValidationResponse(IncomingOrder holding, ValidationReport report,
            String status, String message) {
        ExternalOrderReceivedResponse response = new ExternalOrderReceivedResponse();
        if (holding != null) {
            response.setExternalOrderNumber(holding.getExternalOrderNumber());
            response.setHoldingId(holding.getId());
        } else if (report != null && report.getPatientGuid() != null) {
            // For rejected orders, still set the order number if available
        }
        response.setStatus(status);
        response.setMessage(message);
        if (report != null) {
            response.setPatientValid(report.isPatientValid());
            response.setPatientRejectionReason(report.getPatientRejectionReason());
            response.setValidTests(report.getValidTests());
            response.setInvalidTests(report.getInvalidTests());
            response.setValidPanels(report.getValidPanels());
            response.setInvalidPanels(report.getInvalidPanels());
            response.setTotalTestsReceived(report.getTotalTestsReceived());
            response.setTotalPanelsReceived(report.getTotalPanelsReceived());
        }
        return response;
    }
}
