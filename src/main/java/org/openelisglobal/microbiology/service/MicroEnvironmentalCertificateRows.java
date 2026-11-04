package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.microbiology.form.MicroPatientReportForm;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPTable;

/** Renders released snapshots inside the existing Laporan Hasil certificate. */
public final class MicroEnvironmentalCertificateRows {
    private MicroEnvironmentalCertificateRows() {
    }

    public static void append(Document document, List<MicroPatientReportForm> reports, Font heading, Font cell)
            throws DocumentException {
        for (var report : reports) {
            document.add(new Paragraph(report.labUnitName, heading));
            var results = new PdfPTable(4);
            results.setWidthPercentage(100);
            for (String key : List.of("parameter", "result", "threshold", "status"))
                results.addCell(new Phrase(message(key), heading));
            for (var test : java.util.stream.Stream.concat(report.initial.stream(), report.additional.stream())
                    .toList()) {
                for (var value : test.values())
                    row(results, cell, test.name() + " / " + value.component(), value.value(), "—",
                            message("released"));
                tableNotes(results, cell, report, "RESULT", test.analysisId());
            }
            for (var culture : report.cultures) {
                row(results, cell, culture.label(),
                        MessageUtil.getMessage("microbiology.m6.outcome." + culture.outcome()), "—",
                        message("released"));
                tableNotes(results, cell, report, "CULTURE", culture.id());
            }
            document.add(results);
            for (var isolate : report.isolates) {
                document.add(new Paragraph(isolate.label() + ": " + isolate.organism(), heading));
                for (var note : report.notes)
                    if ("ISOLATE".equals(note.targetType()) && isolate.id().equals(note.targetId()))
                        document.add(new Paragraph(note.text(), cell));
                var ast = new PdfPTable(4);
                ast.setWidthPercentage(100);
                for (String key : List.of("agent", "raw", "interpretation", "breakpoint"))
                    ast.addCell(new Phrase(MessageUtil.getMessage("microbiology.m6." + key), heading));
                for (var reading : isolate.susceptibilities()) {
                    row(ast, cell, reading.agent(), reading.raw() + " " + safe(reading.units()),
                            MessageUtil.getMessage("microbiology.m6.interpretation." + reading.interpretation()),
                            reading.standard() + " " + reading.version());
                    tableNotes(ast, cell, report, "DRUG", reading.readingId());
                }
                document.add(ast);
            }
            for (var note : report.notes)
                if (note.targetType() == null)
                    document.add(new Paragraph(note.text(), cell));
        }
    }

    private static void tableNotes(PdfPTable table, Font font, MicroPatientReportForm report, String type, String id) {
        for (var note : report.notes)
            if (type.equals(note.targetType()) && id.equals(note.targetId())) {
                var cell = new org.openpdf.text.pdf.PdfPCell(new Phrase(note.text(), font));
                cell.setColspan(4);
                table.addCell(cell);
            }
    }

    private static void row(PdfPTable table, Font font, String... values) {
        for (var value : values)
            table.addCell(new Phrase(safe(value), font));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String message(String key) {
        return MessageUtil.getMessage("microbiology.environmental.certificate." + key);
    }
}
