package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer-types")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyzerTypeRestController extends BaseRestController {

    private final AnalyzerTypeCatalogService catalogService;
    private final BridgeProfileManagementService managementService;

    @Autowired
    public AnalyzerTypeRestController(AnalyzerTypeCatalogService catalogService,
            BridgeProfileManagementService managementService) {
        this.catalogService = catalogService;
        this.managementService = managementService;
    }

    @GetMapping
    public ResponseEntity<AnalyzerTypeCatalogView> getAnalyzerTypes() {
        return ResponseEntity.ok(catalogService.getCatalog());
    }

    @PostMapping
    public ResponseEntity<JsonNode> create(@RequestBody ProfileMutationRequest request,
            HttpServletRequest httpRequest) {
        JsonNode created = managementService.create(request == null ? null : request.profile(),
                getSysUserId(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<JsonNode> updateShared(@PathVariable String profileId,
            @RequestBody ProfileMutationRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(managementService.updateShared(profileId, request == null ? null : request.profile(),
                getSysUserId(httpRequest)));
    }

    @PostMapping("/{profileId}/duplicate")
    public ResponseEntity<JsonNode> duplicate(@PathVariable String profileId,
            @RequestBody DuplicateProfileRequest request, HttpServletRequest httpRequest) {
        JsonNode duplicated = managementService.duplicate(profileId, request.sourceRevision(),
                request.targetProfileId(), request.displayName(), getSysUserId(httpRequest));
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

    public record DuplicateProfileRequest(int sourceRevision, String targetProfileId, String displayName) {
    }

    public record ErrorResponse(String error) {
    }
}
