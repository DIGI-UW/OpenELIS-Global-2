package org.openelisglobal.tb.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.tb.service.TbIdentificationResultService;
import org.openelisglobal.tb.service.TbSmearResultService;
import org.openelisglobal.tb.valueholder.TbEnums.AfbResult;
import org.openelisglobal.tb.valueholder.TbEnums.IdentificationResult;
import org.openelisglobal.tb.valueholder.TbIdentificationResult;
import org.openelisglobal.tb.valueholder.TbSmearResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for TB diagnostic test results (smear and identification).
 */
@RestController
@RequestMapping(value = "/rest/tb/diagnostics")
public class TbDiagnosticsRestController extends BaseRestController {

    @Autowired
    private TbSmearResultService tbSmearResultService;

    @Autowired
    private TbIdentificationResultService tbIdentificationResultService;

    // ==================== Smear Result Endpoints ====================

    /**
     * Get smear result by sample item ID.
     */
    @GetMapping(value = "/smear/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TbSmearResult> getSmearResult(@PathVariable String sampleItemId) {
        Optional<TbSmearResult> result = tbSmearResultService.findBySampleItemId(sampleItemId);
        return result.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get smear results by AFB grading.
     */
    @GetMapping(value = "/smear/by-afb/{afbResult}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbSmearResult>> getSmearResultsByAfb(@PathVariable AfbResult afbResult) {
        List<TbSmearResult> results = tbSmearResultService.findByAfbResult(afbResult);
        return ResponseEntity.ok(results);
    }

    /**
     * Get all positive smear results.
     */
    @GetMapping(value = "/smear/positive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbSmearResult>> getPositiveSmearResults() {
        List<TbSmearResult> results = tbSmearResultService.findPositiveResults();
        return ResponseEntity.ok(results);
    }

    /**
     * Create a new smear result.
     */
    @PostMapping(value = "/smear", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createSmearResult(@RequestBody TbSmearResult smearResult,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        smearResult.setSysUserId(sysUserId);
        smearResult.setResultDate(new Timestamp(System.currentTimeMillis()));
        Integer id = tbSmearResultService.insert(smearResult);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "TB smear result created");
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing smear result.
     */
    @PutMapping(value = "/smear/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateSmearResult(@PathVariable Integer id,
            @RequestBody TbSmearResult smearResult, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        TbSmearResult existing = tbSmearResultService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        smearResult.setId(id);
        smearResult.setSysUserId(sysUserId);
        tbSmearResultService.update(smearResult);

        return ResponseEntity.ok(Map.of("message", "TB smear result updated"));
    }

    // ==================== Identification Result Endpoints ====================

    /**
     * Get identification result by sample item ID.
     */
    @GetMapping(value = "/identification/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TbIdentificationResult> getIdentificationResult(@PathVariable String sampleItemId) {
        Optional<TbIdentificationResult> result = tbIdentificationResultService.findBySampleItemId(sampleItemId);
        return result.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get identification results by result type.
     */
    @GetMapping(value = "/identification/by-result/{result}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbIdentificationResult>> getIdentificationResultsByType(
            @PathVariable IdentificationResult result) {
        List<TbIdentificationResult> results = tbIdentificationResultService.findByResult(result);
        return ResponseEntity.ok(results);
    }

    /**
     * Get all MTB positive samples.
     */
    @GetMapping(value = "/identification/mtb-positive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbIdentificationResult>> getMtbPositiveResults() {
        List<TbIdentificationResult> results = tbIdentificationResultService.findMtbPositive();
        return ResponseEntity.ok(results);
    }

    /**
     * Create a new identification result.
     */
    @PostMapping(value = "/identification", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createIdentificationResult(
            @RequestBody TbIdentificationResult identificationResult, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        identificationResult.setSysUserId(sysUserId);
        identificationResult.setResultDate(new Timestamp(System.currentTimeMillis()));
        Integer id = tbIdentificationResultService.insert(identificationResult);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "TB identification result created");
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing identification result.
     */
    @PutMapping(value = "/identification/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateIdentificationResult(@PathVariable Integer id,
            @RequestBody TbIdentificationResult identificationResult, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        TbIdentificationResult existing = tbIdentificationResultService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        identificationResult.setId(id);
        identificationResult.setSysUserId(sysUserId);
        tbIdentificationResultService.update(identificationResult);

        return ResponseEntity.ok(Map.of("message", "TB identification result updated"));
    }

    // ==================== Statistics Endpoints ====================

    /**
     * Get diagnostic statistics for dashboard.
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDiagnosticStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Smear statistics
        stats.put("smearNegativeCount", tbSmearResultService.countByAfbResult(AfbResult.NEGATIVE));
        stats.put("smearScantyCount", tbSmearResultService.countByAfbResult(AfbResult.SCANTY));
        stats.put("smear1PlusCount", tbSmearResultService.countByAfbResult(AfbResult.PLUS1));
        stats.put("smear2PlusCount", tbSmearResultService.countByAfbResult(AfbResult.PLUS2));
        stats.put("smear3PlusCount", tbSmearResultService.countByAfbResult(AfbResult.PLUS3));

        // Identification statistics
        stats.put("mtbPositiveCount", tbIdentificationResultService.countByResult(IdentificationResult.MTB));
        stats.put("ntmCount", tbIdentificationResultService.countByResult(IdentificationResult.NTM));
        stats.put("idNegativeCount", tbIdentificationResultService.countByResult(IdentificationResult.NEGATIVE));

        return ResponseEntity.ok(stats);
    }
}
