package org.openelisglobal.qc.service;

import java.util.List;

/**
 * Service interface for QC Dashboard (T120).
 *
 * Provides compliance status and dashboard data for the QC monitoring UI.
 */
public interface QCDashboardService {

    /**
     * Get compliance status for all instruments.
     *
     * @return List of instrument compliance statuses
     */
    List<InstrumentComplianceStatus> getAllInstrumentComplianceStatus();

    /**
     * Get compliance status for a specific instrument.
     *
     * @param instrumentId The instrument ID
     * @return The instrument's compliance status
     */
    InstrumentComplianceStatus getInstrumentComplianceStatus(Integer instrumentId);

    /**
     * Get dashboard summary with aggregate counts.
     *
     * @return Dashboard summary
     */
    DashboardSummary getDashboardSummary();

    /**
     * Compliance status for a single instrument.
     */
    class InstrumentComplianceStatus {
        private Integer instrumentId;
        private String instrumentName;
        private String complianceColor; // GREEN, YELLOW, RED
        private int unresolvedRejections;
        private int unresolvedWarnings;
        private List<String> triggeredRules;
        private String lastResultTime;
        private String lastViolationTime;
        private int activeControlLots;

        public Integer getInstrumentId() {
            return instrumentId;
        }

        public void setInstrumentId(Integer instrumentId) {
            this.instrumentId = instrumentId;
        }

        public String getInstrumentName() {
            return instrumentName;
        }

        public void setInstrumentName(String instrumentName) {
            this.instrumentName = instrumentName;
        }

        public String getComplianceColor() {
            return complianceColor;
        }

        public void setComplianceColor(String complianceColor) {
            this.complianceColor = complianceColor;
        }

        public int getUnresolvedRejections() {
            return unresolvedRejections;
        }

        public void setUnresolvedRejections(int unresolvedRejections) {
            this.unresolvedRejections = unresolvedRejections;
        }

        public int getUnresolvedWarnings() {
            return unresolvedWarnings;
        }

        public void setUnresolvedWarnings(int unresolvedWarnings) {
            this.unresolvedWarnings = unresolvedWarnings;
        }

        public List<String> getTriggeredRules() {
            return triggeredRules;
        }

        public void setTriggeredRules(List<String> triggeredRules) {
            this.triggeredRules = triggeredRules;
        }

        public String getLastResultTime() {
            return lastResultTime;
        }

        public void setLastResultTime(String lastResultTime) {
            this.lastResultTime = lastResultTime;
        }

        public String getLastViolationTime() {
            return lastViolationTime;
        }

        public void setLastViolationTime(String lastViolationTime) {
            this.lastViolationTime = lastViolationTime;
        }

        public int getActiveControlLots() {
            return activeControlLots;
        }

        public void setActiveControlLots(int activeControlLots) {
            this.activeControlLots = activeControlLots;
        }
    }

    /**
     * Dashboard summary with aggregate data.
     */
    class DashboardSummary {
        private int totalInstruments;
        private int compliantInstruments; // GREEN
        private int warningInstruments; // YELLOW
        private int nonCompliantInstruments; // RED
        private int totalUnresolvedViolations;
        private int totalRejections;
        private int totalWarnings;
        private String lastUpdateTime;

        public int getTotalInstruments() {
            return totalInstruments;
        }

        public void setTotalInstruments(int totalInstruments) {
            this.totalInstruments = totalInstruments;
        }

        public int getCompliantInstruments() {
            return compliantInstruments;
        }

        public void setCompliantInstruments(int compliantInstruments) {
            this.compliantInstruments = compliantInstruments;
        }

        public int getWarningInstruments() {
            return warningInstruments;
        }

        public void setWarningInstruments(int warningInstruments) {
            this.warningInstruments = warningInstruments;
        }

        public int getNonCompliantInstruments() {
            return nonCompliantInstruments;
        }

        public void setNonCompliantInstruments(int nonCompliantInstruments) {
            this.nonCompliantInstruments = nonCompliantInstruments;
        }

        public int getTotalUnresolvedViolations() {
            return totalUnresolvedViolations;
        }

        public void setTotalUnresolvedViolations(int totalUnresolvedViolations) {
            this.totalUnresolvedViolations = totalUnresolvedViolations;
        }

        public int getTotalRejections() {
            return totalRejections;
        }

        public void setTotalRejections(int totalRejections) {
            this.totalRejections = totalRejections;
        }

        public int getTotalWarnings() {
            return totalWarnings;
        }

        public void setTotalWarnings(int totalWarnings) {
            this.totalWarnings = totalWarnings;
        }

        public String getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(String lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }
    }
}
