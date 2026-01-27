package org.openelisglobal.coldstorage.controller.rest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.coldstorage.valueholder.FreezerReading;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/coldstorage/reports")
public class FreezerReportDataController extends BaseRestController {

    @Autowired
    private FreezerReadingService freezerReadingService;

    @Autowired
    private FreezerService freezerService;

    @GetMapping("/excursions")
    public ResponseEntity<List<Map<String, Object>>> getExcursions(@RequestParam(required = false) Long freezerId,
            @RequestParam String start, @RequestParam String end) {

        List<Map<String, Object>> excursions = new ArrayList<>();

        try {
            OffsetDateTime startTime = OffsetDateTime.parse(start);
            OffsetDateTime endTime = OffsetDateTime.parse(end);

            List<Freezer> freezersToCheck;
            if (freezerId != null) {
                Freezer freezer = freezerService.findById(freezerId).orElse(null);
                freezersToCheck = freezer != null ? List.of(freezer) : List.of();
            } else {
                freezersToCheck = freezerService.getAllFreezers("");
            }

            for (Freezer freezer : freezersToCheck) {
                Long fId = freezer.getId();
                List<FreezerReading> readings = freezerReadingService.getReadingsBetween(fId, startTime, endTime);

                // Group consecutive excursion readings
                processExcursionsForPreview(readings, freezer, excursions);
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(excursions);
    }

    private void processExcursionsForPreview(List<FreezerReading> readings, Freezer freezer,
            List<Map<String, Object>> excursions) {
        if (readings.isEmpty()) {
            return;
        }

        List<FreezerReading> currentExcursion = new ArrayList<>();
        FreezerReading.Status currentStatus = null;

        for (FreezerReading reading : readings) {
            if (reading.getStatus() == FreezerReading.Status.WARNING
                    || reading.getStatus() == FreezerReading.Status.CRITICAL) {

                if (currentExcursion.isEmpty() || reading.getStatus() == currentStatus) {
                    currentExcursion.add(reading);
                    currentStatus = reading.getStatus();
                } else {
                    saveExcursionPreview(currentExcursion, freezer, excursions);
                    currentExcursion = new ArrayList<>();
                    currentExcursion.add(reading);
                    currentStatus = reading.getStatus();
                }
            } else {
                if (!currentExcursion.isEmpty()) {
                    saveExcursionPreview(currentExcursion, freezer, excursions);
                    currentExcursion = new ArrayList<>();
                    currentStatus = null;
                }
            }
        }

        if (!currentExcursion.isEmpty()) {
            saveExcursionPreview(currentExcursion, freezer, excursions);
        }
    }

    private void saveExcursionPreview(List<FreezerReading> excursionReadings, Freezer freezer,
            List<Map<String, Object>> excursions) {
        if (excursionReadings.isEmpty()) {
            return;
        }

        FreezerReading firstReading = excursionReadings.get(0);
        FreezerReading lastReading = excursionReadings.get(excursionReadings.size() - 1);

        Map<String, Object> excursion = new HashMap<>();
        excursion.put("alertId", firstReading.getId());
        excursion.put("freezerId", freezer.getId());
        excursion.put("freezerName", freezer.getName());
        excursion.put("locationName", freezer.getRoom());
        excursion.put("startTime", firstReading.getRecordedAt() != null ? firstReading.getRecordedAt().toString() : "");
        excursion.put("endTime", lastReading.getRecordedAt() != null ? lastReading.getRecordedAt().toString() : "");

        // Calculate duration in seconds
        if (firstReading.getRecordedAt() != null && lastReading.getRecordedAt() != null) {
            long durationSeconds = java.time.Duration.between(firstReading.getRecordedAt(), lastReading.getRecordedAt())
                    .getSeconds();
            excursion.put("durationSeconds", durationSeconds);
        }

        // Find min/max temperatures
        excursionReadings.stream().filter(r -> r.getTemperatureCelsius() != null)
                .min((r1, r2) -> r1.getTemperatureCelsius().compareTo(r2.getTemperatureCelsius()))
                .ifPresent(r -> excursion.put("minTemperature", r.getTemperatureCelsius()));

        excursionReadings.stream().filter(r -> r.getTemperatureCelsius() != null)
                .max((r1, r2) -> r1.getTemperatureCelsius().compareTo(r2.getTemperatureCelsius()))
                .ifPresent(r -> excursion.put("maxTemperature", r.getTemperatureCelsius()));

        excursion.put("severity", firstReading.getStatus().name());
        excursion.put("status", "RESOLVED");

        excursions.add(excursion);
    }
}
