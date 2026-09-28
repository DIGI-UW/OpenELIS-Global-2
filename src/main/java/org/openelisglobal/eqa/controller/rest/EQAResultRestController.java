package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQAProviderScoringService;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize(EQAGuards.READ)
public class EQAResultRestController extends BaseRestController {

    @Autowired
    private EQAProviderScoringService scoringService;

    /**
     * What a participant has reported so far, per test of the scheme.
     */
    @GetMapping(value = "/cycles/{cycleId}/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> cycleResults(@PathVariable Long cycleId, @RequestParam Long organizationId) {
        return scoringService.intakeGrid(cycleId, organizationId);
    }

    /**
     * Provider-side entry of a participant's phoned or emailed results —
     * {@code {"organizationId": 12, "results": [{"testId": 7, "value":
     * "Reactive"}]}}. Numbers and qualitative words alike; a blank value leaves the
     * test untouched.
     */
    @PostMapping(value = "/cycles/{cycleId}/results", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public Map<String, Object> takeInCycleResults(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestBody Map<String, Object> body) {
        Long organizationId = longField(body, "organizationId");
        if (organizationId == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
        Map<Long, String> reported = new LinkedHashMap<>();
        if (body.get("results") instanceof List<?> rows) {
            for (Object row : rows) {
                if (row instanceof Map<?, ?> cell && cell.get("testId") != null) {
                    reported.put(Long.valueOf(String.valueOf(cell.get("testId"))),
                            cell.get("value") == null ? null : String.valueOf(cell.get("value")));
                }
            }
        }
        return scoringService.takeIn(cycleId, organizationId, reported, EQASubmissionMethod.MANUAL,
                getSysUserId(request));
    }

    /**
     * Import the participant's export bundle CSV as pasted or uploaded —
     * {@code {"organizationId": 12, "csv":
     * "cycle_id,...,analyte_name,result_value,..."}}.
     */
    @PostMapping(value = "/cycles/{cycleId}/results/import", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PROVIDER)
    public Map<String, Object> importCycleResultsCsv(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestBody Map<String, Object> body) {
        Long organizationId = longField(body, "organizationId");
        if (organizationId == null) {
            throw new IllegalArgumentException("organizationId is required");
        }
        return scoringService.importReportedCsv(cycleId, organizationId, stringField(body, "csv"),
                getSysUserId(request));
    }
}
