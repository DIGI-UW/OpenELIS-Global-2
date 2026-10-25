package org.openelisglobal.reports.action.implementation;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.common.util.PdfReportText;
import org.openelisglobal.internationalization.UiMessages;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openelisglobal.reports.action.implementation.reportBeans.PatientReportOrder;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportCallbackLine;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportComponentLine;
import org.openelisglobal.reports.action.implementation.reportBeans.ReportSampleLine;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPCellEvent;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

/**
 * The patient report (FRS OGC-1111 Part A): one report per order, each starting
 * on a new page numbered from 1. Page 1 carries the full header, the patient
 * and order card and the Samples block; later pages a compact header with the
 * patient strip. The results follow by section under a column header repeated
 * on every page, then the sign-off and "End of report". The footer of every
 * page carries the flag legend, the issue time, the order and patient, and
 * "Page x of y".
 *
 * <p>
 * Designed for monochrome printers and photocopies: black and two grays only,
 * with abnormal and critical results carried by arrows, weight and bars rather
 * than colour. Every label comes from the interface's translation catalogue, so
 * the report reads in the screens' language and wording.
 */
final class PatientResultsPdf {

    /** Site settings the report prints. */
    record Settings(String siteName, String additionalSiteInfo, byte[] leftLogo, byte[] rightLogo,
            boolean useBillingNumber, String billingNumberLabel, boolean useSTNumber, boolean useContactTracing,
            byte[] labDirectorSignature, String labDirectorName, String labDirectorTitle) {
    }

    static final Color GRAY = new Color(0x59, 0x59, 0x59);
    static final Color CRITICAL_FILL = new Color(0xe6, 0xe6, 0xe6);

    private static final float SIDE_MARGIN = 30;
    private static final float TOP_MARGIN = 20;
    private static final float FOOTER_HEIGHT = 26;
    private static final float BOTTOM_MARGIN = TOP_MARGIN + FOOTER_HEIGHT;
    private static final float CONTINUATION_HEIGHT = 34;
    private static final float LOGO = 48;
    private static final float ACCREDITATION_LOGO = 32;
    private static final float[] RESULT_WIDTHS = { 152, 22, 76, 70, 38, 76, 42, 62 };
    private static final int COLUMNS = RESULT_WIDTHS.length;
    private static final int RANGE_COLUMN = 5;
    private static final int UNITS_COLUMN = 6;
    private static final float INDENT = 10;

    private static final String UP = "↑";
    private static final String DOWN = "↓";
    private static final String DOUBLE_UP = "⇈";
    private static final String DOUBLE_DOWN = "⇊";
    private static final String DAGGER = "†";
    private static final String DOT = " · ";

    private final Settings settings;
    private final UiMessages messages;
    private final Fonts fonts = new Fonts();

    private PatientResultsPdf(Settings settings, UiMessages messages) {
        this.settings = settings;
        this.messages = messages;
    }

    /**
     * @param items  the result rows, sorted by order, then section
     * @param orders what each order prints once, by the rows' accession number
     */
    static byte[] render(Settings settings, UiMessages messages, List<ClinicalPatientData> items,
            Map<String, PatientReportOrder> orders) {
        return new PatientResultsPdf(settings, messages).render(items, orders);
    }

