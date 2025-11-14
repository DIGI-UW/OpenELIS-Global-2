package org.openelisglobal.storage.controller;

import org.openelisglobal.storage.service.BarcodeValidationRequest;
import org.openelisglobal.storage.service.BarcodeValidationResponse;
import org.openelisglobal.storage.service.BarcodeValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for barcode validation Provides endpoint for validating
 * storage location barcodes
 */
@RestController
@RequestMapping("/rest/storage/barcode")
public class BarcodeValidationRestController {

    @Autowired
    private BarcodeValidationService barcodeValidationService;

    /**
     * Validate a storage location barcode
     *
     * POST /rest/storage/barcode/validate
     *
     * @param request BarcodeValidationRequest with barcode string
     * @return BarcodeValidationResponse with validation result
     */
    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BarcodeValidationResponse> validateBarcode(@RequestBody BarcodeValidationRequest request) {
        // Validate request
        if (request == null || request.getBarcode() == null || request.getBarcode().trim().isEmpty()) {
            BarcodeValidationResponse errorResponse = new BarcodeValidationResponse();
            errorResponse.setValid(false);
            errorResponse.setFailedStep("REQUEST_VALIDATION");
            errorResponse.setErrorMessage("Barcode is required");
            return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
        }

        // Perform validation
        BarcodeValidationResponse response = barcodeValidationService.validateBarcode(request.getBarcode());

        // Return 200 OK with validation result (both valid and invalid results return
        // 200)
        return ResponseEntity.ok(response);
    }
}
