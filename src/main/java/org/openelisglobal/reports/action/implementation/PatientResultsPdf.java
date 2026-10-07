package org.openelisglobal.reports.action.implementation;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.common.util.PdfReportText;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.reports.action.implementation.reportBeans.ClinicalPatientData;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

/**
 * The patient results report: for each patient, each order's patient and order
 * details and its results by section, then the validation block. Every page
 * carries the result flag legend, the report date and, when the site asks for
 * them, page numbers.
 */
final class PatientResultsPdf {

    record Settings(List<String> headerLines, List<byte[]> accreditationLogos, String accreditationNotesLine,
            boolean useBillingNumber, String billingNumberLabel, boolean useContactTracing, byte[] labDirectorSignature,
            String labDirectorName, String labDirectorTitle, boolean usePageNumbers) {
    }

    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 8);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font NOTICE_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 7);
    private static final float[] RESULT_WIDTHS = { 153, 30, 110, 50, 30, 110, 70 };
    private static final float VALIDATION_HEIGHT = 80;
    private static final Color LABEL_BACKGROUND = new Color(230, 230, 230);

    private PatientResultsPdf() {
    }

    static byte[] render(Settings settings, List<ClinicalPatientData> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 30, 30, 30, 50);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        Footer footer = new Footer(settings.usePageNumbers());
        writer.setPageEvent(footer);
        document.open();
        int start = 0;
        while (start < items.size()) {
            int end = start;
            while (end < items.size()
                    && Objects.equals(items.get(end).getSubjectNumber(), items.get(start).getSubjectNumber())) {
                end++;
            }
            if (start > 0) {
                document.newPage();
            }
            footer.startPatient(writer.getPageNumber());
            addPatient(document, writer, settings, items.subList(start, end));
            start = end;
        }
        document.close();
        return out.toByteArray();
    }

    /**
     * One patient from the top of a page: header, orders and results, validation at
     * the foot.
     */
    private static void addPatient(Document document, PdfWriter writer, Settings settings,
            List<ClinicalPatientData> items) {
        ReportHeaderPdf.add(document, MessageUtil.getMessage("report.analysisReport"), settings.headerLines(),
                settings.accreditationLogos(), settings.accreditationNotesLine());
        PdfPTable results = null;
        for (int i = 0; i < items.size(); i++) {
            ClinicalPatientData item = items.get(i);
            ClinicalPatientData previous = i == 0 ? null : items.get(i - 1);
            boolean newOrder = previous == null
                    || !Objects.equals(item.getAccessionNumber(), previous.getAccessionNumber());
            boolean newSection = newOrder || !Objects.equals(item.getTestSection(), previous.getTestSection());
            if (newOrder) {
                if (results != null) {
                    document.add(results);
                    results = null;
                }
                addOrderNotices(document, item);
                addOrderDetails(document, settings, item);
            }
            if (newSection) {
                if (results != null) {
                    document.add(results);
                }
                results = resultsTable();
                PdfPCell identity = new PdfPCell(new Phrase(
                        MessageUtil.getMessage("report.patientCode") + " "
                                + Objects.toString(item.getSubjectNumber(), "") + "    "
                                + MessageUtil.getMessage("report.ordinanceNo") + " " + item.getAccessionNumber(),
                        SECTION_FONT));
                identity.setColspan(RESULT_WIDTHS.length);
                results.addCell(identity);
                addSectionHeading(results, item.getTestSection());
                results.setHeaderRows(3);
            }
            results.addCell(cell(item.getTestName(), TEXT_FONT, Element.ALIGN_LEFT));
            results.addCell(cell(item.getSampleSortOrder(), TEXT_FONT, Element.ALIGN_CENTER));
            results.addCell(cell(item.getResult(), TEXT_FONT, Element.ALIGN_RIGHT));
            results.addCell(cell(item.getAnalysisStatus(), TEXT_FONT, Element.ALIGN_CENTER));
            results.addCell(cell(item.getAlerts(), TEXT_FONT, Element.ALIGN_CENTER));
            results.addCell(cell(item.getTestRefRange(), TEXT_FONT, Element.ALIGN_CENTER));
            results.addCell(cell(item.getUom(), TEXT_FONT, Element.ALIGN_CENTER));
            if (item.getNote() != null) {
                PdfPCell note = cell(item.getNote(), TEXT_FONT, Element.ALIGN_LEFT);
                note.setColspan(RESULT_WIDTHS.length);
                note.setPaddingLeft(12);
                results.addCell(note);
            }
        }
        if (results != null) {
            document.add(results);
        }
        addValidation(document, writer, settings);
    }

    private static void addOrderNotices(Document document, ClinicalPatientData item) {
        if (item.isCorrectedResult()) {
            Paragraph corrected = new Paragraph(MessageUtil.getMessage("report.correctedReport"), NOTICE_FONT);
            corrected.setAlignment(Element.ALIGN_CENTER);
            document.add(corrected);
        }
        Paragraph status = new Paragraph(
                MessageUtil.getMessage("report.results") + " " + Objects.toString(item.getCompleteFlag(), ""),
                SECTION_FONT);
        status.setAlignment(Element.ALIGN_CENTER);
        status.setSpacingBefore(8);
        document.add(status);

    }

    private static void addOrderDetails(Document document, Settings settings, ClinicalPatientData item) {
        String nationalIdLabel = MessageUtil.getMessage("report.idNational");
        String nationalId = Objects.toString(item.getNationalId(), "");
        if (settings.useBillingNumber()) {
            nationalIdLabel += "\n" + Objects.toString(settings.billingNumberLabel(), "");
            nationalId += "\n" + Objects.toString(item.getBillingNumber(), "");
        }
        String[] site = Objects.toString(item.getSiteInfo(), "").split("\\|", 2);

        PdfPTable details = new PdfPTable(new float[] { 100, 100, 87, 117, 22, 26, 25, 50, 20, 26 });
        details.setWidthPercentage(100);
        details.setSpacingBefore(6);
        details.addCell(label(MessageUtil.getMessage("report.patientCode"), 1));
        details.addCell(value(item.getSubjectNumber(), 1));
        details.addCell(label(nationalIdLabel, 1));
        details.addCell(value(nationalId, 1));
        details.addCell(label(MessageUtil.getMessage("report.age"), 1));
        details.addCell(value(item.getAge(), 1));
        details.addCell(label(MessageUtil.getMessage("report.dob"), 1));
        details.addCell(value(GenericValidator.isBlankOrNull(item.getDob()) ? "\u2014" : item.getDob(), 1));
        details.addCell(label(MessageUtil.getMessage("report.sex"), 1));
        details.addCell(value("0".equals(item.getGender()) ? "" : item.getGender(), 1));
        addRow(details, "label.patient_name", item.getPatientName(), "report.referringSite", site[0]);
        addRow(details, "report.prescriber", item.getContactInfo(), "report.referringSiteDepartment",
                site.length > 1 ? site[1] : "");
        addRow(details, "report.ordinanceNo", item.getAccessionNumber(), "report.program", item.getLabOrderType());
        addRow(details, "report.orderDate", item.getOrderDate(), "report.receiptDate", item.getRecievedDate());
        PdfPCell collection = value(MessageUtil.getMessage("report.specimenCollectTimes") + ": "
                + Objects.toString(item.getCollectionDateTime(), ""), 10);
        collection.setPaddingTop(6);
        collection.setPaddingBottom(10);
        details.addCell(collection);
        if (settings.useContactTracing()) {
            details.addCell(value(MessageUtil.getMessage("label.note") + ": "
                    + MessageUtil.getMessage("field.contacttracing.indexname.label") + ": "
                    + Objects.toString(item.getContactTracingIndexName(), "") + " "
                    + MessageUtil.getMessage("field.contacttracing.indexrecordnumber.label") + ": "
                    + Objects.toString(item.getContactTracingIndexRecordNumber(), ""), 10));
        }
        document.add(details);
    }

    private static PdfPTable resultsTable() {
        PdfPTable table = new PdfPTable(RESULT_WIDTHS);
        table.setWidthPercentage(100);
        table.setSpacingBefore(14);
        return table;
    }

    private static void addSectionHeading(PdfPTable table, String section) {
        PdfPCell title = new PdfPCell(new Phrase(Objects.toString(section, ""), SECTION_FONT));
        title.setColspan(RESULT_WIDTHS.length);
        title.setHorizontalAlignment(Element.ALIGN_CENTER);
        title.setBorder(Rectangle.TOP | Rectangle.LEFT | Rectangle.RIGHT);
        table.addCell(title);
        for (String key : new String[] { "report.test", "report.specimen", "report.outcome", "report.status",
                "report.alert", "report.referenceValue", "report.unit" }) {
            PdfPCell header = new PdfPCell(new Phrase(MessageUtil.getMessage(key), HEADER_FONT));
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setBorder(Rectangle.BOTTOM);
            table.addCell(header);
        }
    }

    /** The comments and signature boxes, at the foot of the patient's last page. */
    private static void addValidation(Document document, PdfWriter writer, Settings settings) {
        PdfPTable validation = new PdfPTable(new float[] { 370, 182 });
        validation.setWidthPercentage(100);
        validation.setTotalWidth(document.right() - document.left());
        validation.setLockedWidth(true);
        PdfPCell comments = new PdfPCell(new Phrase(MessageUtil.getMessage("report.labInfomation"), TEXT_FONT));
        comments.setFixedHeight(VALIDATION_HEIGHT);
        validation.addCell(comments);
        // a row-spanning cell drawn by writeSelectedRows keeps only its first row's
        // height
        PdfPTable signatureAndDate = new PdfPTable(1);
        PdfPCell signature = new PdfPCell();
        signature.setFixedHeight(VALIDATION_HEIGHT - 20);
        signature.setBorder(Rectangle.TOP | Rectangle.LEFT | Rectangle.RIGHT);
        signature.addElement(new Phrase(MessageUtil.getMessage("report.signValidation"), TEXT_FONT));
        if (settings.labDirectorSignature() != null) {
            try {
                Image image = Image.getInstance(settings.labDirectorSignature());
                image.scaleToFit(170, 26);
                signature.addElement(image);
            } catch (Exception e) {
                LogEvent.logError(PatientResultsPdf.class.getSimpleName(), "addValidation",
                        "Unreadable lab director signature: " + e.getMessage());
            }
            if (settings.labDirectorTitle() != null) {
                signature.addElement(new Phrase(Objects.toString(settings.labDirectorName(), ""), TEXT_FONT));
                signature.addElement(new Phrase("\n" + settings.labDirectorTitle(), TEXT_FONT));
            }
        }
        signatureAndDate.addCell(signature);
        PdfPCell date = new PdfPCell(new Phrase(MessageUtil.getMessage("report.date"), TEXT_FONT));
        date.setFixedHeight(20);
        date.setBorder(Rectangle.BOTTOM | Rectangle.LEFT | Rectangle.RIGHT);
        signatureAndDate.addCell(date);
        PdfPCell signatureColumn = new PdfPCell(signatureAndDate);
        signatureColumn.setBorder(Rectangle.NO_BORDER);
        signatureColumn.setPadding(0);
        validation.addCell(signatureColumn);

        if (writer.getVerticalPosition(true) - document.bottom() < VALIDATION_HEIGHT + 6) {
            document.newPage();
            // a page needs content before writeSelectedRows can draw on it
            document.add(new Paragraph(" "));
        }
        validation.writeSelectedRows(0, -1, document.left(), document.bottom() + VALIDATION_HEIGHT,
                writer.getDirectContent());
    }

    private static void addRow(PdfPTable table, String leftKey, String leftValue, String rightKey, String rightValue) {
        table.addCell(label(MessageUtil.getMessage(leftKey), 1));
        table.addCell(value(leftValue, 1));
        table.addCell(label(MessageUtil.getMessage(rightKey), 1));
        table.addCell(value(rightValue, 7));
    }

    private static PdfPCell label(String text, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(Objects.toString(text, ""), TEXT_FONT));
        cell.setBackgroundColor(LABEL_BACKGROUND);
        cell.setColspan(colspan);
        return cell;
    }

    private static PdfPCell value(String text, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(Objects.toString(text, ""), TEXT_FONT));
        cell.setColspan(colspan);
        return cell;
    }

    private static PdfPCell cell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(PdfReportText.plain(text), font));
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    /** The legend, report date and page number at the foot of every page. */
    private static final class Footer extends PdfPageEventHelper {
        private final boolean usePageNumbers;
        private int patientFirstPage = 1;

        Footer(boolean usePageNumbers) {
            this.usePageNumbers = usePageNumbers;
        }

        /** Each patient's pages are numbered from 1, as the template restarted them. */
        void startPatient(int page) {
            patientFirstPage = page;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            String legend = MessageUtil.getMessage("report.legend") + "  B = "
                    + MessageUtil.getMessage("report.belowNormal") + "  E = "
                    + MessageUtil.getMessage("report.aboveNormal") + "  * = "
                    + MessageUtil.getMessage("report.abnormal") + "  BB = "
                    + MessageUtil.getMessage("report.criticalBelowNormal") + "  EE = "
                    + MessageUtil.getMessage("report.criticalAboveNormal") + "  R = "
                    + MessageUtil.getMessage("report.extLabReference") + "  C = "
                    + MessageUtil.getMessage("report.confirmTest");
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, new Phrase(legend, FOOTER_FONT),
                    document.left(), document.bottom() - 14, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase(MessageUtil.getMessage("report.reportDate") + " " + DateUtil.getCurrentDateAsText() + " "
                            + DateUtil.getCurrentTimeAsText(), FOOTER_FONT),
                    document.left(), document.bottom() - 26, 0);
            if (usePageNumbers) {
                ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                        new Phrase(MessageUtil.getMessage("report.label.page") + " "
                                + (writer.getPageNumber() - patientFirstPage + 1), FOOTER_FONT),
                        document.right(), document.bottom() - 26, 0);
            }
        }
    }
}
