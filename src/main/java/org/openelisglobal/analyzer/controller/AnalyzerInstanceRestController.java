package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.form.AnalyzerSiteBindingSelectionRequest;
import org.openelisglobal.analyzer.service.AnalyzerInstanceService;
import org.openelisglobal.analyzer.service.AnalyzerInstanceState;
import org.openelisglobal.analyzer.service.AnalyzerInstanceView;
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
@RequestMapping("/rest/analyzer/analyzers")
@PreAuthorize("hasAnyRole('ANALYSER_IMPORT', 'ADMIN')")
public class AnalyzerInstanceRestController extends BaseRestController {

    private final AnalyzerInstanceService analyzerInstanceService;

    @Autowired
    public AnalyzerInstanceRestController(AnalyzerInstanceService analyzerInstanceService) {
        this.analyzerInstanceService = analyzerInstanceService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody AnalyzerInstanceRequest input,
            HttpServletRequest request) {
        AnalyzerInstanceView created = analyzerInstanceService.create(input, getSysUserId(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toMap(created));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        String normalizedSearch = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> analyzers = analyzerInstanceService.list().stream()
                .filter(state -> normalizedSearch == null || normalizedSearch.isEmpty()
                        || state.name().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || state.profileId().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .filter(state -> status == null || status.isBlank() || state.status().name().equalsIgnoreCase(status))
                .map(AnalyzerInstanceRestController::toStateMap).toList();
        return ResponseEntity.ok(Map.of("analyzers", analyzers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) {
        return ResponseEntity.ok(toMap(analyzerInstanceService.get(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String id,
            @Valid @RequestBody AnalyzerInstanceRequest input, HttpServletRequest request) {
        return ResponseEntity.ok(toMap(analyzerInstanceService.update(id, input, getSysUserId(request))));
    }

    @PutMapping("/{id}/site-binding")
    public ResponseEntity<Map<String, Object>> selectSiteBindingRevision(@PathVariable String id,
            @Valid @RequestBody AnalyzerSiteBindingSelectionRequest input, HttpServletRequest request) {
        return ResponseEntity.ok(toMap(analyzerInstanceService.selectSiteBindingRevision(id, input.getSiteBindingId(),
                input.getRevision(), input.getBindingFingerprint(), getSysUserId(request))));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "Invalid analyzer request" : exception.getMessage();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private static Map<String, Object> toMap(AnalyzerInstanceView view) {
        Map<String, Object> response = toStateMap(view.state());
        response.put("connected", view.connected());
        if (view.connection() != null) {
            response.put("connection", view.connection());
        }
        if (view.connectionErrorKey() != null) {
            response.put("connectionErrorKey", view.connectionErrorKey());
        }
        return response;
    }

    private static Map<String, Object> toStateMap(AnalyzerInstanceState state) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", state.analyzerId());
        response.put("name", state.name());
        response.put("testUnitIds", state.labUnitIds());
        response.put("profileId", state.profileId());
        response.put("profileRevision", state.profileRevision());
        response.put("profileFingerprint", state.profileFingerprint());
        response.put("bridgeConnectionId", state.bridgeConnectionId());
        response.put("status", state.status().name());
        response.put("heldResultCount", state.heldResultCount());
        response.put("connected", state.bridgeConnectionId() != null);
        return response;
    }
}
