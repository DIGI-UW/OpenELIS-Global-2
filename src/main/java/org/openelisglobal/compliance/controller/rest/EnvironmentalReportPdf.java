package org.openelisglobal.compliance.controller.rest;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.compliance.service.ComplianceEvaluationResult;
import org.openelisglobal.compliance.service.EnvironmentalReportRow;
import org.openelisglobal.compliance.valueholder.ComplianceStatus;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.microbiology.service.MicroEnvironmentalCertificateRows;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

/**
 * The environmental report an order prints from the Compliance Report page
 * (OGC-552, OGC-1617). Every report lists every validated result. When a
 * compliance standard is linked it is a certificate: it adds the compliance
 * summary, the conclusion for the assessed parameters and the decision rule.
 * Without one it is a results report and says it is not a statement of
 * conformity. Every label comes from a message key, so a site prints in its own
 * language.
 */
final class EnvironmentalReportPdf {

    /** What an amended report states about the report it replaces. */
    record Amendment(String certificateNumber, Integer number, String supersedes, String reason) {
    }

    private static final Color HEADER = new Color(33, 82, 149);

    private final Font titleFont = CertificateFonts.bold(14);
    private final Font sectionFont = CertificateFonts.bold(11);
    private final Font headerFont = CertificateFonts.bold(9, Color.WHITE);
    private final Font cellFont = CertificateFonts.regular(9);
    private final Font labelFont = CertificateFonts.bold(9);

    static byte[] build(ComplianceReportOrderDTO dto, Amendment amendment) throws DocumentException {
        return new EnvironmentalReportPdf().render(dto, amendment);
    }

    private byte[] render(ComplianceReportOrderDTO dto, Amendment amendment) throws DocumentException {
        boolean certificate = ComplianceReportOrderDTO.KIND_COMPLIANCE.equals(dto.getReportKind());
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize());
        PdfWriter.getInstance(document, buf);
        document.open();

        String title = "envReport.title." + (certificate ? "certificate" : "results")
                + (amendment != null ? ".amendment" : "");
        document.add(new Phrase(text(title) + "\n\n", titleFont));
        if (amendment != null) {
            document.add(
                    new Phrase(text("envReport.certificateNumber", amendment.certificateNumber()) + "\n", labelFont));
        }
        document.add(new Phrase(text("envReport.labNumber", dto.getLabNumber()) + "\n", cellFont));
        if (amendment != null && amendment.number() != null && amendment.number() >= 1) {
            document.add(new Phrase("\n" + text("envReport.amendment.notice") + "\n\n", sectionFont));
            document.add(new Phrase(text("envReport.amendment.number", String.valueOf(amendment.number())) + "\n",
                    cellFont));
            document.add(
                    new Phrase(text("envReport.amendment.supersedes", safe(amendment.supersedes())) + "\n", cellFont));
            document.add(new Phrase(text("envReport.amendment.reason", safe(amendment.reason())) + "\n", cellFont));
        }
        if (certificate) {
            document.add(new Phrase(text("envReport.standard", safe(dto.getStandardName())) + "\n", cellFont));
        }
        document.add(new Phrase(text("envReport.collectionDate", safe(dto.getCollectionDate())) + "\n", cellFont));

        List<String[]> site = new ArrayList<>();
        site.add(new String[] { text("envReport.site.site"), safe(dto.getSiteName()) });
        site.add(new String[] { text("envReport.site.gps"), safe(dto.getGpsCoordinates()) });
        site.add(new String[] { text("envReport.site.collectedAt"), safe(dto.getCollectionDate()) });
        site.add(new String[] { text("envReport.site.collectionMethod"), safe(dto.getCollectionMethod()) });
        if (certificate) {
            site.add(new String[] { text("envReport.site.regulation"), safe(dto.getRegulationNumber()) });
            site.add(new String[] { text("envReport.site.standard"), safe(dto.getStandardName()) });
        }
        addTwoColumns(document, text("envReport.section.site"), site);
        addTwoColumns(document, text("envReport.section.conditions"),
                List.of(new String[] { text("envReport.conditions.waterTemp"), safe(dto.getWaterTemp()) },
                        new String[] { text("envReport.conditions.ambientTemp"), safe(dto.getAmbientTemp()) },
                        new String[] { text("envReport.conditions.weather"), safe(dto.getWeather()) },
                        new String[] { text("envReport.conditions.preservation"), safe(dto.getPreservation()) }));

        addResults(document, dto.getReportRows(), certificate);
        if (certificate) {
            addComplianceSummary(document, dto);
        } else {
            document.add(new Phrase("\n" + text("envReport.notConformity") + "\n", cellFont));
        }
        MicroEnvironmentalCertificateRows.append(document, dto.getMicrobiology(), sectionFont, cellFont);
        addSignatures(document, dto);

