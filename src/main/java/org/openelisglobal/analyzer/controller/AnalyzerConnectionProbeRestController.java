package org.openelisglobal.analyzer.controller;

import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerConnectionProbeException;
import org.openelisglobal.analyzer.service.AnalyzerConnectionProbeService;
import org.openelisglobal.analyzer.service.AnalyzerConnectionProbeView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer/analyzers")
public class AnalyzerConnectionProbeRestController {

    private final AnalyzerConnectionProbeService service;

    public AnalyzerConnectionProbeRestController(AnalyzerConnectionProbeService service) {
        this.service = service;
    }

    @PostMapping("/{id}/test-connection")
    public ResponseEntity<AnalyzerConnectionProbeView> testConnection(@PathVariable String id) {
        return ResponseEntity.ok(service.probe(id));
    }

    @ExceptionHandler(AnalyzerConnectionProbeException.class)
    public ResponseEntity<ErrorResponse> handleProbeError(AnalyzerConnectionProbeException exception) {
        return ResponseEntity.status(status(exception.messageKey()))
                .body(new ErrorResponse(exception.messageKey(), exception.messageArgs()));
    }

    private static HttpStatus status(String messageKey) {
        return switch (messageKey) {
        case "analyzer.testConnection.analyzerIdMissing" -> HttpStatus.BAD_REQUEST;
        case "analyzer.testConnection.analyzerNotFound" -> HttpStatus.NOT_FOUND;
        case "analyzer.testConnection.bridge.notConfigured" -> HttpStatus.SERVICE_UNAVAILABLE;
        default -> HttpStatus.BAD_GATEWAY;
        };
    }

    public record ErrorResponse(String messageKey, Map<String, Object> messageArgs) {
    }
}
