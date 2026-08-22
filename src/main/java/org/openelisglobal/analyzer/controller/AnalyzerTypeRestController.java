package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerTypeCatalogView;
import org.openelisglobal.analyzer.service.BridgeProfileCatalogException;
import org.openelisglobal.analyzer.service.BridgeProfileManagementException;
import org.openelisglobal.analyzer.service.BridgeProfileManagementService;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer-types")
@PreAuthorize("hasAnyRole('ANALYSER_IMPORT', 'ADMIN')")
public class AnalyzerTypeRestController extends BaseRestController {

    private final AnalyzerTypeCatalogService catalogService;
    private final BridgeProfileManagementService managementService;
    private final AnalyzerMappingCatalogService mappingCatalogService;

    @Autowired
    public AnalyzerTypeRestController(AnalyzerTypeCatalogService catalogService,
            BridgeProfileManagementService managementService, AnalyzerMappingCatalogService mappingCatalogService) {
        this.catalogService = catalogService;
        this.managementService = managementService;
        this.mappingCatalogService = mappingCatalogService;
    }

    @GetMapping
    public ResponseEntity<AnalyzerTypeCatalogView> getAnalyzerTypes() {
        return ResponseEntity.ok(catalogService.getCatalog());
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<AnalyzerTypeCatalogView.TypeSummary> getAnalyzerType(@PathVariable String profileId,
            @RequestParam int revision) {
        return ResponseEntity.ok(catalogService.getType(profileId, revision));
    }

    @GetMapping("/mapping-catalog/tests")
    public ResponseEntity<List<AnalyzerMappingCatalogService.TestOption>> searchMappingTests(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(mappingCatalogService.searchActiveTests(search));
    }

    @GetMapping("/mapping-catalog/tests/{testId}/result-options")
    public ResponseEntity<List<AnalyzerMappingCatalogService.ResultOption>> getMappingResultOptions(
            @PathVariable String testId) {
        return ResponseEntity.ok(mappingCatalogService.getActiveResultOptions(testId));
    }

    @PostMapping("/drafts")
    public ResponseEntity<JsonNode> createDraft(@RequestBody CreateDraftRequest request,
            HttpServletRequest httpRequest) {
        JsonNode created = managementService.createDraft(request == null ? null : request.displayName(),
                getSysUserId(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<JsonNode> getDraft(@PathVariable String draftId) {
        return ResponseEntity.ok(managementService.getDraft(draftId));
    }

    @PutMapping("/drafts/{draftId}")
    public ResponseEntity<JsonNode> updateDraft(@PathVariable String draftId,
            @RequestBody ProfileMutationRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(managementService.updateDraft(draftId, request == null ? null : request.profile(),
                getSysUserId(httpRequest)));
    }

    @PostMapping("/drafts/{draftId}/publish")
    public ResponseEntity<JsonNode> publishDraft(@PathVariable String draftId, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(managementService.publishDraft(draftId, getSysUserId(httpRequest)));
    }

    @PostMapping("/{profileId}/update")
    public ResponseEntity<JsonNode> updateShared(@PathVariable String profileId,
            @RequestBody SourceRevisionRequest request, HttpServletRequest httpRequest) {
        JsonNode draft = managementService.updateShared(profileId, request.sourceRevision(), getSysUserId(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(draft);
    }

    @PostMapping("/{profileId}/duplicate")
    public ResponseEntity<JsonNode> duplicate(@PathVariable String profileId,
            @RequestBody DuplicateProfileRequest request, HttpServletRequest httpRequest) {
        JsonNode duplicated = managementService.duplicate(profileId, request.sourceRevision(), request.displayName(),
                getSysUserId(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicated);
    }

    @PostMapping("/{profileId}/deactivate")
    public ResponseEntity<JsonNode> deactivate(@PathVariable String profileId, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(managementService.deactivate(profileId, getSysUserId(httpRequest)));
    }

    @PostMapping("/{profileId}/reactivate")
    public ResponseEntity<JsonNode> reactivate(@PathVariable String profileId, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(managementService.reactivate(profileId, getSysUserId(httpRequest)));
    }

    @GetMapping("/{profileId}/history")
    public ResponseEntity<JsonNode> history(@PathVariable String profileId) {
        return ResponseEntity.ok(managementService.history(profileId));
    }

    @ExceptionHandler(BridgeProfileManagementException.class)
    public ResponseEntity<ErrorResponse> handleProfileManagementError(BridgeProfileManagementException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatus());
        return ResponseEntity.status(status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(BridgeProfileCatalogException.class)
    public ResponseEntity<ErrorResponse> handleProfileCatalogError(BridgeProfileCatalogException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ErrorResponse(exception.getMessage()));
    }

    public record ProfileMutationRequest(JsonNode profile) {
    }

    public record CreateDraftRequest(String displayName) {
    }

    public record SourceRevisionRequest(int sourceRevision) {
    }

    public record DuplicateProfileRequest(int sourceRevision, String displayName) {
    }

    public record ErrorResponse(String error) {
    }
}
