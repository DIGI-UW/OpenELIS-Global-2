package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.microbiology.form.*;

/**
 * Formats an immutable issued snapshot for Jasper; never reads current clinical
 * data.
 */
public final class MicroPatientReportRows {
    private MicroPatientReportRows() {
    }

    public static List<MicroPatientReportRow> of(MicroPatientReportForm report) {
        var rows = new ArrayList<MicroPatientReportRow>();
        heading(rows, "initial");
        tests(rows, report.initial, report.notes);
        heading(rows, "culture");
        for (var row : report.cultures) {
            text(rows, row.label() + ": " + message("outcome." + row.outcome()) + "; " + message("incubation") + ": "
                    + row.incubationHours() + " " + message("hours"));
            notes(rows, report.notes, "CULTURE", row.id());
        }
        for (var isolate : report.isolates) {
            text(rows, isolate.label() + ": " + isolate.organism());
            notes(rows, report.notes, "ISOLATE", isolate.id());
        }
        heading(rows, "ast");
        if (!"NOT_APPLICABLE".equals(report.classification))
            text(rows, message("classification." + report.classification));
        for (var isolate : report.isolates) {
            text(rows, isolate.label() + ": " + isolate.organism());
            rows.add(new MicroPatientReportRow(null, null, message("agent"), message("raw"), message("interpretation"),
                    message("breakpoint"), true));
            for (var row : isolate.susceptibilities()) {
                rows.add(new MicroPatientReportRow(null, null, row.agent(), safe(row.raw()) + " " + safe(row.units()),
                        message("interpretation." + row.interpretation()),
                        row.standard() + " " + row.version()
                                + ("NON_MENINGITIS".equals(row.clinicalContext())
                                        ? " — " + message("clinicalContext.nonMeningitis")
                                        : ""),
                        true));
                notes(rows, report.notes, "DRUG", row.readingId());
            }
        }
        heading(rows, "additional");
        tests(rows, report.additional, report.notes);
        notes(rows, report.notes, null, null);
        return rows;
    }

    private static void tests(List<MicroPatientReportRow> rows, List<MicroPatientReportForm.Test> tests,
            List<MicroPatientReportForm.Note> notes) {
        for (var test : tests) {
            text(rows, test.name());
            for (var value : test.values())
                text(rows, value.component() + ": " + value.value());
            notes(rows, notes, "RESULT", test.analysisId());
        }
    }

    private static void notes(List<MicroPatientReportRow> rows, List<MicroPatientReportForm.Note> notes, String type,
            String id) {
        for (var note : notes)
            if (Objects.equals(type, note.targetType()) && Objects.equals(id, note.targetId()))
                text(rows, note.text() + " — " + safe(note.author()));
    }

    private static void heading(List<MicroPatientReportRow> rows, String key) {
        rows.add(new MicroPatientReportRow(message(key), null, null, null, null, null, false));
    }

    private static void text(List<MicroPatientReportRow> rows, String text) {
        rows.add(new MicroPatientReportRow(null, text, null, null, null, null, false));
    }

    private static String message(String key) {
        return MessageUtil.getMessage("microbiology.m6." + key);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
