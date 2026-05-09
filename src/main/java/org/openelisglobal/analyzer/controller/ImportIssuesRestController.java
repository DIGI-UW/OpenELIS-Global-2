package org.openelisglobal.analyzer.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Import Issues admin dashboard endpoint.
 *
 * <p>
 * Surfaces staging rows that the analyzer-import pipeline flagged as
 * un-actionable: rows written with {@code import_issue_reason} populated
 * because the incoming Host Test Code didn't resolve against any
 * {@code analyzer_test_map} row, a cartridge code arrived where an analyte code
 * was expected, or a dict value didn't match any {@code dict_entry}. Without
 * this panel those rows drain off the Ready-for-Validation screen silently and
 * operators have to read Tomcat logs to diagnose misconfigured host catalogs.
 * </p>
 *
 * <p>
 * Pairs with the bridge's {@code /admin/rejected-bundles} endpoint which
 * surfaces bundles OE never even accepted (auth failures, malformed FHIR, retry
 * exhaustion). The frontend queries both independently; the
 * {@code bridgeRejectedBundlesUrl} field in the response tells the panel where
 * to fetch the bridge-side list.
 * </p>
 */
@RestController
@RequestMapping("/rest/analyzer")
@PreAuthorize("hasRole('ADMIN')")
public class ImportIssuesRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(ImportIssuesRestController.class);

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    @GetMapping("/import-issues")
    public ResponseEntity<Map<String, Object>> getImportIssues(
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        try {
            int safeLimit = Math.max(1, Math.min(limit == null ? DEFAULT_LIMIT : limit, MAX_LIMIT));
            List<AnalyzerResults> rows = analyzerResultsService.findWithImportIssues(safeLimit);

            List<Map<String, Object>> rowJson = rows.stream().map(this::rowToMap).toList();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", rowJson.size());
            data.put("limit", safeLimit);
            data.put("rows", rowJson);
            // Frontend hint — the bridge exposes its own rejection store at this
            // path (same-origin via proxy or configured bridge URL); keep as a
            // relative value so the panel can compose the final URL.
            data.put("bridgeRejectedBundlesUrl", "/admin/rejected-bundles");

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving import issues", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    private Map<String, Object> rowToMap(AnalyzerResults r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("analyzerId", r.getAnalyzerId());
        m.put("accessionNumber", r.getAccessionNumber());
        m.put("testName", r.getTestName());
        m.put("result", r.getResult());
        m.put("units", r.getUnits());
        m.put("resultType", r.getResultType());
        m.put("testId", r.getTestId());
        m.put("readOnly", r.isReadOnly());
        m.put("importIssueReason", r.getImportIssueReason());
        m.put("completeDate", r.getCompleteDate() != null ? r.getCompleteDate().toInstant().toString() : null);
        m.put("lastupdated", r.getLastupdated() != null ? r.getLastupdated().toInstant().toString() : null);
        return m;
    }
}
