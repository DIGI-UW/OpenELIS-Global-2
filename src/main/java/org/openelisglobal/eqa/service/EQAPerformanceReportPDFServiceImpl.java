package org.openelisglobal.eqa.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.dao.AnalysisDAO;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.dao.AnalyteDAO;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.eqa.dao.EQACycleDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.qaevent.service.EqaScoreNceService;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.systemuser.dao.SystemUserDAO;
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
    private static final BaseColor SECTION_BG = new BaseColor(233, 238, 242);
    private static final Font SECTION_ROW_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
    private static final Font FOOTER_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

    /** Printed wherever a value is absent, so a blank cell never reads as zero. */
    private static final String DASH = "—";

    @Autowired
    private EQACycleDAO eqaCycleDAO;
    @Autowired
    private EQAParticipantResultDAO participantResultDAO;
    @Autowired
    private AnalyteDAO analyteDAO;
    @Autowired
    private AnalysisDAO analysisDAO;
    @Autowired
    private SystemUserDAO systemUserDAO;
    @Autowired
    private EQAPanelSampleDAO panelSampleDAO;
    @Autowired
    private NCEventService ncEventService;

    @Override
    public byte[] generatePerformanceReport(Long cycleId) {
        EQACycle cycle = eqaCycleDAO.get(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown cycle " + cycleId));
        EQAProgram scheme = cycle.getScheme();

        // Everything the PDF prints is resolved inside this transaction: the
        // renderer runs after the session would otherwise be gone.
        List<Row> rows = collectRows(cycleId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 42, 42);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addHeader(document, cycle, scheme, rows.size());
            addProgrammeSummary(document, rows);
            addSectionSummary(document, rows);
            addScoringTable(document, rows);
            addSignOff(document);
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("EQA performance report generation failed", e);
        }
        return stampPageNumbers(out.toByteArray());
    }

    /**
     * "Page 1 of 3" on every page, added in a second pass because the total is only
     * known once the document is closed. A signed, filed document needs to show
     * whether a page is missing.
     */
    private byte[] stampPageNumbers(byte[] pdf) {
        ByteArrayOutputStream numbered = new ByteArrayOutputStream();
        try {
            PdfReader reader = new PdfReader(pdf);
            PdfStamper stamper = new PdfStamper(reader, numbered);
            int pages = reader.getNumberOfPages();
            for (int page = 1; page <= pages; page++) {
                Rectangle size = reader.getPageSize(page);
                ColumnText.showTextAligned(stamper.getOverContent(page), Element.ALIGN_RIGHT,
                        new Phrase(MessageUtil.getMessage("eqa.report.footer.page", new Object[] { page, pages }),
                                FOOTER_FONT),
                        size.getRight(36), size.getBottom(24), 0);
            }
            stamper.close();
            reader.close();
        } catch (DocumentException | java.io.IOException e) {
            // The report itself is sound; losing the footer is not worth failing on.
            LogEvent.logWarn(this.getClass().getSimpleName(), "stampPageNumbers", e.getMessage());
            return pdf;
        }
        return numbered.toByteArray();
    }

    /**
     * A DRAFT row is a lab's unsubmitted working value. It carries no standing on a
     * document a reviewer signs, and counting it would inflate the denominator of
     * every rate on the page.
     */
    private List<Row> collectRows(Long cycleId) {
        List<EQAParticipantResult> results = participantResultDAO.getAllMatching("cycle.id", cycleId).stream()
                .filter(result -> result.getSubmissionStatus() != EQASubmissionStatus.DRAFT).toList();

        Map<String, String> sections = sectionsByAnalysis(results);
        Map<String, String> analytes = analyteNames(results);
        Map<String, String> analysts = analystNames(results);
        Map<Long, String> targets = revealedTargets(results);
        Map<Long, String> nceNumbers = nceNumbers(results);

        List<Row> rows = new ArrayList<>();
        for (EQAParticipantResult result : results) {
            // An id that resolves to no row still prints as the id: a blank cell
            // would hide which analyte or analyst the data fault is on.
            String analyteKey = key(result.getAnalyteId());
            String analystKey = key(result.getAssignedAnalystId());
            rows.add(new Row(result.getRound() == null ? null : result.getRound().getRoundNumber(),
                    sectionName(result, sections), analytes.getOrDefault(analyteKey, analyteKey),
                    result.getResultValue(), targets.get(result.getId()), result.getResultUnit(), result.getZScore(),
                    result.getPerformanceStatus(), nceNumbers.get(result.getId()),
                    analysts.getOrDefault(analystKey, analystKey),
                    result.getSubmittedAt() == null ? null : formatDay(result.getSubmittedAt()),
                    result.getScoreReceivedAt() == null ? null : formatDay(result.getScoreReceivedAt())));
        }
        rows.sort(Comparator.comparing(Row::round, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Row::section, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Row::analyte, Comparator.nullsLast(Comparator.naturalOrder())));
        return rows;
    }

    private void addHeader(Document document, EQACycle cycle, EQAProgram scheme, int rowCount)
            throws DocumentException {
        document.add(paragraph(MessageUtil.getMessage("eqa.report.title"), TITLE_FONT, 6f));

        PdfPTable meta = new PdfPTable(new float[] { 1f, 2.4f, 1f, 2.4f });
        meta.setWidthPercentage(100);
        metaCell(meta, MessageUtil.getMessage("eqa.report.scheme"), scheme == null ? DASH : dash(scheme.getName()));
        metaCell(meta, MessageUtil.getMessage("eqa.report.provider"),
                scheme == null ? DASH : dash(scheme.getProvider()));
        metaCell(meta, MessageUtil.getMessage("eqa.report.schemeType"),
                scheme == null ? DASH : schemeTypeLabel(scheme.getSchemeType()));
        metaCell(meta, MessageUtil.getMessage("eqa.report.cycle"), cycleIdentifier(cycle));
        metaCell(meta, MessageUtil.getMessage("eqa.report.period"), period(cycle));
        metaCell(meta, MessageUtil.getMessage("eqa.report.laboratory"), siteName());
        metaCell(meta, MessageUtil.getMessage("eqa.report.generated"), formatDate(new Date()));
        document.add(meta);

        if (rowCount == 0) {
            document.add(paragraph(MessageUtil.getMessage("eqa.report.noResults"), META_FONT, 12f));
        }
    }

    private void addProgrammeSummary(Document document, List<Row> rows) throws DocumentException {
        document.add(paragraph(MessageUtil.getMessage("eqa.report.summary.programme"), SECTION_FONT, 14f));
        Tally tally = new Tally();
        rows.forEach(tally::add);

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

    /**
     * A cycle can run several rounds, and the same analyte is reported in each. The
     * round column is what keeps two such rows apart.
     */
    private void addScoringTable(Document document, List<Row> rows) throws DocumentException {
        if (rows.isEmpty()) {
            return;
        }
        document.add(paragraph(MessageUtil.getMessage("eqa.report.table.title"), SECTION_FONT, 14f));

        PdfPTable table = new PdfPTable(new float[] { 0.8f, 2f, 1.5f, 1.5f, 0.9f, 1.4f, 1.4f, 1.4f, 1.2f, 1.2f });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        headerRow(table, MessageUtil.getMessage("eqa.report.table.round"),
                MessageUtil.getMessage("eqa.report.table.analyte"), MessageUtil.getMessage("eqa.report.table.reported"),
                MessageUtil.getMessage("eqa.report.table.target"), MessageUtil.getMessage("eqa.report.table.zscore"),
                MessageUtil.getMessage("eqa.report.table.performance"), MessageUtil.getMessage("eqa.report.table.nce"),
                MessageUtil.getMessage("eqa.report.table.analyst"),
                MessageUtil.getMessage("eqa.report.table.submitted"),
                MessageUtil.getMessage("eqa.report.table.scored"));

        // Section rides above its rows as a banner rather than repeating in every
        // line: as a column it wrapped each row onto two lines and made the table
        // twice as long for no added information.
        String currentSection = null;
        for (Row row : rows) {
            if (!row.sectionLabel().equals(currentSection)) {
                currentSection = row.sectionLabel();
                PdfPCell banner = new PdfPCell(new Phrase(currentSection, SECTION_ROW_FONT));
                banner.setColspan(10);
                banner.setBackgroundColor(SECTION_BG);
                banner.setPadding(4f);
                table.addCell(banner);
            }
            bodyRow(table, row.roundLabel(), row.analyteLabel(), row.reportedLabel(), row.targetLabel(), row.zLabel(),
                    row.performanceLabel(), dash(row.nceNumber()), dash(row.analyst()), dash(row.submittedAt()),
                    dash(row.scoredAt()));
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
        return GenericValidator.isBlankOrNull(value) ? DASH : value;
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
            return DASH;
        }
        String start = cycle.getPlannedStartDate() == null ? DASH : cycle.getPlannedStartDate().toString();
        String end = cycle.getPlannedEndDate() == null ? DASH : cycle.getPlannedEndDate().toString();
        return start + " — " + end;
    }

    private String siteName() {
        return dash(ConfigurationProperties.getInstance().getPropertyValue(ConfigurationProperties.Property.SiteName));
    }

    /**
     * The lab unit that ran the analyte, taken from the linked analysis when the
     * result came through standard result entry and falling back to the scheme's
     * own section otherwise.
     */
    private String sectionName(EQAParticipantResult result, Map<String, String> sectionsByAnalysis) {
        String fromAnalysis = sectionsByAnalysis.get(key(result.getAnalysisId()));
        if (fromAnalysis != null) {
            return fromAnalysis;
        }
        EQAProgram scheme = result.getCycle() == null ? null : result.getCycle().getScheme();
        return scheme == null || scheme.getTestSection() == null ? null : scheme.getTestSection().getTestSectionName();
    }

    /**
     * One IN query per referenced table, not a lookup per row — as do
     * {@link #analyteNames} and {@link #analystNames}. An id that no longer
     * resolves is simply absent from the map, so the caller's fallback decides what
     * the cell reads.
     */
    private Map<String, String> sectionsByAnalysis(List<EQAParticipantResult> results) {
        List<String> ids = idsOf(results, EQAParticipantResult::getAnalysisId);
        Map<String, String> sections = new HashMap<>();
        for (Analysis analysis : ids.isEmpty() ? List.<Analysis>of() : analysisDAO.get(ids)) {
            if (analysis.getTest() != null && analysis.getTest().getTestSection() != null) {
                sections.put(analysis.getId(), analysis.getTest().getTestSection().getTestSectionName());
            }
        }
        return sections;
    }

    private Map<String, String> analyteNames(List<EQAParticipantResult> results) {
        List<String> ids = idsOf(results, EQAParticipantResult::getAnalyteId);
        Map<String, String> names = new HashMap<>();
        for (Analyte analyte : ids.isEmpty() ? List.<Analyte>of() : analyteDAO.get(ids)) {
            if (analyte.getAnalyteName() != null) {
                names.put(analyte.getId(), analyte.getAnalyteName());
            }
        }
        return names;
    }

    private Map<String, String> analystNames(List<EQAParticipantResult> results) {
        List<String> ids = idsOf(results, EQAParticipantResult::getAssignedAnalystId);
        Map<String, String> names = new HashMap<>();
        for (SystemUser user : ids.isEmpty() ? List.<SystemUser>of() : systemUserDAO.get(ids)) {
            if (user.getNameForDisplay() != null) {
                names.put(user.getId(), user.getNameForDisplay());
            }
        }
        return names;
    }

    /**
     * The assigned value each in-house result was scored against, keyed by result
     * id. Only panels that have actually been unblinded contribute: a sealed target
     * is the whole point of a blinded panel (FR-V2.4-03), and this report is served
     * under the EQA read permission, not the unblind one. External PT keeps its
     * targets at the provider, so those rows have none either way.
     */
    private Map<Long, String> revealedTargets(List<EQAParticipantResult> results) {
        List<Long> panelSampleIds = results.stream().map(EQAParticipantResult::getPanelSampleId)
                .filter(Objects::nonNull).distinct().toList();
        if (panelSampleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, EQAPanelSample> samples = new HashMap<>();
        for (EQAPanelSample sample : panelSampleDAO.get(panelSampleIds)) {
            if (sample.getPanel() != null && sample.getPanel().getUnblindedAt() != null) {
                samples.put(sample.getId(), sample);
            }
        }

        Map<Long, String> targets = new HashMap<>();
        for (EQAParticipantResult result : results) {
            EQAPanelSample sample = samples.get(result.getPanelSampleId());
            if (sample != null && !GenericValidator.isBlankOrNull(sample.getTargetValue())) {
                targets.put(result.getId(), withUnit(sample.getTargetValue(), sample.getTargetUnit()));
            }
        }
        return targets;
    }

    /**
     * The non-conformity an unacceptable score raised (FR-V2.3-01), so the report
     * shows the investigation it is evidence for. Only unacceptable rows are looked
     * up — no other tier creates one — which keeps this to a handful of queries
     * rather than one per printed line.
     */
    private Map<Long, String> nceNumbers(List<EQAParticipantResult> results) {
        Map<Long, String> numbers = new HashMap<>();
        for (EQAParticipantResult result : results) {
            if (result.getPerformanceStatus() != EQAPerformanceStatus.UNACCEPTABLE) {
                continue;
            }
            NcEvent nce = ncEventService.findByTriggerSource(EqaScoreNceService.TRIGGER_SOURCE_EQA_UNACCEPTABLE,
                    String.valueOf(result.getId()));
            if (nce != null && nce.getNceNumber() != null) {
                numbers.put(result.getId(), nce.getNceNumber());
            }
        }
        return numbers;
    }

    /** Values carry their unit inline, so the table spends no column on it. */
    private static String withUnit(String value, String unit) {
        if (GenericValidator.isBlankOrNull(value)) {
            return null;
        }
        return GenericValidator.isBlankOrNull(unit) ? value : value + " " + unit;
    }

    private static List<String> idsOf(List<EQAParticipantResult> results, Function<EQAParticipantResult, Long> idOf) {
        return results.stream().map(idOf).filter(Objects::nonNull).map(String::valueOf).distinct().toList();
    }

    private static String key(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private static String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    /**
     * Day only, for the table's date columns. The clock time wrapped the cell in
     * two, and on a quarterly cycle nobody reads the minute a result was submitted.
     * The header's generated-at stamp keeps its time.
     */
    private static String formatDay(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private static String schemeTypeLabel(EQASchemeType schemeType) {
        return schemeType == null ? DASH
                : MessageUtil.getMessage("eqa.schemeType." + schemeType.name().toLowerCase(Locale.ROOT));
    }

    private record Row(Integer round, String section, String analyte, String reported, String target, String unit,
            BigDecimal zScore, EQAPerformanceStatus performance, String nceNumber, String analyst, String submittedAt,
            String scoredAt) {

        String reportedLabel() {
            return dash(withUnit(reported, unit));
        }

        String targetLabel() {
            return dash(target);
        }

        String roundLabel() {
            return round == null ? DASH : String.valueOf(round);
        }

        String sectionLabel() {
            return GenericValidator.isBlankOrNull(section) ? MessageUtil.getMessage("eqa.report.unassignedSection")
                    : section;
        }

        String analyteLabel() {
            return dash(analyte);
        }

        String zLabel() {
            return zScore == null ? DASH : zScore.stripTrailingZeros().toPlainString();
        }

        String performanceLabel() {
            return performance == null ? MessageUtil.getMessage("eqa.report.summary.unscored")
                    : MessageUtil.getMessage("eqa.performanceStatus." + performance.name().toLowerCase(Locale.ROOT));
        }
    }

    private static final class Tally {
        private int total;
        private int scored;
        private int acceptable;
        private int questionable;
        private int unacceptable;

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
                return DASH;
            }
            return BigDecimal.valueOf(acceptable * 100L).divide(BigDecimal.valueOf(scored), 1, RoundingMode.HALF_UP)
                    .stripTrailingZeros().toPlainString() + "%";
        }
    }
}