    private byte[] render(List<ClinicalPatientData> items, Map<String, PatientReportOrder> orders) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), SIDE_MARGIN, SIDE_MARGIN, TOP_MARGIN,
                BOTTOM_MARGIN);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        Pages pages = new Pages();
        writer.setPageEvent(pages);
        document.open();
        int start = 0;
        while (start < items.size()) {
            int end = start;
            String accession = items.get(start).getAccessionNumber();
            while (end < items.size() && Objects.equals(items.get(end).getAccessionNumber(), accession)) {
                end++;
            }
            List<ClinicalPatientData> rows = items.subList(start, end);
            PatientReportOrder order = orders == null ? null : orders.get(accession);
            if (order == null) {
                order = new PatientReportOrder();
            }
            if (start > 0) {
                document.setMargins(SIDE_MARGIN, SIDE_MARGIN, TOP_MARGIN, BOTTOM_MARGIN);
                document.newPage();
            }
            pages.startOrder(
                    new OrderPages(writer, order, rows.get(0), title(order), anyDagger(rows), writer.getPageNumber()));
            document.setMargins(SIDE_MARGIN, SIDE_MARGIN, TOP_MARGIN + CONTINUATION_HEIGHT, BOTTOM_MARGIN);
            addOrder(document, order, rows);
            start = end;
        }
        document.close();
        return out.toByteArray();
    }

    private String title(PatientReportOrder order) {
        return messages.get(order.isComplete() ? "report.patient.title.final" : "report.patient.title.partial");
    }

    private static boolean anyDagger(List<ClinicalPatientData> rows) {
        return rows.stream().anyMatch(row -> !row.isInAccreditedScope());
    }

    private void addOrder(Document document, PatientReportOrder order, List<ClinicalPatientData> rows) {
        ClinicalPatientData first = rows.get(0);
        document.add(header(order));
        if (order.isCorrected()) {
            document.add(correctedBanner(order));
        }
        document.add(card(order, first));
        document.add(samples(order));
        document.add(results(rows));
        document.add(signOff(order, rows));
    }

    // ----- page 1 header

    private PdfPTable header(PatientReportOrder order) {
        float width = contentWidth();
        float centre = width - 2 * LOGO - 3 * (ACCREDITATION_LOGO + 4) - 2 * 8;
        PdfPTable header = table(new float[] { LOGO, 8, centre, ACCREDITATION_LOGO + 4, ACCREDITATION_LOGO + 4,
                ACCREDITATION_LOGO + 4, 8, LOGO });
        header.addCell(imageCell(settings.leftLogo(), LOGO, LOGO, Element.ALIGN_CENTER));
        header.addCell(blank());
        Paragraph lines = new Paragraph();
        lines.setLeading(10);
        lines.add(new Phrase(nonNull(settings.siteName()) + "\n", fonts.bold(12, Color.BLACK)));
        lines.add(new Phrase(title(order) + "\n", fonts.bold(8, GRAY)));
        if (!GenericValidator.isBlankOrNull(settings.additionalSiteInfo())) {
            lines.add(new Phrase(settings.additionalSiteInfo() + "\n", fonts.regular(8, GRAY)));
        }
        String director = directorLine();
        if (director != null) {
            lines.add(new Phrase(director + "\n", fonts.regular(8, GRAY)));
        }
        if (!order.getAccreditingBodies().isEmpty()) {
            lines.add(new Phrase(
                    messages.format("report.patient.accred.notesLine", String.join(", ", order.getAccreditingBodies())),
                    fonts.regular(7, GRAY)));
        }
        PdfPCell centreCell = new PdfPCell();
        centreCell.addElement(lines);
        centreCell.setBorder(Rectangle.NO_BORDER);
        centreCell.setPadding(0);
        centreCell.setPaddingTop(-2);
        header.addCell(centreCell);
        List<byte[]> marks = order.getAccreditationLogos();
        for (int slot = 0; slot < 3; slot++) {
            header.addCell(imageCell(slot < marks.size() ? marks.get(slot) : null, ACCREDITATION_LOGO, LOGO,
                    Element.ALIGN_CENTER));
        }
        header.addCell(blank());
        header.addCell(imageCell(settings.rightLogo(), LOGO, LOGO, Element.ALIGN_CENTER));
        PdfPCell rule = new PdfPCell();
        rule.setColspan(8);
        rule.setBorder(Rectangle.BOTTOM);
        rule.setBorderWidthBottom(0.75f);
        rule.setFixedHeight(4);
        header.addCell(rule);
        return header;
    }

    /** "Lab director: name, title", or null when no director is configured. */
    private String directorLine() {
        if (GenericValidator.isBlankOrNull(settings.labDirectorName())) {
            return null;
        }
        String line = messages.get("report.patient.labDirector") + ": " + settings.labDirectorName();
        return GenericValidator.isBlankOrNull(settings.labDirectorTitle()) ? line
                : line + ", " + settings.labDirectorTitle();
    }

    private PdfPTable correctedBanner(PatientReportOrder order) {
        String issued = order.getReplacedIssueTime() == null ? "" : order.getReplacedIssueTime();
        String change = GenericValidator.isBlankOrNull(order.getChangeSummary())
                ? messages.get("report.patient.corrected.seeReplaced")
                : order.getChangeSummary();
        String line = messages.format("report.patient.corrected.line", issued, change);
        if (order.getVersion() != null) {
            line = messages.format("report.patient.version", order.getVersion()) + " " + line;
        }
        PdfPTable banner = table(new float[] { 1 });
        banner.setSpacingBefore(6);
        PdfPCell cell = new PdfPCell(new Phrase(line, fonts.bold(8, Color.BLACK)));
        cell.setBorderWidth(1.5f);
        cell.setPadding(3);
        cell.setPaddingLeft(6);
        banner.addCell(cell);
        return banner;
    }

    // ----- patient and order card

    private PdfPTable card(PatientReportOrder order, ClinicalPatientData row) {
        PdfPTable card = table(new float[] { 1, 0.035f, 1, 0.035f, 1 });
        card.setSpacingBefore(4);

        PdfPTable patient = block(messages.get("common.patient"));
        PdfPTable nameLine = new PdfPTable(new float[] { 3, 2 });
        nameLine.setWidthPercentage(100);
        nameLine.addCell(
                text(PdfReportText.plain(row.getPatientName()), fonts.bold(10, Color.BLACK), Element.ALIGN_LEFT));
        String gender = "0".equals(row.getGender()) ? "" : nonNull(row.getGender());
        nameLine.addCell(text(join(DOT, row.getAge(), gender), fonts.regular(8, Color.BLACK), Element.ALIGN_RIGHT));
        PdfPCell nameCell = new PdfPCell(nameLine);
        nameCell.setColspan(2);
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPadding(0);
        patient.addCell(nameCell);
        field(patient, messages.get("patient.dob"), row.getDob(), false);
        field(patient, messages.get("patient.merge.patientId"), row.getSubjectNumber(), true);
        field(patient, messages.get("patient.merge.nationalId"), row.getNationalId(), true);
        if (settings.useBillingNumber()) {
            field(patient, nonNull(settings.billingNumberLabel()), row.getBillingNumber(), true);
        }
        if (settings.useSTNumber() && !GenericValidator.isBlankOrNull(row.getStNumber())) {
            field(patient, messages.get("patient.st.number"), row.getStNumber(), true);
        }
        if (settings.useContactTracing()) {
            field(patient, messages.get("report.patient.contactTracing.indexName"), row.getContactTracingIndexName(),
                    false);
            field(patient, messages.get("report.patient.contactTracing.indexRecordNumber"),
                    row.getContactTracingIndexRecordNumber(), true);
        }

        String[] site = nonNull(row.getSiteInfo()).split("\\|", 2);
        PdfPTable orderBlock = block(messages.get("report.patient.block.order"));
        field(orderBlock, messages.get("sample.label.labnumber"), row.getAccessionNumber(), true);
        field(orderBlock, messages.get("common.program"), row.getLabOrderType(), false);
        field(orderBlock, messages.get("common.requester"), row.getContactInfo(), false);
        field(orderBlock, messages.get("report.patient.siteWard"), join(" / ", site[0], site.length > 1 ? site[1] : ""),
                false);

        PdfPTable dates = block(messages.get("report.patient.block.dates"));
        field(dates, messages.get("sample.label.orderdate"),
                order.getOrderDate() == null ? row.getOrderDate() : order.getOrderDate(), false);
        field(dates, messages.get("report.patient.completed"),
                order.getCompletedTime() == null ? messages.get("report.patient.completed.pending")
                        : order.getCompletedTime(),
                false);

        card.addCell(nested(patient));
        card.addCell(blank());
        card.addCell(nested(orderBlock));
        card.addCell(blank());
        card.addCell(nested(dates));
        return card;
    }

    private PdfPTable block(String heading) {
        PdfPTable block = new PdfPTable(new float[] { 72, 108 });
        block.setWidthPercentage(100);
        PdfPCell title = text(heading.toUpperCase(messages.getLocale()), fonts.bold(6.5f, GRAY), Element.ALIGN_LEFT);
        title.setColspan(2);
        title.setBorder(Rectangle.BOTTOM);
        title.setBorderWidthBottom(0.5f);
        title.setBorderColorBottom(GRAY);
        title.setPaddingBottom(2);
        block.addCell(title);
        return block;
    }

    private void field(PdfPTable block, String label, String value, boolean code) {
        block.addCell(compact(text(label, fonts.regular(7.5f, GRAY), Element.ALIGN_LEFT)));
        block.addCell(compact(text(PdfReportText.plain(value), code ? fonts.mono(8) : fonts.regular(8, Color.BLACK),
                Element.ALIGN_LEFT)));
    }

    /** A card or Samples line at the 9.5 pt pitch of the design. */
    private static PdfPCell compact(PdfPCell cell) {
        cell.setPaddingTop(0);
        cell.setPaddingBottom(1);
        return cell;
    }

    // ----- samples block

    private PdfPTable samples(PatientReportOrder order) {
        PdfPTable samples = table(new float[] { 18, 150, 62, 62, contentWidth() - 292 });
        samples.setSpacingBefore(4);
        samples.setKeepTogether(true);
        for (String label : new String[] { messages.get("report.patient.sample.number"),
                messages.get("report.patient.samples"), messages.get("common.collected"),
                messages.get("common.received"), messages.get("report.patient.sample.condition") }) {
            PdfPCell heading = text(label.toUpperCase(messages.getLocale()), fonts.bold(6.5f, GRAY),
                    Element.ALIGN_LEFT);
            heading.setBorder(Rectangle.BOTTOM);
            heading.setBorderWidthBottom(0.5f);
            heading.setBorderColorBottom(GRAY);
            heading.setPaddingBottom(2);
            samples.addCell(heading);
        }
        samples.getRow(0).getCells()[0].setHorizontalAlignment(Element.ALIGN_CENTER);
        String notRecorded = messages.get("report.patient.sample.notRecorded");
        for (ReportSampleLine line : order.getSamples()) {
            samples.addCell(compact(text(line.number(), fonts.mono(8), Element.ALIGN_CENTER)));
            samples.addCell(compact(text(line.type(), fonts.regular(8, Color.BLACK), Element.ALIGN_LEFT)));
            samples.addCell(compact(text(line.collected() == null ? notRecorded : line.collected(),
                    fonts.regular(7.5f, Color.BLACK), Element.ALIGN_LEFT)));
            samples.addCell(compact(text(line.received(), fonts.regular(7.5f, Color.BLACK), Element.ALIGN_LEFT)));
            if (line.rejected()) {
                String reason = GenericValidator.isBlankOrNull(line.rejectionReason())
                        ? messages.get("report.patient.result.rejected")
                        : messages.format("report.patient.sample.rejected", line.rejectionReason());
                samples.addCell(compact(text(reason, fonts.bold(8, Color.BLACK), Element.ALIGN_LEFT)));
            } else {
                samples.addCell(compact(text(line.conditions(), fonts.regular(8, Color.BLACK), Element.ALIGN_LEFT)));
            }
        }
        return samples;
    }

    // ----- results

    private PdfPTable results(List<ClinicalPatientData> rows) {
        PdfPTable results = table(RESULT_WIDTHS);
        results.setSpacingBefore(4);
        results.setSplitLate(true);
        results.setSplitRows(false);
        String[] headings = { messages.get("label.results.test"), messages.get("report.patient.col.sample"),
                messages.get("label.results.method"), messages.get("label.results.result"),
                messages.get("label.results.flag"), messages.get("label.results.range"),
                messages.get("label.validation.review.units"), messages.get("report.patient.col.completed") };
        int[] alignments = { Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_RIGHT,
                Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_LEFT, Element.ALIGN_LEFT };
        float[] widths = results.getAbsoluteWidths();
        for (int column = 0; column < COLUMNS; column++) {
            String label = headings[column].toUpperCase(messages.getLocale());
            PdfPCell heading = text(label, fonts.bold(wordFittingSize(label, widths[column] - 3, 6.5f), GRAY),
                    alignments[column]);
            heading.setBorder(Rectangle.BOTTOM);
            heading.setBorderWidthBottom(0.75f);
            heading.setPaddingBottom(2);
            if (column == 0) {
                heading.setPaddingLeft(8);
            }
            results.addCell(heading);
        }
        results.setHeaderRows(1);

        String section = null;
        String panel = null;
        for (ClinicalPatientData row : rows) {
            PdfPTable block = table(RESULT_WIDTHS);
            if (!Objects.equals(row.getTestSection(), section)) {
                section = row.getTestSection();
                panel = null;
                PdfPCell title = text(nonNull(section).toUpperCase(messages.getLocale()), fonts.bold(8.5f, Color.BLACK),
                        Element.ALIGN_LEFT);
                title.setColspan(COLUMNS);
                title.setBorder(Rectangle.BOTTOM);
                title.setBorderWidthBottom(0.5f);
                title.setBorderColorBottom(GRAY);
                title.setPaddingTop(2);
                title.setPaddingBottom(1.5f);
                block.addCell(title);
            }
            String rowPanel = row.getPanel() == null ? null : row.getPanel().getId();
            boolean inPanel = rowPanel != null && !GenericValidator.isBlankOrNull(row.getPanelName());
            if (inPanel && !rowPanel.equals(panel)) {
                PdfPCell panelName = text(PdfReportText.plain(row.getPanelName()), fonts.bold(8.5f, Color.BLACK),
                        Element.ALIGN_LEFT);
                panelName.setColspan(COLUMNS);
                panelName.setPaddingLeft(8);
                block.addCell(panelName);
            }
            panel = inPanel ? rowPanel : null;
            addResultRow(block, row, inPanel ? INDENT : 0);
            for (ReportComponentLine component : row.getComponents()) {
                addComponentRow(block, component, (inPanel ? INDENT : 0) + INDENT);
            }
            if (!GenericValidator.isBlankOrNull(row.getNote())) {
                block.addCell(
                        subLine(messages.get("field.note") + ": " + PdfReportText.plain(row.getNote()).trim(), false));
            }
            if (row.getCallback() != null) {
                block.addCell(subLine(callbackLine(row.getCallback()), true));
            }
            PdfPCell wrapper = new PdfPCell(block);
            wrapper.setColspan(COLUMNS);
            wrapper.setPadding(0);
            wrapper.setBorder(Rectangle.BOTTOM);
            wrapper.setBorderWidthBottom(0.25f);
            wrapper.setBorderColorBottom(GRAY);
            results.addCell(wrapper);
        }
        return results;
    }

    private void addResultRow(PdfPTable block, ClinicalPatientData row, float indent) {
        String flagCode = row.getComponents().isEmpty() ? row.getFlagCode() : null;
        boolean critical = PatientCILNSPClinical_vreduit.isCritical(flagCode);
        boolean abnormal = !GenericValidator.isBlankOrNull(flagCode);
        Color fill = critical ? CRITICAL_FILL : null;

        String name = PdfReportText.plain(row.getTestName()).trim();
        if (!row.isInAccreditedScope()) {
            name += " " + DAGGER;
        }
        PdfPCell test = text(name, fonts.regular(8.5f, Color.BLACK), Element.ALIGN_LEFT);
        test.setPaddingLeft(8 + indent);
        if (abnormal) {
            test.setCellEvent(new Bar(flagCode));
        }
        cells(block, fill, test);

        cells(block, fill, text(row.getSampleSortOrder(), fonts.mono(8), Element.ALIGN_CENTER));

        PdfPCell method;
        if (!GenericValidator.isBlankOrNull(row.getPerformingLabName())) {
            method = text(messages.format("report.patient.performedBy", row.getPerformingLabName()),
                    fonts.italic(7, GRAY), Element.ALIGN_LEFT);
        } else {
            method = text(row.getMethodName(), fonts.regular(7, GRAY), Element.ALIGN_LEFT);
        }
        cells(block, fill, method);

        PdfPCell result = resultCell(row, critical, abnormal);
        result.setPaddingTop(0);
        if (statusOnly(row) && !row.isReferralPending() && !row.isConfirmationSample()) {
            result.setColspan(2);
            result.setHorizontalAlignment(Element.ALIGN_LEFT);
            result.setPaddingLeft(12);
            cells(block, fill, result);
        } else {
            cells(block, fill, result);
            cells(block, fill, flagCell(flagCode, row.isReferralPending(), row.isConfirmationSample()));
        }
        boolean showRange = row.getComponents().isEmpty();
        cells(block, fill, fitted(showRange ? PdfReportText.plain(row.getTestRefRange()) : "", RANGE_COLUMN));
        cells(block, fill, fitted(showRange ? PdfReportText.plain(row.getUom()) : "", UNITS_COLUMN));
        cells(block, fill, text(row.isReleased() && !row.isSampleRejected() ? row.getCompletedTime() : "",
                fonts.regular(7, Color.BLACK), Element.ALIGN_LEFT));
    }

    /**
     * The row prints a status in place of a result: the sample or the result was
     * rejected, or the result is not released yet.
     */
    private static boolean statusOnly(ClinicalPatientData row) {
        return row.isSampleRejected() || row.isTestRejected()
                || (!row.isReleased() && !GenericValidator.isBlankOrNull(row.getAnalysisStatus())
                        && GenericValidator.isBlankOrNull(row.getResult()));
    }

    private PdfPCell resultCell(ClinicalPatientData row, boolean critical, boolean abnormal) {
        if (row.isSampleRejected()) {
            return text(messages.get("report.patient.result.sampleRejected"), fonts.italic(9, Color.BLACK),
                    Element.ALIGN_RIGHT);
        }
        if (row.isTestRejected()) {
            return text(messages.get("report.patient.result.rejected"), fonts.italic(9, Color.BLACK),
                    Element.ALIGN_RIGHT);
        }
        if (!row.isReleased() && !GenericValidator.isBlankOrNull(row.getAnalysisStatus())
                && GenericValidator.isBlankOrNull(row.getResult())) {
            return text(row.getAnalysisStatus(), fonts.italic(9, Color.BLACK), Element.ALIGN_RIGHT);
        }
        if (!row.getComponents().isEmpty()) {
            return text("", fonts.regular(9, Color.BLACK), Element.ALIGN_RIGHT);
        }
        Font font = critical ? fonts.bold(9.5f, Color.BLACK)
                : abnormal ? fonts.bold(9, Color.BLACK) : fonts.regular(9, Color.BLACK);
        return text(PdfReportText.plain(row.getResult()), font, Element.ALIGN_RIGHT);
    }

    private void addComponentRow(PdfPTable block, ReportComponentLine component, float indent) {
        String flagCode = GenericValidator.isBlankOrNull(component.flagCode()) ? null : component.flagCode();
        boolean critical = PatientCILNSPClinical_vreduit.isCritical(flagCode);
        Color fill = critical ? CRITICAL_FILL : null;
        PdfPCell label = text(component.label(), fonts.regular(8.5f, Color.BLACK), Element.ALIGN_LEFT);
        label.setPaddingLeft(8 + indent);
        if (flagCode != null) {
            label.setCellEvent(new Bar(flagCode));
        }
        cells(block, fill, label);
        cells(block, fill, blank());
        cells(block, fill, blank());
        Font font = critical ? fonts.bold(9.5f, Color.BLACK)
                : flagCode != null ? fonts.bold(9, Color.BLACK) : fonts.regular(9, Color.BLACK);
        cells(block, fill, text(PdfReportText.plain(component.value()), font, Element.ALIGN_RIGHT));
        cells(block, fill, flagCell(flagCode, false, false));
        cells(block, fill, fitted(component.range(), RANGE_COLUMN));
        cells(block, fill, fitted(component.unit(), UNITS_COLUMN));
        cells(block, fill, blank());
    }

    /** Arrow and translated letter, the reversed CRITICAL tag, then R and C. */
    private PdfPCell flagCell(String flagCode, boolean referred, boolean confirmation) {
        Phrase phrase = new Phrase();
        Font flagFont = fonts.bold(8.5f, Color.BLACK);
        String flag = flagText(flagCode);
        if (!flag.isEmpty()) {
            phrase.add(new Chunk(flag, flagFont));
        }
        List<String> markers = new ArrayList<>();
        if (referred) {
            markers.add(messages.get("report.patient.flag.referred"));
        }
        if (confirmation) {
            markers.add(messages.get("report.patient.flag.confirm"));
        }
        if (!markers.isEmpty()) {
            phrase.add(new Chunk((flag.isEmpty() ? "" : " ") + String.join(" ", markers), flagFont));
        }
        if (PatientCILNSPClinical_vreduit.isCritical(flagCode)) {
            phrase.add(Chunk.NEWLINE);
            Chunk tag = new Chunk(
                    " " + messages.get("label.results.flag.critical").toUpperCase(messages.getLocale()) + " ",
                    fonts.bold(6, Color.WHITE));
            tag.setBackground(Color.BLACK, 0, 1, 0, 0.5f);
            phrase.add(tag);
        }
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setLeading(0, 1.1f);
        cell.setPadding(1.5f);
        cell.setPaddingTop(0.5f);
        cell.setPaddingBottom(1.75f);
        return cell;
    }

    /**
     * The printed flag for an internal code: an arrow and the language's letter.
     */
    String flagText(String flagCode) {
        if (flagCode == null) {
            return "";
        }
        switch (flagCode) {
        case ResultAlertFlags.BELOW:
            return DOWN + " " + messages.get("report.patient.flag.low");
        case ResultAlertFlags.ABOVE:
            return UP + " " + messages.get("report.patient.flag.high");
        case ResultAlertFlags.CRITICALLY_BELOW:
            return DOUBLE_DOWN + " " + messages.get("report.patient.flag.criticalLow");
        case ResultAlertFlags.CRITICALLY_ABOVE:
            return DOUBLE_UP + " " + messages.get("report.patient.flag.criticalHigh");
        case ResultAlertFlags.ABNORMAL:
            return messages.get("report.patient.flag.abnormal");
        default:
            return "";
        }
    }

    /**
     * The largest size up to {@code size} at which every word of a column heading
     * fits its column, so a longer language wraps between words, never inside one.
     */
    private float wordFittingSize(String label, float width, float size) {
        return wordFittingSize(label, width, size, true);
    }

    private float wordFittingSize(String label, float width, float size, boolean bold) {
        float fitted = size;
        for (String word : label.split("\\s+")) {
            while (fitted > 4.5f && fonts.base(bold).getWidthPoint(word, fitted) > width) {
                fitted -= 0.25f;
            }
        }
        return fitted;
    }

    /**
     * A reference range or unit in its column, in a smaller size when one of its
     * words is wider than the column, so it never breaks inside a word.
     */
    private PdfPCell fitted(String value, int column) {
        String text = nonNull(value);
        return text(text, fonts.regular(wordFittingSize(text, RESULT_WIDTHS[column] - 3, 8, false), GRAY),
                Element.ALIGN_LEFT);
    }

    private String callbackLine(ReportCallbackLine callback) {
        String outcome;
        switch (nonNull(callback.status())) {
        case "CONFIRMED":
            outcome = messages.get("report.patient.callback.confirmed");
            break;
        case "REACHED_NO_READBACK":
            outcome = messages.get("report.patient.callback.noReadback");
            break;
        case "UNABLE_TO_REACH":
            outcome = messages.get("report.patient.callback.unreachable");
            break;
        default:
            outcome = nonNull(callback.status());
        }
        String line = messages.format("report.patient.callback.line", nonNull(callback.recipient()),
                nonNull(callback.loggedAt()), nonNull(callback.loggedBy()), outcome);
        if (callback.earlierAttempts() > 0) {
            line += " " + messages.format("report.patient.callback.more", callback.earlierAttempts());
        }
        return line;
    }

    private PdfPCell subLine(String text, boolean callback) {
        PdfPCell cell = text(text, fonts.italic(7, Color.BLACK), Element.ALIGN_LEFT);
        cell.setColspan(COLUMNS);
        cell.setPaddingLeft(callback ? 12 : 8);
        cell.setPaddingTop(0);
        if (callback) {
            cell.setCellEvent(new DottedRule());
        }
        return cell;
    }

    // ----- sign-off

    private PdfPTable signOff(PatientReportOrder order, List<ClinicalPatientData> rows) {
        String conclusion = null;
        for (ClinicalPatientData row : rows) {
            if (!GenericValidator.isBlankOrNull(row.getConclusion())) {
                conclusion = row.getConclusion();
            }
        }
        PdfPTable signOff = table(new float[] { 300, 8, contentWidth() - 308 });
        signOff.setSpacingBefore(4);
        signOff.setKeepTogether(true);

        PdfPCell comments = new PdfPCell();
        comments.setBorder(Rectangle.TOP);
        comments.setBorderWidthTop(1);
        comments.addElement(new Phrase(messages.get("report.patient.comments").toUpperCase(messages.getLocale()),
                fonts.bold(6.5f, GRAY)));
        if (conclusion != null) {
            comments.addElement(new Phrase(PdfReportText.plain(conclusion), fonts.regular(8, Color.BLACK)));
        }
        signOff.addCell(comments);
        PdfPCell gap = blank();
        gap.setBorder(Rectangle.TOP);
        gap.setBorderWidthTop(1);
        signOff.addCell(gap);

        PdfPCell released = new PdfPCell();
        released.setBorder(Rectangle.TOP);
        released.setBorderWidthTop(1);
        released.addElement(new Phrase(messages.get("report.patient.releasedBy").toUpperCase(messages.getLocale()),
                fonts.bold(6.5f, GRAY)));
        for (String validator : order.getValidators()) {
            released.addElement(new Paragraph(10, validator, fonts.regular(8, Color.BLACK)));
        }
        Image signature = image(settings.labDirectorSignature());
        if (signature != null) {
            signature.scaleToFit(120, 30);
            signature.setSpacingBefore(4);
            released.addElement(signature);
        }
        String director = directorLine();
        if (director != null) {
            Paragraph directorParagraph = new Paragraph(9, director, fonts.regular(7.5f, GRAY));
            directorParagraph.setSpacingBefore(2);
            released.addElement(directorParagraph);
        }
        signOff.addCell(released);

        PdfPCell end = text(messages.get("report.patient.endOfReport"), fonts.regular(7, GRAY), Element.ALIGN_CENTER);
        end.setColspan(3);
        end.setPaddingTop(3);
        signOff.addCell(end);
        return signOff;
    }

    // ----- page furniture

    /** Per-order page numbering, the continuation header and the footer. */
    private final class Pages extends PdfPageEventHelper {
        private final List<OrderPages> orders = new ArrayList<>();
        private OrderPages current;

        void startOrder(OrderPages order) {
            current = order;
            orders.add(order);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (current == null) {
                return;
            }
            current.lastPage = writer.getPageNumber();
            int page = writer.getPageNumber() - current.firstPage + 1;
            PdfContentByte canvas = writer.getDirectContent();
            if (page > 1) {
                continuationHeader(canvas, document, page);
            }
            footer(canvas, document, page);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            for (OrderPages order : orders) {
                order.total.beginText();
                order.total.setFontAndSize(fonts.base(false), 7);
                order.total.setTextMatrix(0, 1);
                order.total.showText(String.valueOf(order.lastPage - order.firstPage + 1));
                order.total.endText();
                order.stripTotal.beginText();
                order.stripTotal.setFontAndSize(fonts.base(false), 7.5f);
                order.stripTotal.setTextMatrix(0, 1);
                order.stripTotal.showText(String.valueOf(order.lastPage - order.firstPage + 1));
                order.stripTotal.endText();
            }
        }

        private void continuationHeader(PdfContentByte canvas, Document document, int page) {
            float top = document.getPageSize().getHeight() - TOP_MARGIN;
            float left = document.left();
            float right = document.right();
            Image logo = image(settings.leftLogo());
            if (logo != null) {
                logo.scaleToFit(24, 24);
                logo.setAbsolutePosition(left + (24 - logo.getScaledWidth()) / 2,
                        top - 24 + (24 - logo.getScaledHeight()) / 2);
                canvas.addImage(logo);
            }
            Phrase title = new Phrase();
            title.add(new Chunk(nonNull(settings.siteName()) + "  ", fonts.bold(9, Color.BLACK)));
            title.add(new Chunk(current.title, fonts.regular(8, GRAY)));
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, title, left + 30, top - 10, 0);
            pageOf(canvas, right, top - 10, page, current.stripTotal, 7.5f);
            float stripSize = 7.5f;
            while (stripSize > 5 && fonts.base(false).getWidthPoint(current.strip, stripSize) > right - left - 30) {
                stripSize -= 0.25f;
            }
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase(current.strip, fonts.regular(stripSize, Color.BLACK)), left + 30, top - 22, 0);
            canvas.saveState();
            canvas.setLineWidth(0.75f);
            canvas.moveTo(left, top - 29);
            canvas.lineTo(right, top - 29);
            canvas.stroke();
            canvas.restoreState();
        }

        private void footer(PdfContentByte canvas, Document document, int page) {
            float left = document.left();
            float right = document.right();
            float base = TOP_MARGIN;
            String legend = legend(current.dagger);
            float legendSize = 7;
            while (legendSize > 5 && fonts.base(false).getWidthPoint(legend, legendSize) > right - left) {
                legendSize -= 0.25f;
            }
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(legend, fonts.regular(legendSize, GRAY)),
                    left, base + 15, 0);
            canvas.saveState();
            canvas.setLineWidth(0.5f);
            canvas.setColorStroke(GRAY);
            canvas.moveTo(left, base + 11);
            canvas.lineTo(right, base + 11);
            canvas.stroke();
            canvas.restoreState();
            String issued = messages.get("report.patient.issued") + " " + current.issued;
            if (current.order.getVersion() != null) {
                issued = messages.format("report.patient.footer.version", current.order.getVersion()) + DOT + issued;
            }
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(issued, fonts.regular(7, GRAY)), left,
                    base + 2, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(join(DOT, current.first.getAccessionNumber(), current.first.getSubjectNumber()),
                            fonts.regular(7, GRAY)),
                    (left + right) / 2, base + 2, 0);
            pageOf(canvas, right, base + 2, page, current.total, 7);
        }

        /** "Page x of " right-aligned, with the order's total filled in at the end. */
        private void pageOf(PdfContentByte canvas, float right, float y, int page, PdfTemplate total, float size) {
            String text = messages.get("report.patient.page") + " " + page + " " + messages.get("of") + " ";
            float textWidth = fonts.base(false).getWidthPoint(text, size);
            float totalWidth = 14;
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(text, fonts.regular(size, GRAY)),
                    right - totalWidth - textWidth, y, 0);
            canvas.saveState();
            canvas.setColorFill(GRAY);
            canvas.addTemplate(total, right - totalWidth, y - 1);
            canvas.restoreState();
        }
    }

    /** One order's place in the document and what its page furniture prints. */
    private final class OrderPages {
        private final PatientReportOrder order;
        private final ClinicalPatientData first;
        private final String title;
        private final boolean dagger;
        private final int firstPage;
        private final PdfTemplate total;
        private final PdfTemplate stripTotal;
        private final String strip;
        private final String issued;
        private int lastPage;

        OrderPages(PdfWriter writer, PatientReportOrder order, ClinicalPatientData first, String title, boolean dagger,
                int firstPage) {
            this.order = order;
            this.first = first;
            this.title = title;
            this.dagger = dagger;
            this.firstPage = firstPage;
            this.lastPage = firstPage;
            this.total = writer.getDirectContent().createTemplate(14, 10);
            this.stripTotal = writer.getDirectContent().createTemplate(14, 10);
            this.strip = strip(order, first);
            this.issued = order.getIssueTime() != null ? order.getIssueTime()
                    : PatientCILNSPClinical_vreduit.reportTime(new Timestamp(System.currentTimeMillis()));
        }
    }

    /** The patient strip on pages 2 and later. */
    private String strip(PatientReportOrder order, ClinicalPatientData row) {
        String[] site = nonNull(row.getSiteInfo()).split("\\|", 2);
        String collected = null;
        if (order.getEarliestCollection() != null) {
            collected = order.isCollectionsDiffer()
                    ? messages.format("report.patient.strip.collectedFirst", order.getEarliestCollection(),
                            order.getSamples().size())
                    : messages.format("report.patient.strip.collected", order.getEarliestCollection());
        }
        return join(DOT, PdfReportText.plain(row.getPatientName()), row.getSubjectNumber(), row.getNationalId(),
                row.getAccessionNumber(), row.getContactInfo(), join(" / ", site[0], site.length > 1 ? site[1] : ""),
                collected);
    }

    /** The flag legend, with the not-accredited line when a row carries it. */
    String legend(boolean dagger) {
        String legend = messages.get("report.patient.legend") + ": " + UP + " "
                + messages.get("report.patient.flag.high") + " " + messages.get("report.patient.legend.high") + "   "
                + DOWN + " " + messages.get("report.patient.flag.low") + " " + messages.get("report.patient.legend.low")
                + "   " + DOUBLE_UP + " " + messages.get("report.patient.flag.criticalHigh") + " / " + DOUBLE_DOWN + " "
                + messages.get("report.patient.flag.criticalLow") + " " + messages.get("report.patient.legend.critical")
                + "   " + messages.get("report.patient.flag.abnormal") + " "
                + messages.get("label.results.flag.abnormal").toLowerCase(messages.getLocale()) + "   "
                + messages.get("report.patient.flag.referred") + " " + messages.get("report.patient.legend.referred")
                + "   " + messages.get("report.patient.flag.confirm") + " "
                + messages.get("report.patient.legend.confirm");
        if (dagger) {
            legend += "   " + DAGGER + " " + messages.get("report.patient.notAccredited");
        }
        return legend;
    }

    // ----- cells

    private float contentWidth() {
        Rectangle page = PdfExportSupport.pageSize();
        return page.getWidth() - 2 * SIDE_MARGIN;
    }

    private PdfPTable table(float[] widths) {
        float sum = 0;
        for (float width : widths) {
            sum += width;
        }
        float[] scaled = new float[widths.length];
        for (int i = 0; i < widths.length; i++) {
            scaled[i] = widths[i] / sum * contentWidth();
        }
        PdfPTable table = new PdfPTable(scaled.length);
        try {
            table.setTotalWidth(scaled);
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
        table.setLockedWidth(true);
        return table;
    }

    private static void cells(PdfPTable table, Color fill, PdfPCell cell) {
        if (fill != null) {
            cell.setBackgroundColor(fill);
        }
        table.addCell(cell);
    }

    private PdfPCell text(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(nonNull(text), font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setLeading(0, 1.1f);
        cell.setPadding(1.5f);
        cell.setPaddingTop(0.5f);
        cell.setPaddingBottom(1.75f);
        return cell;
    }

    private static PdfPCell blank() {
        PdfPCell cell = new PdfPCell(new Phrase(""));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    private static PdfPCell nested(PdfPTable table) {
        PdfPCell cell = new PdfPCell(table);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        return cell;
    }

    /**
     * A square slot of the given size with the image scaled to fit, keeping its
     * shape.
     */
    private static PdfPCell imageCell(byte[] bytes, float size, float height, int alignment) {
        Image image = image(bytes);
        PdfPCell cell;
        if (image == null) {
            cell = new PdfPCell();
        } else {
            image.scaleToFit(size, size);
            cell = new PdfPCell(image, false);
        }
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        cell.setFixedHeight(height);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private static Image image(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return Image.getInstance(bytes);
        } catch (IOException | RuntimeException e) {
            LogEvent.logError(PatientResultsPdf.class.getSimpleName(), "image",
                    "Unreadable report image: " + e.getMessage());
            return null;
        }
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }

    /** The non-blank parts joined by the separator. */
    private static String join(String separator, String... parts) {
        List<String> present = new ArrayList<>();
        for (String part : parts) {
            if (!GenericValidator.isBlankOrNull(part)) {
                present.add(part.trim());
            }
        }
        return String.join(separator, present);
    }

    /**
     * The left bar of an abnormal row: solid 2 pt for high and abnormal, dashed for
     * low, 5 pt for critical.
     */
    private static final class Bar implements PdfPCellEvent {
        private final String flagCode;

        Bar(String flagCode) {
            this.flagCode = flagCode;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            canvas.saveState();
            canvas.setColorFill(Color.BLACK);
            canvas.setColorStroke(Color.BLACK);
            if (PatientCILNSPClinical_vreduit.isCritical(flagCode)) {
                canvas.rectangle(position.getLeft(), position.getBottom(), 5, position.getHeight());
                canvas.fill();
            } else if (ResultAlertFlags.BELOW.equals(flagCode)) {
                canvas.setLineWidth(2);
                canvas.setLineCap(PdfContentByte.LINE_CAP_BUTT);
                canvas.setLineDash(2, 2, 0);
                canvas.moveTo(position.getLeft() + 1, position.getTop());
                canvas.lineTo(position.getLeft() + 1, position.getBottom());
                canvas.stroke();
            } else {
                canvas.rectangle(position.getLeft(), position.getBottom(), 2, position.getHeight());
                canvas.fill();
            }
            canvas.restoreState();
        }
    }

    /** The dotted rule to the left of a callback line. */
    private static final class DottedRule implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.LINECANVAS];
            canvas.saveState();
            canvas.setLineWidth(1);
            canvas.setLineCap(PdfContentByte.LINE_CAP_BUTT);
            canvas.setLineDash(1, 1.5f, 0);
            canvas.moveTo(position.getLeft() + 8, position.getTop());
            canvas.lineTo(position.getLeft() + 8, position.getBottom());
            canvas.stroke();
            canvas.restoreState();
        }
    }

    /**
     * DejaVu Sans, embedded: it carries the arrows the flags print (which the PDF's
     * built-in Helvetica does not), and every script the catalogue's languages use.
     * Falls back to Helvetica only when the font cannot be loaded.
     */
    static final class Fonts {
        private static final String DIRECTORY = "net/sf/jasperreports/fonts/dejavu/";
        private static final BaseFont REGULAR = load("DejaVuSans.ttf");
        private static final BaseFont BOLD = load("DejaVuSans-Bold.ttf");
        private static final BaseFont ITALIC = load("DejaVuSans-Oblique.ttf");
        private static final BaseFont MONO = load("DejaVuSansMono.ttf");

        Font regular(float size, Color color) {
            return font(REGULAR, size, Font.NORMAL, color);
        }

        Font bold(float size, Color color) {
            return font(BOLD, size, Font.BOLD, color);
        }

        Font italic(float size, Color color) {
            return font(ITALIC, size, Font.ITALIC, color);
        }

        Font mono(float size) {
            return MONO == null ? new Font(Font.COURIER, size) : new Font(MONO, size, Font.NORMAL, Color.BLACK);
        }

        BaseFont base(boolean bold) {
            BaseFont font = bold ? BOLD : REGULAR;
            if (font != null) {
                return font;
            }
            try {
                return BaseFont.createFont(bold ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA, BaseFont.WINANSI,
                        false);
            } catch (IOException | DocumentException e) {
                throw new IllegalStateException(e);
            }
        }

        static boolean unicode() {
            return REGULAR != null && BOLD != null && ITALIC != null && MONO != null;
        }

        private static Font font(BaseFont base, float size, int helveticaStyle, Color color) {
            if (base == null) {
                return new Font(Font.HELVETICA, size, helveticaStyle, color);
            }
            return new Font(base, size, Font.NORMAL, color);
        }

        private static BaseFont load(String file) {
            String resource = DIRECTORY + file;
            try (InputStream in = PatientResultsPdf.class.getClassLoader().getResourceAsStream(resource)) {
                if (in == null) {
                    LogEvent.logError(PatientResultsPdf.class.getSimpleName(), "load",
                            "Font " + resource + " is not on the classpath; the patient report falls back to"
                                    + " Helvetica and cannot print the flag arrows");
                    return null;
                }
                return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(),
                        null);
            } catch (IOException | DocumentException e) {
                LogEvent.logError(PatientResultsPdf.class.getSimpleName(), "load",
                        "Font " + resource + " could not be read: " + e);
                return null;
            }
        }
    }
}
