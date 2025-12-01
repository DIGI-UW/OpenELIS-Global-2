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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.sampleitem.dto.AddTestsResponse;
import org.openelisglobal.sampleitem.dto.CancelTestResponse;
import org.openelisglobal.sampleitem.dto.CreateAliquotResponse;
import org.openelisglobal.sampleitem.dto.SearchSamplesResponse;
import org.openelisglobal.sampleitem.form.AddTestsForm;
import org.openelisglobal.sampleitem.form.CancelTestForm;
import org.openelisglobal.sampleitem.form.CreateAliquotForm;
import org.openelisglobal.sampleitem.service.SampleManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * Create an aliquot from a parent sample item.
     *
     * <p>
     * Creates a new child sample item by transferring a specified quantity from the
     * parent. The parent's remaining quantity is decreased automatically. The new
     * aliquot receives an external ID with .{n} suffix.
     *
     * <p>
     * Example: POST /rest/sample-management/aliquot Body: {"parentSampleItemId":
     * "123", "quantityToTransfer": 5.0, "notes": "For PCR testing"}
     *
     * @param form the aliquot creation request with parentSampleItemId and
     *             quantityToTransfer
     * @return CreateAliquotResponse with 201 CREATED on success
     * @throws IllegalArgumentException if validation fails (400 BAD REQUEST)
     * @throws IllegalStateException    if parent has no remaining quantity (400 BAD
     *                                  REQUEST)
     */
    @PostMapping(value = "/aliquot", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CreateAliquotResponse> createAliquot(@Valid @RequestBody CreateAliquotForm form,
            HttpServletRequest request) {

        try {
            String sysUserId = getSysUserId(request);
            if (sysUserId == null) {
                throw new IllegalStateException("User not authenticated");
            }

            LogEvent.logInfo(this.getClass().getName(), "createAliquot", "Creating aliquot from parent: "
                    + form.getParentSampleItemId() + ", quantity: " + form.getQuantityToTransfer());

            CreateAliquotResponse response = sampleManagementService.createAliquot(form, sysUserId);

            LogEvent.logInfo(this.getClass().getName(), "createAliquot",
                    "Aliquot created successfully: " + response.getAliquot().getExternalId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            LogEvent.logWarn(this.getClass().getName(), "createAliquot",
                    "Validation error creating aliquot: " + e.getMessage());
            throw e;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "createAliquot", "Error creating aliquot: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Add tests to one or more sample items.
     *
     * <p>
     * Adds the specified tests to the specified sample items. Automatically detects
     * and skips duplicates (tests already ordered for a sample item). Returns a
     * detailed report of added and skipped tests per sample item.
     *
     * <p>
     * Example: POST /rest/sample-management/add-tests Body: {"sampleItemIds":
     * ["123", "456"], "testIds": ["789", "012"]}
     *
     * @param form    the add tests request containing sample item IDs and test IDs
     * @param request the HTTP request for authentication
     * @return AddTestsResponse with success count and detailed results
     * @throws IllegalArgumentException if sample item or test not found (400 BAD
     *                                  REQUEST)
     */
    @PostMapping(value = "/add-tests", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AddTestsResponse> addTestsToSamples(@Valid @RequestBody AddTestsForm form,
            HttpServletRequest request) {

        try {
            String sysUserId = getSysUserId(request);
            if (sysUserId == null) {
                throw new IllegalStateException("User not authenticated");
            }

            LogEvent.logInfo(this.getClass().getName(), "addTestsToSamples",
                    String.format("Adding %d test(s) to %d sample item(s)", form.getTestIds().size(),
                            form.getSampleItemIds().size()));

            AddTestsResponse response = sampleManagementService.addTestsToSamples(form, sysUserId);

            LogEvent.logInfo(this.getClass().getName(), "addTestsToSamples",
                    String.format("Successfully added %d test(s)", response.getSuccessCount()));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            LogEvent.logWarn(this.getClass().getName(), "addTestsToSamples",
                    "Validation error adding tests: " + e.getMessage());
            throw e;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "addTestsToSamples", "Error adding tests: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Cancel/remove a test from a sample item.
     *
     * <p>
     * Sets the analysis status to "Canceled" for the specified analysis. Only tests
     * that have not been completed or finalized can be cancelled.
     *
     * <p>
     * Example: POST /rest/sample-management/cancel-test Body: {"analysisId": "123",
     * "sampleItemId": "456"}
     *
     * @param form    the cancel test request containing analysis ID and sample item
     *                ID
     * @param request the HTTP request for authentication
     * @return CancelTestResponse with cancellation result
     * @throws IllegalArgumentException if analysis not found or doesn't belong to
     *                                  sample item (400 BAD REQUEST)
     * @throws IllegalStateException    if analysis cannot be cancelled (400 BAD
     *                                  REQUEST)
     */
    @PostMapping(value = "/cancel-test", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CancelTestResponse> cancelTest(@Valid @RequestBody CancelTestForm form,
            HttpServletRequest request) {

        try {
            String sysUserId = getSysUserId(request);
            if (sysUserId == null) {
                throw new IllegalStateException("User not authenticated");
            }

            LogEvent.logInfo(this.getClass().getName(), "cancelTest",
                    String.format("Cancelling test - analysisId: %s, sampleItemId: %s", form.getAnalysisId(),
                            form.getSampleItemId()));

            CancelTestResponse response = sampleManagementService.cancelTest(form, sysUserId);

            LogEvent.logInfo(this.getClass().getName(), "cancelTest",
                    String.format("Test cancelled successfully: %s", response.getTestName()));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            LogEvent.logWarn(this.getClass().getName(), "cancelTest",
                    "Validation error cancelling test: " + e.getMessage());
            throw e;

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "cancelTest", "Error cancelling test: " + e.getMessage());
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
     * Exception handler for illegal state errors (e.g., no remaining quantity).
     *
     * @param e the exception
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        LogEvent.logWarn(this.getClass().getName(), "handleIllegalStateException", e.getMessage());

        ErrorResponse error = new ErrorResponse("Invalid State", e.getMessage());
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
