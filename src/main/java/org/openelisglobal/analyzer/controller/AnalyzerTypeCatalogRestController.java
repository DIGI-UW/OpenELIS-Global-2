package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerProfileCatalogException;
import org.openelisglobal.analyzer.service.AnalyzerProfileCatalogFilter;
import org.openelisglobal.analyzer.service.AnalyzerProfileForkRequest;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogSummary;
import org.openelisglobal.analyzer.service.BridgeProfileCatalogEntry;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/{profileId}")
    public AnalyzerTypeCatalogSummary get(@PathVariable String profileId,
            @RequestParam(required = false) Integer revision) {
        return catalogService.get(profileId, revision);
    }

    @GetMapping("/{profileId}/history")
    public List<BridgeProfileCatalogEntry> history(@PathVariable String profileId) {
        return catalogService.history(profileId);
    }

    @PostMapping("/{profileId}/fork")
    public ResponseEntity<AnalyzerTypeCatalogSummary> fork(@PathVariable String profileId,
            @RequestBody AnalyzerProfileForkRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.fork(profileId, request, getSysUserId(httpRequest)));
    }

    @PostMapping("/{profileId}/deactivate")
    public AnalyzerTypeCatalogSummary deactivate(@PathVariable String profileId, HttpServletRequest request) {
        return catalogService.deactivate(profileId, getSysUserId(request));
    }

    @PostMapping("/{profileId}/reactivate")
    public AnalyzerTypeCatalogSummary reactivate(@PathVariable String profileId, HttpServletRequest request) {
        return catalogService.reactivate(profileId, getSysUserId(request));
    }

    @ExceptionHandler(AnalyzerProfileCatalogException.class)
    public ResponseEntity<Map<String, String>> bridgeUnavailable(AnalyzerProfileCatalogException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", exception.getMessage()));
    }
}
