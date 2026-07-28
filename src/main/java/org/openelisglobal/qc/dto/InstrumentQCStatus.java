package org.openelisglobal.qc.dto;

import java.util.List;

/**
 * Dashboard DTO: QC status snapshot for a single instrument.
 */
public class InstrumentQCStatus {

    private String instrumentId;
    private String instrumentName;
    private String instrumentType;
    private String instrumentLocation;
    private String complianceColor; // GREEN, YELLOW, RED
    private int unresolvedRejections;
    private int unresolvedWarnings;
    private List<String> triggeredRules;
    private List<TriggeredRuleDetail> triggeredRuleDetails;
    private List<AnalyteDetail> analyteDetails;
    private String lastResultTime;
    private String lastViolationTime;
    private int activeControlLots;

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public String getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
    }

    public String getInstrumentLocation() {
        return instrumentLocation;
    }

    public void setInstrumentLocation(String instrumentLocation) {
        this.instrumentLocation = instrumentLocation;
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

    public List<TriggeredRuleDetail> getTriggeredRuleDetails() {
        return triggeredRuleDetails;
    }

    public void setTriggeredRuleDetails(List<TriggeredRuleDetail> triggeredRuleDetails) {
        this.triggeredRuleDetails = triggeredRuleDetails;
    }

    public List<AnalyteDetail> getAnalyteDetails() {
        return analyteDetails;
    }

    public void setAnalyteDetails(List<AnalyteDetail> analyteDetails) {
        this.analyteDetails = analyteDetails;
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