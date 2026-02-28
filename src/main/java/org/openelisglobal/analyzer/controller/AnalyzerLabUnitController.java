package org.openelisglobal.analyzer.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerLabUnitService;
import org.openelisglobal.analyzer.valueholder.AnalyzerLabUnit;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for analyzer lab unit assignments. Endpoints: GET/PUT
 * /analyzers/{id}/lab-units.
 */
@RestController
@RequestMapping("/rest/analyzer")
@PreAuthorize("hasRole('GLOBAL_ADMIN')")
public class AnalyzerLabUnitController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerLabUnitController.class);

    @Autowired
    private AnalyzerLabUnitService analyzerLabUnitService;

    @GetMapping("/analyzers/{analyzerId}/lab-units")
    public ResponseEntity<List<Map<String, Object>>> getLabUnits(@PathVariable String analyzerId) {
        try {
            List<AnalyzerLabUnit> units = analyzerLabUnitService.getLabUnitsByAnalyzerId(analyzerId);
            List<Map<String, Object>> result = new ArrayList<>();
            for (AnalyzerLabUnit u : units) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("labUnitId", u.getLabUnitId());
                result.add(m);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting lab units", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    @PutMapping("/analyzers/{analyzerId}/lab-units")
    public ResponseEntity<Map<String, Object>> replaceLabUnits(@PathVariable String analyzerId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> labUnitIds = (List<String>) request.get("labUnitIds");
            if (labUnitIds == null) {
                labUnitIds = new ArrayList<>();
            }
            analyzerLabUnitService.replaceLabUnitsForAnalyzer(analyzerId, labUnitIds);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error replacing lab units", e);
            // API error payloads are developer-facing; frontend maps to localized UI text.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }
}
