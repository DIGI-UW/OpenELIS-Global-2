package org.openelisglobal.eqa.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EQAPerformanceReportPDFServiceImpl implements EQAPerformanceReportPDFService {

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
    private static final Font META_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private static final Font HEAD_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
    private static final Font CELL_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    private static final BaseColor HEAD_BG = new BaseColor(21, 96, 143);

    @Autowired
    private EQACycleDAO eqaCycleDAO;
    @Autowired
    private EQAParticipantResultDAO participantResultDAO;
    @Autowired
    private AnalyteService analyteService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SystemUserService systemUserService;

    @Override
    public byte[] generatePerformanceReport(Long cycleId, Long labEnrollmentId) {
        EQACycle cycle = eqaCycleDAO.get(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown cycle " + cycleId));
        EQAProgram scheme = cycle.getScheme();

        // Everything the PDF prints is resolved inside this transaction: the
        // renderer runs after the session would otherwise be gone.
        List<Row> rows = collectRows(cycleId, labEnrollmentId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 42, 42);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addHeader(document, cycle, scheme, labEnrollmentId, rows.size());
            addProgrammeSummary(document, rows);
            addSectionSummary(document, rows);
            addScoringTable(document, rows);
            addSignOff(document);
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("EQA performance report generation failed", e);
        }
        return out.toByteArray();
    }

    private List<Row> collectRows(Long cycleId, Long labEnrollmentId) {
        List<EQAParticipantResult> results = labEnrollmentId == null
                ? participantResultDAO.getAllMatching("cycle.id", cycleId)
                : participantResultDAO.getAllMatching(Map.of("cycle.id", cycleId, "labEnrollmentId", labEnrollmentId));

        List<Row> rows = new ArrayList<>();
        for (EQAParticipantResult result : results) {
            rows.add(new Row(sectionName(result), analyteName(result.getAnalyteId()), result.getResultValue(),
                    result.getResultUnit(), result.getZScore(), result.getPerformanceStatus(),
                    result.getSubmissionStatus(), analystName(result.getAssignedAnalystId()),
                    result.getScoreReceivedAt() == null ? null : formatDate(result.getScoreReceivedAt())));
        }
        rows.sort(Comparator.comparing(Row::section, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Row::analyte, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    private void addHeader(Document document, EQACycle cycle, EQAProgram scheme, Long labEnrollmentId, int rowCount)
            throws DocumentException {
        document.add(paragraph(MessageUtil.getMessage("eqa.report.title"), TITLE_FONT, 6f));

        PdfPTable meta = new PdfPTable(new float[] { 1f, 2.4f, 1f, 2.4f });
        meta.setWidthPercentage(100);
        metaCell(meta, MessageUtil.getMessage("eqa.report.scheme"), scheme == null ? "—" : scheme.getName());
        metaCell(meta, MessageUtil.getMessage("eqa.report.provider"),
                scheme == null || GenericValidator.isBlankOrNull(scheme.getProvider()) ? "—" : scheme.getProvider());
        metaCell(meta, MessageUtil.getMessage("eqa.report.schemeType"),
                scheme == null || scheme.getSchemeType() == null ? "—" : scheme.getSchemeType().name());
        metaCell(meta, MessageUtil.getMessage("eqa.report.cycle"), cycleIdentifier(cycle));
        metaCell(meta, MessageUtil.getMessage("eqa.report.period"), period(cycle));
        metaCell(meta, MessageUtil.getMessage("eqa.report.laboratory"), siteName());
        metaCell(meta, MessageUtil.getMessage("eqa.report.participant"),
                labEnrollmentId == null ? MessageUtil.getMessage("eqa.report.allParticipants")
                        : String.valueOf(labEnrollmentId));
        metaCell(meta, MessageUtil.getMessage("eqa.report.generated"),
                formatDate(new java.sql.Timestamp(System.currentTimeMillis())));
        document.add(meta);

        if (rowCount == 0) {
            document.add(paragraph(MessageUtil.getMessage("eqa.report.noResults"), META_FONT, 12f));
        }
    }

    private void addProgrammeSummary(Document document, List<Row> rows) throws DocumentException {
        document.add(paragraph(MessageUtil.getMessage("eqa.report.summary.programme"), SECTION_FONT, 14f));
        Tally tally = Tally.of(rows);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        headerRow(table, MessageUtil.getMessage("eqa.report.summary.total"),
                MessageUtil.getMessage("eqa.report.summary.scored"),
                MessageUtil.getMessage("eqa.report.summary.acceptable"),
                MessageUtil.getMessage("eqa.report.summary.questionable"),
                MessageUtil.getMessage("eqa.report.summary.unacceptable"),
                MessageUtil.getMessage("eqa.report.summary.acceptableRate"));
        bodyRow(table, String.valueOf(tally.total), String.valueOf(tally.scored), String.valueOf(tally.acceptable),
                String.valueOf(tally.questionable), String.valueOf(tally.unacceptable), tally.acceptableRate());
        document.add(table);
    }

    private void addSectionSummary(Document document, List<Row> rows) throws DocumentException {
        Map<String, Tally> bySection = new LinkedHashMap<>();
        for (Row row : rows) {
            bySection.computeIfAbsent(row.sectionLabel(), key -> new Tally()).add(row);
        }
        if (bySection.isEmpty()) {
            return;
        }

        document.add(paragraph(MessageUtil.getMessage("eqa.report.summary.section"), SECTION_FONT, 14f));
        PdfPTable table = new PdfPTable(new float[] { 2.2f, 1f, 1f, 1f, 1f, 1f, 1.2f });
        table.setWidthPercentage(100);
        headerRow(table, MessageUtil.getMessage("eqa.report.table.section"),
                MessageUtil.getMessage("eqa.report.summary.total"), MessageUtil.getMessage("eqa.report.summary.scored"),
                MessageUtil.getMessage("eqa.report.summary.acceptable"),
                MessageUtil.getMessage("eqa.report.summary.questionable"),
                MessageUtil.getMessage("eqa.report.summary.unacceptable"),
                MessageUtil.getMessage("eqa.report.summary.acceptableRate"));
        for (Map.Entry<String, Tally> entry : bySection.entrySet()) {
            Tally tally = entry.getValue();
            bodyRow(table, entry.getKey(), String.valueOf(tally.total), String.valueOf(tally.scored),
                    String.valueOf(tally.acceptable), String.valueOf(tally.questionable),
                    String.valueOf(tally.unacceptable), tally.acceptableRate());
        }
        document.add(table);
    }

    private void addScoringTable(Document document, List<Row> rows) throws DocumentException {
        if (rows.isEmpty()) {
            return;
        }
        document.add(paragraph(MessageUtil.getMessage("eqa.report.table.title"), SECTION_FONT, 14f));

        PdfPTable table = new PdfPTable(new float[] { 1.8f, 1.8f, 1.2f, 0.8f, 1f, 1.3f, 1.6f, 1.3f });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        headerRow(table, MessageUtil.getMessage("eqa.report.table.section"),
                MessageUtil.getMessage("eqa.report.table.analyte"), MessageUtil.getMessage("eqa.report.table.reported"),
                MessageUtil.getMessage("eqa.report.table.unit"), MessageUtil.getMessage("eqa.report.table.zscore"),
                MessageUtil.getMessage("eqa.report.table.performance"),
                MessageUtil.getMessage("eqa.report.table.analyst"), MessageUtil.getMessage("eqa.report.table.scored"));
        for (Row row : rows) {
            bodyRow(table, row.sectionLabel(), dash(row.analyte()), dash(row.reported()), dash(row.unit()),
                    row.zLabel(), row.performanceLabel(), dash(row.analyst()), dash(row.scoredAt()));
        }
        document.add(table);
    }

    private void addSignOff(Document document) throws DocumentException {
        document.add(paragraph(MessageUtil.getMessage("eqa.report.signoff.title"), SECTION_FONT, 18f));
        PdfPTable table = new PdfPTable(new float[] { 1f, 2f, 1f, 1.4f });
        table.setWidthPercentage(100);
        metaCell(table, MessageUtil.getMessage("eqa.report.signoff.reviewedBy"), " ");
        metaCell(table, MessageUtil.getMessage("eqa.report.signoff.date"), " ");
        document.add(table);
    }

    // ---- rendering helpers ----

    private Paragraph paragraph(String text, Font font, float spacingBefore) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(4f);
        return paragraph;
    }

    private void metaCell(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEAD_FONT));
        labelCell.setBackgroundColor(HEAD_BG);
        labelCell.setPadding(4f);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, CELL_FONT));
        valueCell.setPadding(4f);
        table.addCell(valueCell);
    }

    private void headerRow(PdfPTable table, String... labels) {
        for (String label : labels) {
            PdfPCell cell = new PdfPCell(new Phrase(label, HEAD_FONT));
            cell.setBackgroundColor(HEAD_BG);
            cell.setPadding(4f);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private void bodyRow(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, CELL_FONT));
            cell.setPadding(4f);
            table.addCell(cell);
        }
    }

    private static String dash(String value) {
        return GenericValidator.isBlankOrNull(value) ? "—" : value;
    }

    // ---- data helpers ----

    private String cycleIdentifier(EQACycle cycle) {
        String number = "#" + cycle.getCycleNumber();
        return GenericValidator.isBlankOrNull(cycle.getCycleName()) ? number : number + " — " + cycle.getCycleName();
    }

    private String period(EQACycle cycle) {
        // A cycle with neither date renders one dash, not "— — —": three dashes in
        // a row read as a rendering fault on a document someone signs.
        if (cycle.getPlannedStartDate() == null && cycle.getPlannedEndDate() == null) {
            return "—";
        }
        String start = cycle.getPlannedStartDate() == null ? "—" : cycle.getPlannedStartDate().toString();
        String end = cycle.getPlannedEndDate() == null ? "—" : cycle.getPlannedEndDate().toString();
        return start + " — " + end;
    }

    private String siteName() {
        String siteName = ConfigurationProperties.getInstance()
                .getPropertyValue(ConfigurationProperties.Property.SiteName);
        return GenericValidator.isBlankOrNull(siteName) ? "—" : siteName;
    }

    /**
     * The lab unit that ran the analyte, taken from the linked analysis when the
     * result came through standard result entry and falling back to the scheme's
     * own section otherwise.
     */
    private String sectionName(EQAParticipantResult result) {
        if (result.getAnalysisId() != null) {
            try {
                Analysis analysis = analysisService.get(String.valueOf(result.getAnalysisId()));
                if (analysis != null && analysis.getTest() != null && analysis.getTest().getTestSection() != null) {
                    return analysis.getTest().getTestSection().getTestSectionName();
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "sectionName",
                        "Could not resolve the section for analysis " + result.getAnalysisId());
            }
        }
        EQAProgram scheme = result.getCycle() == null ? null : result.getCycle().getScheme();
        if (scheme != null && scheme.getTestSection() != null) {
            return scheme.getTestSection().getTestSectionName();
        }
        return null;
    }

    private String analyteName(Long analyteId) {
        if (analyteId != null) {
            try {
                Analyte analyte = analyteService.get(String.valueOf(analyteId));
                if (analyte != null && analyte.getAnalyteName() != null) {
                    return analyte.getAnalyteName();
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "analyteName",
                        "Could not resolve analyte name for id " + analyteId);
            }
        }
        return analyteId == null ? null : "analyte " + analyteId;
    }

    private String analystName(Long analystId) {
        if (analystId == null) {
            return null;
        }
        try {
            SystemUser user = systemUserService.get(String.valueOf(analystId));
            if (user != null) {
                return user.getNameForDisplay();
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "analystName",
                    "Could not resolve analyst name for id " + analystId);
        }
        return String.valueOf(analystId);
    }

    private static String formatDate(java.sql.Timestamp timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(timestamp);
    }

    private record Row(String section, String analyte, String reported, String unit, BigDecimal zScore,
            EQAPerformanceStatus performance, EQASubmissionStatus submissionStatus, String analyst, String scoredAt) {

        String sectionLabel() {
            return GenericValidator.isBlankOrNull(section) ? MessageUtil.getMessage("eqa.report.unassignedSection")
                    : section;
        }

        String zLabel() {
            return zScore == null ? "—" : zScore.stripTrailingZeros().toPlainString();
        }

        String performanceLabel() {
            return performance == null ? MessageUtil.getMessage("eqa.report.summary.unscored") : performance.name();
        }
    }

    private static final class Tally {
        private int total;
        private int scored;
        private int acceptable;
        private int questionable;
        private int unacceptable;

        static Tally of(List<Row> rows) {
            Tally tally = new Tally();
            rows.forEach(tally::add);
            return tally;
        }

        void add(Row row) {
            total++;
            if (row.performance() == null) {
                return;
            }
            scored++;
            switch (row.performance()) {
            case ACCEPTABLE:
                acceptable++;
                break;
            case QUESTIONABLE:
                questionable++;
                break;
            default:
                unacceptable++;
                break;
            }
        }

        /** Acceptable as a share of scored results — unscored rows never count. */
        String acceptableRate() {
            if (scored == 0) {
                return "—";
            }
            return BigDecimal.valueOf(acceptable * 100L).divide(BigDecimal.valueOf(scored), 1, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString() + "%";
        }
    }
}
