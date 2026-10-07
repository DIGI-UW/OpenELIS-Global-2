package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.reports.action.implementation.reportBeans.IndeterminateReportData;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

/**
 * The study (RetroCI) indeterminate results: version one lists each test of the
 * HIV algorithm the order has a result for, version two gives the serology
 * status, and the location report lists each service's orders with their
 * status. The texts are the study's, in French in every language, with the HIV
 * glossary and the report date at the foot of every page.
 */
final class StudyIndeterminateResultsPdf {

    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 12);
    private static final Font UNDERLINED_FONT = new Font(Font.HELVETICA, 9, Font.UNDERLINE);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8);

    private static final String GLOSSARY = "LEXIQUE\nVIH: 1 = VIH1   2 = VIH2   D = VIH Dual   Positif = VIH Positif   "
            + "N = VIH négatif   U = VIH indéterminé";

    private static final String SEROLOGY_METHOD = "(Algorithme en parallèle : Enzygnost Integral II et Murex 1.2.0)";

    private StudyIndeterminateResultsPdf() {
    }

    static byte[] versionOne(List<IndeterminateReportData> orders, String studyName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = open(out);
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                document.newPage();
            }
            IndeterminateReportData order = orders.get(index);
            addHeading(document, order, studyName);
            Paragraph title = new Paragraph("Résultats des tests de l’algorithme utilisé", HEADING_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);
            PdfPTable tests = new PdfPTable(new float[] { 168, 141, 243 });
            tests.setWidthPercentage(100);
            tests.addCell(plain("Test", UNDERLINED_FONT));
            tests.addCell(plain("Résultat", UNDERLINED_FONT));
            tests.addCell(plain("", TEXT_FONT));
            for (String[] test : new String[][] { { "Enzygnost HIV Integral II", order.getIntegral() },
                    { "Murex HIV-1.2.O", order.getMurex() },
                    { "Vironstika HIV Uni-Form II plus O", order.getVironstika() }, { "Bioline", order.getBioline() },
                    { "Genie II HIV-1/HIV-2", order.getGenie_hiv1_hiv2() }, { "New Lav Blot I", order.getWb1() },
                    { "New Lav Blot II", order.getWb2() }, { "p24 antigen", order.getP24() },
                    { "DNA PCR", order.getPcr() }, { "Genie II sérum dilué 1/100", order.getGenie100() },
                    { "Genie II sérum dilué 1/10", order.getGenie10() } }) {
                if (test[1] != null) {
                    tests.addCell(plain(test[0], TEXT_FONT));
                    tests.addCell(plain(test[1], TEXT_FONT));
                    tests.addCell(plain("", TEXT_FONT));
                }
            }
            PdfPCell conclusion = plain("Conclusion", UNDERLINED_FONT);
            conclusion.setPaddingTop(14);
            tests.addCell(conclusion);
            PdfPCell result = plain(Objects.toString(order.getFinalResult(), ""), TEXT_FONT);
            result.setPaddingTop(14);
            tests.addCell(result);
            PdfPCell biologist = plain("Le biologiste", UNDERLINED_FONT);
            biologist.setPaddingTop(14);
            biologist.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tests.addCell(biologist);
            document.add(tests);
        }
        document.close();
        return out.toByteArray();
    }

    static byte[] versionTwo(List<IndeterminateReportData> orders, String studyName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = open(out);
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                document.newPage();
            }
            IndeterminateReportData order = orders.get(index);
            addHeading(document, order, studyName);
            addSerologyHeading(document);
            Paragraph status = new Paragraph("Statut sérologique " + Objects.toString(order.getFinalResult(), ""),
                    TEXT_FONT);
            status.setIndentationLeft(54);
            status.setSpacingBefore(14);
            document.add(status);
            Paragraph biologist = new Paragraph("Le Biologiste", TEXT_FONT);
            biologist.setAlignment(Element.ALIGN_CENTER);
            biologist.setSpacingBefore(40);
            document.add(biologist);
        }
        document.close();
        return out.toByteArray();
    }

    /**
     * Each service's orders, a group for each service, doctor and reception date in
     * the order the items arrive.
     */
    static byte[] byLocation(List<IndeterminateReportData> orders, String studyName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 30, 30, 30, 70);
        ReportHeaderPdf.openRepeating(document, out, studyName, ReportHeaderPdf.siteNameLines(), "SEROLOGIE",
                "Sérologie VIH " + SEROLOGY_METHOD).setPageEvent(new GlossaryFooter());
        int start = 0;
        while (start < orders.size()) {
            int end = start;
            while (end < orders.size() && group(orders.get(end)).equals(group(orders.get(start)))) {
                end++;
            }
            IndeterminateReportData first = orders.get(start);
            PdfPTable table = new PdfPTable(new float[] { 112, 84, 106, 140, 110 });
            table.setWidthPercentage(100);
            table.setSpacingBefore(12);
            table.setHeaderRows(4);
            addGroupLine(table, "Service", first.getOrgname(), true);
            addGroupLine(table, "Médecin", first.getDoctor(), false);
            addGroupLine(table, "Date de réception", first.getReceivedDate(), false);
            for (String heading : new String[] { "Lab No", "Sujet No", "Date de prélèvement", "Result Sérologie VIH",
                    "" }) {
                table.addCell(plain(heading, UNDERLINED_FONT));
            }
            for (IndeterminateReportData order : orders.subList(start, end)) {
                for (String value : new String[] { order.getLabNo(), order.getSubjectNumber(),
                        order.getCollectiondate(), order.getFinalResult(), "" }) {
                    table.addCell(plain(Objects.toString(value, ""), TEXT_FONT));
                }
            }
            document.add(table);
            start = end;
        }
        Paragraph biologist = new Paragraph("Le Biologiste", TEXT_FONT);
        biologist.setIndentationLeft(320);
        biologist.setSpacingBefore(16);
        document.add(biologist);
        document.close();
        return out.toByteArray();
    }

    private static String group(IndeterminateReportData order) {
        return Objects.toString(order.getOrgname(), "") + "|" + Objects.toString(order.getDoctor(), "") + "|"
                + Objects.toString(order.getReceivedDate(), "");
    }

    private static void addGroupLine(PdfPTable table, String label, String value, boolean ruledAbove) {
        PdfPCell labelCell = plain(label, TEXT_FONT);
        PdfPCell valueCell = plain(Objects.toString(value, ""), TEXT_FONT);
        valueCell.setColspan(4);
        for (PdfPCell cell : new PdfPCell[] { labelCell, valueCell }) {
            if (ruledAbove) {
                cell.setBorder(Rectangle.TOP);
                cell.setPaddingTop(6);
            }
            table.addCell(cell);
        }
    }

    private static Document open(ByteArrayOutputStream out) {
        Document document = new Document(PdfExportSupport.pageSize(), 30, 30, 30, 70);
        PdfWriter writer = ReportHeaderPdf.open(document, out);
        writer.setPageEvent(new GlossaryFooter());
        return document;
    }

    /** The site header and the patient block, repeated on every page. */
    private static void addHeading(Document document, IndeterminateReportData order, String studyName) {
        ReportHeaderPdf.add(document, studyName, ReportHeaderPdf.siteNameLines());
        StudyPatientBlockPdf.add(document,
                new StudyPatientBlockPdf.Patient(order.getSubjectNumber(), order.getLabNo(), order.getGender(), null,
                        order.getBirth_date(), order.getAge(), null, order.getCollectiondate(), order.getReceivedDate(),
                        order.getDoctor(), order.getOrgname(), null, null));
    }

    private static void addSerologyHeading(Document document) {
        document.add(new Paragraph("SEROLOGIE", HEADING_FONT));
        Paragraph method = new Paragraph("Sérologie VIH " + SEROLOGY_METHOD, TEXT_FONT);
        method.setSpacingBefore(8);
        document.add(method);
    }

    private static PdfPCell plain(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4);
        return cell;
    }

    /** The HIV glossary and the report date at the foot of every page. */
    private static final class GlossaryFooter extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText column = new ColumnText(writer.getDirectContent());
            column.setSimpleColumn(document.left(), 20, document.right(), document.bottom() - 16);
            column.addElement(new Paragraph(GLOSSARY, SMALL_FONT));
            column.go();
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(DateUtil.getCurrentDateAsText(), SMALL_FONT), document.right(), 22, 0);
        }
    }
}
