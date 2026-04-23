package org.openelisglobal.qc.dto;

/**
 * Dashboard DTO: aggregate QC counts across all instruments.
 */
public class QCDashboardSummary {

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