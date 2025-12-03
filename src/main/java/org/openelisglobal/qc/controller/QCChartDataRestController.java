package org.openelisglobal.qc.controller;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for QC Chart Data (Levey-Jennings Charts). Supports User
 * Story 2: Monitor QC Data with Control Charts.
 *
 * Following Constitution IV.5: @Transactional in services ONLY (NOT
 * controllers)
 */
@RestController
@RequestMapping("/rest/qc/charts")
public class QCChartDataRestController {

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private QCStatisticsDAO statisticsDAO;

    @Autowired
    private QCRuleViolationDAO violationDAO;

    /**
     * Get chart data for a specific control lot with optional filtering. GET
     * /rest/qc/charts/{controlLotId}
     *
     * @param controlLotId The control lot ID
     * @param startDate    Optional start date filter (inclusive)
     * @param endDate      Optional end date filter (inclusive)
     * @return Chart data including results and violation markers
     */
    @GetMapping("/{controlLotId}")
    public ResponseEntity<ChartDataResponse> getChartData(@PathVariable("controlLotId") String controlLotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            // Build date range
            Timestamp startTimestamp = null;
            Timestamp endTimestamp = null;

            if (startDate != null) {
                startTimestamp = Timestamp.valueOf(LocalDateTime.of(startDate, LocalTime.MIN));
            }
            if (endDate != null) {
                endTimestamp = Timestamp.valueOf(LocalDateTime.of(endDate, LocalTime.MAX));
            }

            // Get QC results
            List<QCResult> results = resultDAO.findByControlLotAndDateRange(controlLotId, startTimestamp, endTimestamp);

            // Get violations for these results
            List<String> resultIds = results.stream().map(QCResult::getId).toList();
            List<QCRuleViolation> violations = new ArrayList<>();
            for (String resultId : resultIds) {
                violations.addAll(violationDAO.findByTriggeringResultId(resultId));
            }

            // Build response
            ChartDataResponse response = new ChartDataResponse();
            response.setControlLotId(controlLotId);
            response.setDataPoints(buildDataPoints(results, violations));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError("QCChartDataRestController", "getChartData", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get statistics (mean, SD) for reference lines in the chart. GET
     * /rest/qc/charts/{controlLotId}/statistics
     *
     * @param controlLotId The control lot ID
     * @return Statistics for drawing reference lines (mean, +/-1SD, +/-2SD, +/-3SD)
     */
    @GetMapping("/{controlLotId}/statistics")
    public ResponseEntity<ChartStatisticsResponse> getChartStatistics(
            @PathVariable("controlLotId") String controlLotId) {
        try {
            QCStatistics stats = statisticsDAO.findLatestByControlLot(controlLotId);

            if (stats == null) {
                return ResponseEntity.notFound().build();
            }

            ChartStatisticsResponse response = new ChartStatisticsResponse();
            response.setControlLotId(controlLotId);
            response.setMean(stats.getMean() != null ? stats.getMean().doubleValue() : 0.0);
            response.setStandardDeviation(
                    stats.getStandardDeviation() != null ? stats.getStandardDeviation().doubleValue() : 0.0);
            response.setCalculationMethod(stats.getCalculationMethod());
            response.setResultCount(stats.getNumValues() != null ? stats.getNumValues() : 0);

            // Calculate SD lines
            double mean = response.getMean();
            double sd = response.getStandardDeviation();
            response.setPlus1SD(mean + sd);
            response.setPlus2SD(mean + 2 * sd);
            response.setPlus3SD(mean + 3 * sd);
            response.setMinus1SD(mean - sd);
            response.setMinus2SD(mean - 2 * sd);
            response.setMinus3SD(mean - 3 * sd);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LogEvent.logError("QCChartDataRestController", "getChartStatistics", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<ChartDataPoint> buildDataPoints(List<QCResult> results, List<QCRuleViolation> violations) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();

        for (QCResult result : results) {
            ChartDataPoint point = new ChartDataPoint();
            point.setResultId(result.getId());
            point.setTimestamp(result.getRunDateTime() != null ? result.getRunDateTime().toInstant().toString() : null);
            point.setValue(result.getResultValue() != null ? result.getResultValue().doubleValue() : null);
            point.setZScore(result.getZScore() != null ? result.getZScore().doubleValue() : null);

            // Find violations for this result (using triggeringResultId)
            List<String> violatedRules = violations.stream()
                    .filter(v -> result.getId().equals(v.getTriggeringResultId())).map(QCRuleViolation::getRuleCode)
                    .toList();
            point.setViolatedRules(violatedRules);
            point.setHasViolation(!violatedRules.isEmpty());

            // Determine severity (highest among violations)
            String severity = violations.stream().filter(v -> result.getId().equals(v.getTriggeringResultId()))
                    .map(QCRuleViolation::getSeverity).filter(s -> "REJECTION".equals(s)).findFirst()
                    .orElse(violatedRules.isEmpty() ? null : "WARNING");
            point.setSeverity(severity);

            dataPoints.add(point);
        }

        return dataPoints;
    }

    // ==================== Response DTOs ====================

    /** Response containing chart data points. */
    public static class ChartDataResponse {

        private String controlLotId;
        private List<ChartDataPoint> dataPoints;

        public String getControlLotId() {
            return controlLotId;
        }

        public void setControlLotId(String controlLotId) {
            this.controlLotId = controlLotId;
        }

        public List<ChartDataPoint> getDataPoints() {
            return dataPoints;
        }

        public void setDataPoints(List<ChartDataPoint> dataPoints) {
            this.dataPoints = dataPoints;
        }
    }

    /** Individual data point for the chart. */
    public static class ChartDataPoint {

        private String resultId;
        private String timestamp;
        private Double value;
        private Double zScore;
        private boolean hasViolation;
        private List<String> violatedRules;
        private String severity;

        public String getResultId() {
            return resultId;
        }

        public void setResultId(String resultId) {
            this.resultId = resultId;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public Double getZScore() {
            return zScore;
        }

        public void setZScore(Double zScore) {
            this.zScore = zScore;
        }

        public boolean isHasViolation() {
            return hasViolation;
        }

        public void setHasViolation(boolean hasViolation) {
            this.hasViolation = hasViolation;
        }

        public List<String> getViolatedRules() {
            return violatedRules;
        }

        public void setViolatedRules(List<String> violatedRules) {
            this.violatedRules = violatedRules;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }

    /** Response containing statistics for chart reference lines. */
    public static class ChartStatisticsResponse {

        private String controlLotId;
        private double mean;
        private double standardDeviation;
        private String calculationMethod;
        private int resultCount;
        private double plus1SD;
        private double plus2SD;
        private double plus3SD;
        private double minus1SD;
        private double minus2SD;
        private double minus3SD;

        public String getControlLotId() {
            return controlLotId;
        }

        public void setControlLotId(String controlLotId) {
            this.controlLotId = controlLotId;
        }

        public double getMean() {
            return mean;
        }

        public void setMean(double mean) {
            this.mean = mean;
        }

        public double getStandardDeviation() {
            return standardDeviation;
        }

        public void setStandardDeviation(double standardDeviation) {
            this.standardDeviation = standardDeviation;
        }

        public String getCalculationMethod() {
            return calculationMethod;
        }

        public void setCalculationMethod(String calculationMethod) {
            this.calculationMethod = calculationMethod;
        }

        public int getResultCount() {
            return resultCount;
        }

        public void setResultCount(int resultCount) {
            this.resultCount = resultCount;
        }

        public double getPlus1SD() {
            return plus1SD;
        }

        public void setPlus1SD(double plus1SD) {
            this.plus1SD = plus1SD;
        }

        public double getPlus2SD() {
            return plus2SD;
        }

        public void setPlus2SD(double plus2SD) {
            this.plus2SD = plus2SD;
        }

        public double getPlus3SD() {
            return plus3SD;
        }

        public void setPlus3SD(double plus3SD) {
            this.plus3SD = plus3SD;
        }

        public double getMinus1SD() {
            return minus1SD;
        }

        public void setMinus1SD(double minus1SD) {
            this.minus1SD = minus1SD;
        }

        public double getMinus2SD() {
            return minus2SD;
        }

        public void setMinus2SD(double minus2SD) {
            this.minus2SD = minus2SD;
        }

        public double getMinus3SD() {
            return minus3SD;
        }

        public void setMinus3SD(double minus3SD) {
            this.minus3SD = minus3SD;
        }
    }
}
