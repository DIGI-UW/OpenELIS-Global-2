package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerBridgePairingService;
import org.openelisglobal.analyzer.service.BridgePairingException;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer/bridge-pairing")
@PreAuthorize("hasRole('GLOBAL_ADMIN')")
public class AnalyzerBridgePairingRestController extends BaseRestController {

    private final AnalyzerBridgePairingService pairing;

    public AnalyzerBridgePairingRestController(AnalyzerBridgePairingService pairing) {
        this.pairing = pairing;
    }

    public record PairingRequest(String code) {
    }

    @GetMapping
    public ResponseEntity<AnalyzerBridgePairingService.Status> status() {
        return ResponseEntity.ok(pairing.getStatus());
    }

    @PostMapping
    public ResponseEntity<AnalyzerBridgePairingService.Status> pair(@RequestBody PairingRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(pairing.pair(body == null ? null : body.code(), getSysUserId(request)));
    }

    @ExceptionHandler(BridgePairingException.class)
    public ResponseEntity<Map<String, String>> refused(BridgePairingException exception) {
        HttpStatus status = switch (exception.getMessage()) {
        case "analyzer.bridgePairing.error.codeRequired" -> HttpStatus.BAD_REQUEST;
        case "analyzer.bridgePairing.error.notConfigured" -> HttpStatus.SERVICE_UNAVAILABLE;
        case "analyzer.bridgePairing.error.unreachable" -> HttpStatus.BAD_GATEWAY;
        default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(Map.of("errorKey", exception.getMessage()));
    }
}
