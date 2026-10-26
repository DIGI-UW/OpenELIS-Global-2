package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

public class MicroPatientReportForm {
    public String caseId, labUnitId, labUnitName, classification, releaseType;
    public Integer reportVersion;
    public Timestamp releasedAt;
    public List<Test> initial = new ArrayList<>(), additional = new ArrayList<>();
    public List<Culture> cultures = new ArrayList<>();
    public List<Isolate> isolates = new ArrayList<>();
    public List<Note> notes = new ArrayList<>();
    public int validatedResults;

    public record Test(String analysisId, String name, List<Value> values) {
    }

    public record Value(String resultId, String componentId, String component, String value) {
    }

    public record Culture(String id, String label, String outcome, Timestamp inoculatedAt, Timestamp outcomeAt,
            BigDecimal incubationHours) {
    }

    public record Isolate(String id, String label, String organism, List<Susceptibility> susceptibilities) {
    }

    public record Susceptibility(String antibioticId, String agent, String readingId, String runId, String raw,
            String units, String interpretation, String standard, String version, String matchedBy) {
    }

    public record Note(String text, String author, Timestamp at) {
    }

    public record Version(String id, int number, String type, Timestamp at, String by, MicroPatientReportForm report) {
    }

    public static class Selection {
        public boolean selected;
    }
}
