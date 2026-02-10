package org.openelisglobal.eqa.controller.rest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.eqa.service.EQAResultService;
import org.openelisglobal.eqa.service.EQAStatisticsService;
import org.openelisglobal.eqa.valueholder.EQAResult;
import org.openelisglobal.eqa.valueholder.EQASubmissionMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/eqa")
public class EQAResultRestController {

    @Autowired
    private EQAResultService resultService;

    @Autowired
    private EQAStatisticsService statisticsService;

    @PostMapping(value = "/distributions/{distributionId}/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submitResult(@PathVariable Long distributionId, @RequestBody Map<String, Object> body) {
        try {
            Number orgId = (Number) body.get("organizationId");
            Number testId = (Number) body.get("testId");
            Number value = (Number) body.get("resultValue");

            if (orgId == null || testId == null || value == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "organizationId, testId, and resultValue are required"));
            }

            EQAResult result = resultService.submitResult(distributionId, orgId.longValue(), testId.longValue(),
                    new BigDecimal(value.toString()), EQASubmissionMethod.MANUAL);

            return ResponseEntity.ok(toResultDto(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/distributions/{distributionId}/results/import", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> batchImportResults(@PathVariable Long distributionId,
            @RequestBody List<Map<String, Object>> rows) {
        try {
            int successCount = 0;
            int errorCount = 0;
            Map<Integer, String> errors = new HashMap<>();

            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                try {
                    Number orgId = (Number) row.get("organizationId");
                    Number testId = (Number) row.get("testId");
                    Number value = (Number) row.get("resultValue");

                    if (orgId == null || testId == null || value == null) {
                        errors.put(i, "Missing required fields");
                        errorCount++;
                        continue;
                    }

                    resultService.submitResult(distributionId, orgId.longValue(), testId.longValue(),
                            new BigDecimal(value.toString()), EQASubmissionMethod.FILE_UPLOAD);
                    successCount++;
                } catch (Exception e) {
                    errors.put(i, e.getMessage());
                    errorCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("successCount", successCount);
            response.put("errorCount", errorCount);
            response.put("totalRows", rows.size());
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/distributions/{distributionId}/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getResults(@PathVariable Long distributionId) {
        List<EQAResult> results = resultService.findByDistributionId(distributionId);

        List<Map<String, Object>> dtos = results.stream().map(this::toResultDto).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("results", dtos);
        response.put("totalCount", dtos.size());
        response.put("distributionId", distributionId);

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/distributions/{distributionId}/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable Long distributionId) {
        List<EQAResult> results = resultService.findByDistributionId(distributionId);

        List<BigDecimal> values = results.stream().filter(r -> r.getResultValue() != null)
                .map(EQAResult::getResultValue).collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("distributionId", distributionId);
        stats.put("participantCount", results.size());
        stats.put("hasEnoughParticipants", results.size() >= EQAStatisticsService.MIN_PARTICIPANTS_FOR_STATS);

        if (!values.isEmpty()) {
            BigDecimal mean = statisticsService.calculateMean(values);
            stats.put("mean", mean);

            if (values.size() >= 2) {
                BigDecimal sd = statisticsService.calculateStandardDeviation(values, mean);
                stats.put("standardDeviation", sd);
            }
        }

        List<Map<String, Object>> resultStats = results.stream().map(r -> {
            Map<String, Object> rs = new HashMap<>();
            rs.put("organizationId", r.getParticipantOrganizationId());
            rs.put("testId", r.getTestId());
            rs.put("resultValue", r.getResultValue());
            rs.put("zScore", r.getZScore());
            rs.put("performanceStatus", r.getPerformanceStatus() != null ? r.getPerformanceStatus().name() : null);
            return rs;
        }).collect(Collectors.toList());
        stats.put("results", resultStats);

        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> toResultDto(EQAResult r) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", r.getId());
        dto.put("organizationId", r.getParticipantOrganizationId());
        dto.put("testId", r.getTestId());
        dto.put("resultValue", r.getResultValue());
        dto.put("zScore", r.getZScore());
        dto.put("performanceStatus", r.getPerformanceStatus() != null ? r.getPerformanceStatus().name() : null);
        dto.put("submissionMethod", r.getSubmissionMethod() != null ? r.getSubmissionMethod().name() : null);
        dto.put("isLateSubmission", r.getIsLateSubmission());
        return dto;
    }
}
