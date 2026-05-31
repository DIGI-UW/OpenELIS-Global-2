package org.openelisglobal.biorepository.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formats and parses structured biorepository transfer request notes.
 * Stored as:
 * Project: {projectName}
 * Reason: {transferReason}
 */
public final class SampleTransferNotesHelper {

    private static final Pattern PROJECT_LINE = Pattern.compile("^Project:\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern REASON_LINE = Pattern.compile("^Reason:\\s*(.+)$", Pattern.MULTILINE | Pattern.DOTALL);

    private SampleTransferNotesHelper() {
    }

    public static String formatStructuredNotes(String projectName, String transferReason) {
        String project = projectName != null ? projectName.trim() : "";
        String reason = transferReason != null ? transferReason.trim() : "";
        return "Project: " + project + "\nReason: " + reason;
    }

    public static ParsedTransferNotes parseStructuredNotes(String requestNotes) {
        ParsedTransferNotes parsed = new ParsedTransferNotes();
        if (requestNotes == null || requestNotes.isBlank()) {
            return parsed;
        }

        Matcher projectMatcher = PROJECT_LINE.matcher(requestNotes);
        if (projectMatcher.find()) {
            parsed.setProjectName(projectMatcher.group(1).trim());
        }

        Matcher reasonMatcher = REASON_LINE.matcher(requestNotes);
        if (reasonMatcher.find()) {
            parsed.setTransferReason(reasonMatcher.group(1).trim());
        }

        if (parsed.getProjectName() == null && parsed.getTransferReason() == null) {
            parsed.setTransferReason(requestNotes.trim());
        }

        return parsed;
    }

    public static class ParsedTransferNotes {
        private String projectName;
        private String transferReason;

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getTransferReason() {
            return transferReason;
        }

        public void setTransferReason(String transferReason) {
            this.transferReason = transferReason;
        }
    }
}
