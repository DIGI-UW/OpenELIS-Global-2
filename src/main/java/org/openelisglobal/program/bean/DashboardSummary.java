package org.openelisglobal.program.bean;

import java.util.Map;

public class DashboardSummary {
    private long totalEntries;
    private long completedQuestionnaires;
    private long pendingQuestionnaires;
    private Map<String, Long> entriesByProgram;

    public DashboardSummary(long totalEntries, long completedQuestionnaires, long pendingQuestionnaires,
            Map<String, Long> entriesByProgram) {
        this.totalEntries = totalEntries;
        this.completedQuestionnaires = completedQuestionnaires;
        this.pendingQuestionnaires = pendingQuestionnaires;
        this.entriesByProgram = entriesByProgram;
    }

    // Getters and setters
    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
    }

    public long getCompletedQuestionnaires() {
        return completedQuestionnaires;
    }

    public void setCompletedQuestionnaires(long completedQuestionnaires) {
        this.completedQuestionnaires = completedQuestionnaires;
    }

    public long getPendingQuestionnaires() {
        return pendingQuestionnaires;
    }

    public void setPendingQuestionnaires(long pendingQuestionnaires) {
        this.pendingQuestionnaires = pendingQuestionnaires;
    }

    public Map<String, Long> getEntriesByProgram() {
        return entriesByProgram;
    }

    public void setEntriesByProgram(Map<String, Long> entriesByProgram) {
        this.entriesByProgram = entriesByProgram;
    }
}