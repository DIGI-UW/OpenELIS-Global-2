package org.openelisglobal.analyzer.controller;

import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerProfileCatalogException;
import org.openelisglobal.analyzer.service.AnalyzerProfileCatalogFilter;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogSummary;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer/types")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyzerTypeCatalogRestController extends BaseRestController {

    private final AnalyzerTypeCatalogService catalogService;

    public AnalyzerTypeCatalogRestController(AnalyzerTypeCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<AnalyzerTypeCatalogSummary> list(@RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String source, @RequestParam(required = false) String status,
            @RequestParam(required = false) String protocol) {
        return catalogService.list(new AnalyzerProfileCatalogFilter(query, source, status, protocol));
    }

    @ExceptionHandler(AnalyzerProfileCatalogException.class)
    public ResponseEntity<Map<String, String>> bridgeUnavailable(AnalyzerProfileCatalogException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", exception.getMessage()));
    }
}
