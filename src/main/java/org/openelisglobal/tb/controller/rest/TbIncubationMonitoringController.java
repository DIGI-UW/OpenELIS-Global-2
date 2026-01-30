package org.openelisglobal.tb.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.tb.service.TbCultureReadingService;
import org.openelisglobal.tb.service.TbCultureReadingService.IncubationSummary;
import org.openelisglobal.tb.valueholder.TbCultureReading;
import org.openelisglobal.tb.valueholder.TbEnums.CultureResult;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for TB Incubation Monitoring operations. Handles weekly
 * readings, culture result determination, and incubation tracking.
 */
@RestController
@RequestMapping(value = "/rest/tb/incubation")
public class TbIncubationMonitoringController extends BaseRestController {

    @Autowired
    private TbCultureReadingService tbCultureReadingService;

    // ==================== Incubation Sample Endpoints ====================

    /**
     * Get incubating samples (inoculated but no final result) for a specific
     * notebook entry.
     *
     * @param entryId Required notebook entry ID to filter samples by entry
     */
    @GetMapping(value = "/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbCultureReading>> getIncubatingSamples(@RequestParam(required = true) Integer entryId) {
        List<TbCultureReading> samples = tbCultureReadingService.findIncubatingSamplesByEntry(entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Get culture readings for a specific sample.
     */
    @GetMapping(value = "/samples/{sampleItemId}/readings", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbCultureReading>> getSampleReadings(@PathVariable String sampleItemId) {
        List<TbCultureReading> readings = tbCultureReadingService.findBySampleItemId(sampleItemId);
        return ResponseEntity.ok(readings);
    }

    /**
     * Get samples by culture result for a specific notebook entry.
     *
     * @param result  The culture result to filter by
     * @param entryId Required notebook entry ID to filter samples by entry
     */
    @GetMapping(value = "/samples/by-result/{result}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbCultureReading>> getSamplesByResult(@PathVariable CultureResult result,
            @RequestParam(required = true) Integer entryId) {
        List<TbCultureReading> samples = tbCultureReadingService.findByCultureResultAndEntry(result, entryId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Get culture-positive samples for a specific notebook entry.
     *
     * @param entryId Required notebook entry ID to filter samples by entry
     */
    @GetMapping(value = "/samples/positive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<TbCultureReading>> getCulturePositiveSamples(
            @RequestParam(required = true) Integer entryId) {
        List<TbCultureReading> samples = tbCultureReadingService.findByCultureResultAndEntry(CultureResult.POSITIVE,
                entryId);
        return ResponseEntity.ok(samples);
    }

    // ==================== Weekly Reading Endpoints ====================

    /**
     * Record a weekly reading for an incubating sample.
     */
    @PostMapping(value = "/reading", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> recordReading(@RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        Integer cultureReadingId = parseInteger(body.get("cultureReadingId"));
        Integer weekNumber = parseInteger(body.get("weekNumber"));
        GrowthObservation observation = GrowthObservation.valueOf((String) body.get("observation"));
        String notes = (String) body.get("notes");

        TbCultureReading reading = tbCultureReadingService.recordReading(cultureReadingId, weekNumber, observation,
                notes, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", reading.getId());
        response.put("weekNumber", weekNumber);
        response.put("observation", observation.name());
        response.put("message", "Weekly reading recorded successfully");

        // Check for auto-determination triggers
        if (observation == GrowthObservation.GROWTH_DETECTED) {
            response.put("autoDetermination", "POSITIVE");
            response.put("prompt", "Growth detected. Mark as Culture Positive?");
        } else if (weekNumber == 8 && observation == GrowthObservation.NO_GROWTH) {
            response.put("autoDetermination", "NEGATIVE");
            response.put("prompt", "Week 8 with no growth. Mark as Culture Negative?");
        } else if (observation == GrowthObservation.CONTAMINATED) {
            response.put("autoDetermination", "CONTAMINATED");
            response.put("prompt", "Culture contaminated. Mark as Contaminated or re-process?");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get samples pending reading for a specific week.
     */
    @GetMapping(value = "/reading/pending/{weekNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getSamplesPendingReading(@PathVariable Integer weekNumber) {
        List<String> sampleIds = tbCultureReadingService.findSampleItemIdsWithoutReadingForWeek(weekNumber);
        return ResponseEntity.ok(sampleIds);
    }

    // ==================== Final Result Determination Endpoints
    // ====================

    /**
     * Mark a culture as positive.
     */
    @PutMapping(value = "/result/{id}/positive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> markPositive(@PathVariable Integer id,
            @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        Integer positiveWeek = parseInteger(body.get("positiveWeek"));

        TbCultureReading reading = tbCultureReadingService.determineFinalResult(id, CultureResult.POSITIVE,
                positiveWeek, sysUserId);
        if (reading == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", reading.getId());
        response.put("result", CultureResult.POSITIVE.name());
        response.put("positiveWeek", positiveWeek);
        response.put("message", "Culture marked as positive");
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a culture as negative.
     */
    @PutMapping(value = "/result/{id}/negative", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> markNegative(@PathVariable Integer id, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        TbCultureReading reading = tbCultureReadingService.determineFinalResult(id, CultureResult.NEGATIVE, null,
                sysUserId);
        if (reading == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", reading.getId());
        response.put("result", CultureResult.NEGATIVE.name());
        response.put("message", "Culture marked as negative (no growth after 8 weeks)");
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a culture as contaminated.
     */
    @PutMapping(value = "/result/{id}/contaminated", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> markContaminated(@PathVariable Integer id, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        TbCultureReading reading = tbCultureReadingService.determineFinalResult(id, CultureResult.CONTAMINATED, null,
                sysUserId);
        if (reading == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", reading.getId());
        response.put("result", CultureResult.CONTAMINATED.name());
        response.put("message", "Culture marked as contaminated");
        return ResponseEntity.ok(response);
    }

    // ==================== Statistics Endpoints ====================

    /**
     * Get incubation monitoring summary for dashboard tiles for a specific notebook
     * entry.
     *
     * @param entryId Required notebook entry ID to filter summary by entry
     */
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IncubationSummary> getIncubationSummary(@RequestParam(required = true) Integer entryId) {
        IncubationSummary summary = tbCultureReadingService.getIncubationSummaryByEntry(entryId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get detailed incubation statistics for a specific notebook entry.
     *
     * @param entryId Required notebook entry ID to filter statistics by entry
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getIncubationStatistics(@RequestParam(required = true) Integer entryId) {
        Map<String, Object> stats = new HashMap<>();

        IncubationSummary summary = tbCultureReadingService.getIncubationSummaryByEntry(entryId);
        stats.put("totalIncubating", summary.totalIncubating());
        stats.put("week1to4", summary.week1to4());
        stats.put("week5to8", summary.week5to8());
        stats.put("positive", summary.positive());
        stats.put("negative", summary.negative());

        // Additional metrics
        stats.put("growthDetectedCount", tbCultureReadingService.countGrowthDetected());

        return ResponseEntity.ok(stats);
    }

    // ==================== Helper Methods ====================

    /**
     * Safely parse an Object to Integer. Handles Integer, Long, String, and Number
     * types.
     */
    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