        document.close();
        return buf.toByteArray();
    }

    private void addResults(Document document, List<EnvironmentalReportRow> rows, boolean withStatus)
            throws DocumentException {
        document.add(new Phrase("\n" + text("envReport.section.results") + "\n\n", sectionFont));
        List<String> headers = new ArrayList<>(
                List.of(text("envReport.col.sample"), text("envReport.col.parameter"), text("envReport.col.result"),
                        text("envReport.col.unit"), text("envReport.col.method"), text("envReport.col.guideline")));
        if (withStatus) {
            headers.add(text("envReport.col.status"));
        }
        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        table.setWidths(withStatus ? new float[] { 2f, 2.5f, 1.5f, 1f, 1.5f, 2f, 1.5f }
                : new float[] { 2f, 2.5f, 1.5f, 1f, 1.5f, 2f });
        headers.forEach(header -> table.addCell(headerCell(header)));
        if (rows == null || rows.isEmpty()) {
            PdfPCell none = new PdfPCell(new Phrase(text("envReport.results.none"), cellFont));
            none.setColspan(headers.size());
            table.addCell(none);
        } else {
            for (EnvironmentalReportRow row : rows) {
                table.addCell(new Phrase(text("envReport.sample", safe(row.sampleNumber()), safe(row.sampleType())),
                        cellFont));
                table.addCell(new Phrase(safe(row.parameter()), cellFont));
                table.addCell(new Phrase(safe(row.result()), cellFont));
                table.addCell(new Phrase(blank(row.unit()), cellFont));
                table.addCell(new Phrase(blank(row.method()), cellFont));
                table.addCell(new Phrase(blank(row.guideline()), cellFont));
                if (withStatus) {
                    table.addCell(new Phrase(row.status() == null ? "" : status(row.status()), cellFont));
                }
            }
        }
        document.add(table);
    }

    private void addComplianceSummary(Document document, ComplianceReportOrderDTO dto) throws DocumentException {
        document.add(new Phrase("\n" + text("envReport.section.compliance") + "\n\n", sectionFont));
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3f, 2f, 2.5f, 1.5f });
        for (String header : new String[] { text("envReport.col.parameter"), text("envReport.col.result"),
                text("envReport.col.guideline"), text("envReport.col.status") }) {
            table.addCell(headerCell(header));
        }
        List<ComplianceEvaluationResult.ParameterResult> assessed = dto.getParameterResults() == null ? List.of()
                : dto.getParameterResults();
        for (ComplianceEvaluationResult.ParameterResult parameter : assessed) {
            table.addCell(new Phrase(safe(parameter.getDisplayName()), cellFont));
            String value = safe(parameter.getResultValue());
            if (parameter.getUnits() != null && !parameter.getUnits().isEmpty()) {
                value += " " + parameter.getUnits();
            }
            table.addCell(new Phrase(value, cellFont));
            table.addCell(new Phrase(safe(parameter.getThresholdDisplay()), cellFont));
            table.addCell(new Phrase(parameter.getStatus() == null ? "–" : status(parameter.getStatus()), cellFont));
        }
        document.add(table);
        if (!assessed.isEmpty() && dto.getComplianceStatus() != null) {
            String overall;
            try {
                overall = status(ComplianceStatus.valueOf(dto.getComplianceStatus()));
            } catch (IllegalArgumentException e) {
                overall = dto.getComplianceStatus();
            }
            document.add(new Phrase(
                    "\n" + MessageUtil.getMessage("envReport.conclusion",
                            new Object[] { overall, assessedResults(assessed), safe(dto.getStandardName()) }) + "\n",
                    labelFont));
        }
        document.add(new Phrase(text("envReport.decisionRule") + "\n", cellFont));
    }

    /**
     * A result judged against several standards has a line per standard and counts
     * once.
     */
    private static long assessedResults(List<ComplianceEvaluationResult.ParameterResult> assessed) {
        return assessed.stream()
                .map(parameter -> parameter.getAnalysisId() == null ? parameter : parameter.getAnalysisId()).distinct()
                .count();
    }

    private void addSignatures(Document document, ComplianceReportOrderDTO dto) throws DocumentException {
        document.add(new Phrase("\n\n" + text("envReport.section.signatures") + "\n\n", sectionFont));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell(signatureCell(dto.getAnalystSignature(), text("envReport.signature.analyst")));
        table.addCell(signatureCell(dto.getManagerSignature(), text("envReport.signature.manager")));
        document.add(table);
    }

    private void addTwoColumns(Document document, String title, List<String[]> rows) throws DocumentException {
        document.add(new Phrase("\n" + title + "\n\n", sectionFont));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 2f, 4f });
        for (String[] row : rows) {
            table.addCell(new Phrase(row[0], labelFont));
            table.addCell(new Phrase(row[1], cellFont));
        }
        document.add(table);
    }

    private PdfPCell headerCell(String header) {
        PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
        cell.setBackgroundColor(HEADER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell signatureCell(ComplianceReportOrderDTO.SignatureDTO signature, String role) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        if (signature != null) {
            cell.addElement(new Phrase(safe(signature.getSignerName()), labelFont));
            cell.addElement(new Phrase(safe(signature.getSignerRole()), cellFont));
            cell.addElement(new Phrase(safe(signature.getSignedAt()), cellFont));
        } else {
            cell.addElement(new Phrase(role + "\n–", cellFont));
        }
        return cell;
    }

    private static String status(ComplianceStatus status) {
        return text("envReport.status." + status.name());
    }

    private static String text(String key, String... args) {
        return args.length == 0 ? MessageUtil.getMessage(key) : MessageUtil.getMessage(key, (Object[]) args);
    }

    private static String safe(String value) {
        return value != null ? value : "–";
    }

    private static String blank(String value) {
        return value != null ? value : "";
    }
}
