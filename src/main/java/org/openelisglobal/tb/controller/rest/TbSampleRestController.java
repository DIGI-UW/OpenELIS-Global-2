package org.openelisglobal.tb.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.tb.service.TbCultureReadingService;
import org.openelisglobal.tb.service.TbQualityCheckService;
import org.openelisglobal.tb.service.TbSampleRegistrationService;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;
import org.openelisglobal.tb.valueholder.TbEnums.QcResult;
import org.openelisglobal.tb.valueholder.TbQualityCheck;
import org.openelisglobal.tb.valueholder.TbSampleRegistration;
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
 * REST controller for TB sample registration and workflow operations.
 */
@RestController
@RequestMapping(value = "/rest/tb")
public class TbSampleRestController extends BaseRestController {

    @Autowired
    private TbSampleRegistrationService tbSampleRegistrationService;

    @Autowired
    private TbQualityCheckService tbQualityCheckService;

    @Autowired
    private TbCultureReadingService tbCultureReadingService;

    // ==================== Sample Registration Endpoints ====================

    /**
     * Get TB registration by sample item ID.
     */
    @GetMapping(value = "/registration/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TbSampleRegistration> getRegistration(@PathVariable String sampleItemId) {
        Optional<TbSampleRegistration> registration = tbSampleRegistrationService.findBySampleItemId(sampleItemId);
        return registration.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all registrations by document number.
     */
    @GetMapping(value = "/registration/by-document/{documentNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbSampleRegistration>> getRegistrationsByDocument(@PathVariable String documentNumber) {
        List<TbSampleRegistration> registrations = tbSampleRegistrationService.findByDocumentNumber(documentNumber);
        return ResponseEntity.ok(registrations);
    }

    /**
     * Create a new TB sample registration.
     */
    @PostMapping(value = "/registration", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createRegistration(@RequestBody TbSampleRegistration registration,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        registration.setSysUserId(sysUserId);
        registration.setDateCreated(new Timestamp(System.currentTimeMillis()));
        Integer id = tbSampleRegistrationService.insert(registration);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "TB sample registration created");
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing TB sample registration.
     */
    @PutMapping(value = "/registration/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateRegistration(@PathVariable Integer id,
            @RequestBody TbSampleRegistration registration, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        TbSampleRegistration existing = tbSampleRegistrationService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        registration.setId(id);
        registration.setSysUserId(sysUserId);
        tbSampleRegistrationService.update(registration);

        return ResponseEntity.ok(Map.of("message", "TB sample registration updated"));
    }

    // ==================== Quality Check Endpoints ====================

    /**
     * Get QC result by sample item ID.
     */
    @GetMapping(value = "/qc/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TbQualityCheck> getQualityCheck(@PathVariable String sampleItemId) {
        Optional<TbQualityCheck> qc = tbQualityCheckService.findBySampleItemId(sampleItemId);
        return qc.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get QC results by overall result.
     */
    @GetMapping(value = "/qc/by-result/{result}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbQualityCheck>> getQualityChecksByResult(@PathVariable QcResult result) {
        List<TbQualityCheck> qcList = tbQualityCheckService.findByOverallResult(result);
        return ResponseEntity.ok(qcList);
    }

    /**
     * Create a new QC result.
     */
    @PostMapping(value = "/qc", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createQualityCheck(@RequestBody TbQualityCheck qualityCheck,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        qualityCheck.setSysUserId(sysUserId);
        qualityCheck.setQcDate(new Timestamp(System.currentTimeMillis()));
        Integer id = tbQualityCheckService.insert(qualityCheck);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "TB quality check created");
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing QC result.
     */
    @PutMapping(value = "/qc/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateQualityCheck(@PathVariable Integer id,
            @RequestBody TbQualityCheck qualityCheck, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        TbQualityCheck existing = tbQualityCheckService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        qualityCheck.setId(id);
        qualityCheck.setSysUserId(sysUserId);
        tbQualityCheckService.update(qualityCheck);

        return ResponseEntity.ok(Map.of("message", "TB quality check updated"));
    }

    // ==================== Culture Reading Endpoints ====================

    /**
     * Get all culture readings for a sample.
     */
    @GetMapping(value = "/culture/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbCultureReading>> getCultureReadings(@PathVariable String sampleItemId) {
        List<TbCultureReading> readings = tbCultureReadingService.findBySampleItemId(sampleItemId);
        return ResponseEntity.ok(readings);
    }

    /**
     * Get a specific week's culture reading.
     */
    @GetMapping(value = "/culture/{sampleItemId}/week/{weekNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TbCultureReading> getCultureReadingByWeek(@PathVariable String sampleItemId,
            @PathVariable Integer weekNumber) {
        Optional<TbCultureReading> reading = tbCultureReadingService.findBySampleItemIdAndWeek(sampleItemId,
                weekNumber);
        return reading.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get samples by growth observation.
     */
    @GetMapping(value = "/culture/by-observation/{observation}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbCultureReading>> getCultureReadingsByObservation(
            @PathVariable GrowthObservation observation) {
        List<TbCultureReading> readings = tbCultureReadingService.findByGrowthObservation(observation);
        return ResponseEntity.ok(readings);
    }

    /**
     * Create a new culture reading.
     */
    @PostMapping(value = "/culture", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createCultureReading(@RequestBody TbCultureReading reading,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        reading.setSysUserId(sysUserId);
        reading.setReadingDate(new Timestamp(System.currentTimeMillis()));
        Integer id = tbCultureReadingService.insert(reading);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("message", "TB culture reading created");
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing culture reading.
     */
    @PutMapping(value = "/culture/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> updateCultureReading(@PathVariable Integer id,
            @RequestBody TbCultureReading reading, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        TbCultureReading existing = tbCultureReadingService.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        reading.setId(id);
        reading.setSysUserId(sysUserId);
        tbCultureReadingService.update(reading);

        return ResponseEntity.ok(Map.of("message", "TB culture reading updated"));
    }

    // ==================== Dashboard/Statistics Endpoints ====================

    /**
     * Get TB workflow statistics for dashboard.
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // QC statistics
        stats.put("qcPassCount", tbQualityCheckService.countByOverallResult(QcResult.PASS));
        stats.put("qcFailCount", tbQualityCheckService.countByOverallResult(QcResult.FAIL_DISCARD));

        // Culture statistics
        stats.put("growthDetectedCount", tbCultureReadingService.countGrowthDetected());

        return ResponseEntity.ok(stats);
    }
}
