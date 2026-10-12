package org.openelisglobal.dashboard.management.equipment;

public record AnalyzerActivity(String id, String name, Status status, long samples, String section,
        String lastActivityAt) {

    public enum Status {
        ACTIVE, QUIET, NO_ACTIVITY
    }
}
