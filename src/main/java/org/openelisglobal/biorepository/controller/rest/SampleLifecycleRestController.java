package org.openelisglobal.biorepository.controller.rest;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.openelisglobal.biorepository.controller.rest.dto.SampleLifecycleResponseDTO;
import org.openelisglobal.biorepository.service.SampleLifecycleService;
import org.openelisglobal.biorepository.valueholder.ChainOfCustodyLog.CustodyAction;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest/biorepository/lifecycle")
public class SampleLifecycleRestController extends BaseRestController {

    @Autowired
    private SampleLifecycleService sampleLifecycleService;

    @GetMapping(value = "/sample-item/{sampleItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getBySampleItem(@PathVariable("sampleItemId") Integer sampleItemId) {
        SampleLifecycleResponseDTO response = sampleLifecycleService.getBySampleItemId(sampleItemId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/bio-sample/{bioSampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getByBioSample(@PathVariable("bioSampleId") Integer bioSampleId) {
        SampleLifecycleResponseDTO response = sampleLifecycleService.getByBioSampleId(bioSampleId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> search(@RequestParam(required = false) String sampleExternalId,
            @RequestParam(required = false) String action, @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        try {
            CustodyAction custodyAction = null;
            if (action != null && !action.trim().isEmpty() && !"ALL".equalsIgnoreCase(action)) {
                custodyAction = CustodyAction.valueOf(action);
            }

            Timestamp startTimestamp = parseDate(startDate, true);
            Timestamp endTimestamp = parseDate(endDate, false);

            Map<String, Object> response = sampleLifecycleService.search(sampleExternalId, custodyAction,
                    startTimestamp, endTimestamp, page, pageSize);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Timestamp parseDate(String rawDate, boolean startOfDay) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }

        LocalDate parsedDate = LocalDate.parse(rawDate, DateTimeFormatter.ISO_LOCAL_DATE);
        return Timestamp.valueOf(startOfDay ? parsedDate.atStartOfDay() : parsedDate.atTime(23, 59, 59));
    }
}
