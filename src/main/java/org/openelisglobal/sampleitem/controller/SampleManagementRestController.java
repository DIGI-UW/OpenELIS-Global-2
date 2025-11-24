/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.sampleitem.controller;

import jakarta.validation.constraints.NotBlank;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;
import org.openelisglobal.sampleitem.service.SampleManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Sample Management operations.
 *
 * <p>
 * Provides RESTful endpoints for sample item search, aliquoting, and test
 * management. All endpoints return JSON responses. Error responses follow
 * standard HTTP status codes.
 *
 * <p>
 * Related: Feature 001-sample-management
 *
 * @see SampleManagementService
 */
@RestController
@RequestMapping("/rest/sample-management")
@Validated
public class SampleManagementRestController extends BaseRestController {

    @Autowired
    private SampleManagementService sampleManagementService;

    /**
     * Search for sample items by accession number.
     *
     * <p>
     * Returns all sample items associated with the given accession number,
     * including parent-child aliquot hierarchy. Optionally includes ordered tests
     * for each sample item.
     *
     * <p>
     * Example: GET
     * /rest/sample-management/search?accessionNumber=20231201-001&includeTests=true
     *
     * @param accessionNumber the sample accession number to search for (required)
     * @param includeTests    if true, loads ordered tests for each sample item
     *                        (default: false)
     * @return SearchSamplesResponse with 200 OK, or empty results if not found
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<SearchSamplesResponse> searchSamplesByAccessionNumber(
            @RequestParam @NotBlank(message = "Accession number is required") String accessionNumber,
            @RequestParam(defaultValue = "false") boolean includeTests) {

        try {
            LogEvent.logInfo(this.getClass().getName(), "searchSamplesByAccessionNumber",
                    "Searching for samples with accession number: " + accessionNumber);

            SearchSamplesResponse response = sampleManagementService.searchByAccessionNumber(accessionNumber,
                    includeTests);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "searchSamplesByAccessionNumber",
                    "Error searching for samples: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Exception handler for validation errors (e.g., missing required parameters).
     *
     * @param e the exception
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(jakarta.validation.ConstraintViolationException e) {
        LogEvent.logWarn(this.getClass().getName(), "handleValidationException", e.getMessage());

        ErrorResponse error = new ErrorResponse("Validation Error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Exception handler for illegal argument errors.
     *
     * @param e the exception
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        LogEvent.logWarn(this.getClass().getName(), "handleIllegalArgumentException", e.getMessage());

        ErrorResponse error = new ErrorResponse("Invalid Request", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Exception handler for general errors.
     *
     * @param e the exception
     * @return error response with 500 INTERNAL SERVER ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        LogEvent.logError(this.getClass().getName(), "handleGeneralException", "Unexpected error: " + e.getMessage());

        ErrorResponse error = new ErrorResponse("Internal Server Error",
                "An unexpected error occurred. Please contact support if the problem persists.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Error response object for REST API errors.
     */
    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
