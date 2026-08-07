package org.openelisglobal.coldstorage.controller.rest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.coldstorage.service.FreezerReadingService;
import org.openelisglobal.coldstorage.service.FreezerService;
import org.openelisglobal.coldstorage.service.dto.FreezerExcursionData;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAnyRole('RECEPTION', 'ADMIN')")
    @GetMapping("/excursions")
    public ResponseEntity<List<FreezerExcursionData>> getExcursions(@RequestParam(required = false) Long freezerId,
            @RequestParam String start, @RequestParam String end) {

        List<FreezerExcursionData> excursions = new ArrayList<>();

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

            // Excursion-grouping business logic lives in FreezerReadingService, not
            // here - controllers are a thin delegation layer per the 5-layer
            // architecture.
            for (Freezer freezer : freezersToCheck) {
                excursions.addAll(freezerReadingService.findExcursions(freezer, startTime, endTime));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(excursions);
    }
}
