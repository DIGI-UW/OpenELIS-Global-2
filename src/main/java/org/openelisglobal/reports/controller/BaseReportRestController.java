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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.controller;

import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base REST Controller for reporting endpoints.
 *
 * <p>
 * Establishes the `/rest/reports` namespace for all reporting-related APIs.
 * Specific reporting endpoints (patient reports, test reports, etc.) are
 * implemented in specialized controllers extending this base class.
 *
 * <p>
 * Provides foundation for ad-hoc reporting with support for: - Report
 * definition discovery and metadata - Report data retrieval and filtering -
 * Multi-format export (JSON, CSV, PDF) - Report execution with parameters
 */
@RestController
@RequestMapping("/rest/reports")
public class BaseReportRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(BaseReportRestController.class);

    /**
     * Health check endpoint to verify the reports namespace is available.
     *
     * @return 200 OK response with namespace info
     */
    @GetMapping
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("namespace", "/rest/reports");
        response.put("message", "Reports API namespace is available");
        return ResponseEntity.ok(response);
    }

    /**
     * Get report API version and capabilities.
     *
     * @return API version and supported features
     */
    @GetMapping("/version")
    public ResponseEntity<?> getVersion() {
        Map<String, Object> response = new HashMap<>();
        response.put("version", "1.0.0");
        response.put("capabilities",
                new String[] { "definitions", "execute", "export-json", "export-csv", "export-pdf" });
        response.put("availableFormats", new String[] { "json", "csv", "pdf" });
        return ResponseEntity.ok(response);
    }

    /**
     * Execute a report with optional parameters and return data in requested
     * format.
     *
     * <p>
     * This is the primary endpoint for generating ad-hoc reports.
     *
     * @param reportId report definition ID
     * @param format   output format (json, csv, pdf) - defaults to json
     * @param limit    maximum rows to return
     * @param offset   pagination offset
     * @return Report data in requested format
     */
    @GetMapping("/formats")
    public ResponseEntity<?> getAvailableFormats() {
        Map<String, Object> response = new HashMap<>();
        response.put("formats", new String[] { "json", "csv", "pdf" });
        response.put("description",
                new Object[] {
                        Map.of("format", "json", "contentType", "application/json", "useCase",
                                "API consumption, web UI"),
                        Map.of("format", "csv", "contentType", "text/csv", "useCase", "Excel, data analysis"),
                        Map.of("format", "pdf", "contentType", "application/pdf", "useCase", "Printing, archival") });

        return ResponseEntity.ok(response);
    }

    /**
     * Get available report categories for filtering and discovery.
     *
     * @return list of report categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getReportCategories() {
        Map<String, Object> response = new HashMap<>();
        response.put("categories", new String[] { "patient", "result", "lab-activity", "quality", "inventory" });
        response.put("message", "Use category to filter reports");
        return ResponseEntity.ok(response);
    }
}
