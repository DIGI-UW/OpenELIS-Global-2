package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.reports.action.implementation.reportBeans.ARVReportData;
import org.openpdf.text.Chunk;
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
 * The study (RetroCI) antiretroviral results reports. Version one draws each
 * section's results in a grid with the laboratory's reference values at the
 * foot of every page, and the client non-conformity checklist on its own page
 * when the order has any; version two lists each result beside its reference
 * values for men and women. The analyzers, methods and reference values are the
 * study's, printed as its reports always have, in French in every language.
 */
final class StudyArvResultsPdf {

    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 12);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 10, Font.UNDERLINE);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 7);
    private static final Font UNDERLINED_FONT = new Font(Font.HELVETICA, 9, Font.UNDERLINE);
    private static final float FOOTER_BOTTOM = 20;

    /** The laboratory's section reference images, when the site has them. */
    record Images(byte[] hematology, byte[] immunology, byte[] biochemistry, byte[] serology) {
        static final Images NONE = new Images(null, null, null, null);
    }

    record Settings(String studyName, Images images) {
    }

    private StudyArvResultsPdf() {
    }

    static byte[] versionOne(List<ARVReportData> orders, Settings settings) {
        Rectangle pageSize = PdfExportSupport.pageSize();
        PdfPTable footer = referenceFooter(pageSize.getWidth() - 60);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(pageSize, 30, 30, 30, FOOTER_BOTTOM + footer.getTotalHeight() + 40);
        PdfWriter writer = ReportHeaderPdf.open(document, out);
        writer.setPageEvent(new ReferenceFooter(footer));
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                document.newPage();
            }
            ARVReportData order = orders.get(index);
            ReportHeaderPdf.add(document, settings.studyName(), ReportHeaderPdf.siteNameLines());
            StudyPatientBlockPdf.add(document, order);
            document.add(new Paragraph("Diagnostic Clinique", HEADING_FONT));
            addVersionOneResults(document, order, settings.images());
            if (order.getAllQaEvents() != null) {
                document.newPage();
                ReportHeaderPdf.add(document, settings.studyName(), ReportHeaderPdf.siteNameLines());
                StudyPatientBlockPdf.add(document, order);
                Paragraph title = new Paragraph("RAPPORT DE NON-CONFORMITE CLIENT", HEADING_FONT);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(8);
                document.add(title);
                StudyNonConformityPdf.ChecklistMarks marks = StudyNonConformityPdf.ChecklistMarks
                        .of(order.getAllQaEvents());
                document.add(StudyNonConformityPdf.tubeReasons(marks));
                document.add(StudyNonConformityPdf.otherReasons(marks, false));
                StudyNonConformityPdf.addSections(document, StudyNonConformityPdf.SectionMarks.of(order));
                document.add(StudyNonConformityPdf.conclusion(false));
            }
        }
        document.close();
        return out.toByteArray();
    }

    /** Each section appears when the order has its first result. */
    private static void addVersionOneResults(Document document, ARVReportData order, Images images) {
        if (order.getGb() != null) {
            addPanel(document, "Hématologie : Hémogramme",
                    "(Analyseur : Sysmex XT 4000I/ Sysmex XT 2000I- Sysmex XN 1000) Echantillon : Sang total/Tube EDTA)",
                    "Méthode: Impédance et cryométrie avec laser semi-conducteur", images.hematology());
            document.add(sampleState(order.getHematologyQaEvent()));
            addGrid(document,
                    new String[][] { { "GB(10^3/ul)", order.getGb() }, { "GR(10^6/ul)", order.getGr() },
                            { "Hb(g/dl)", order.getHb() }, { "Hct(%)", order.getHct() }, { "VGM(fl)", order.getVgm() },
                            { "CCMH(fl)", order.getCcmh() }, { "TCMH(pg)", order.getTcmh() } });
            addGrid(document,
                    new String[][] { { "Plaq(10^3/ul)", order.getPlq() }, { "N(%)", order.getNper() },
                            { "L(%)", order.getLper() }, { "M(%)", order.getMper() }, { "Eo(%)", order.getEoper() },
                            { "Ba(%)", order.getBper() } });
        }
        if (order.getCd4per() != null) {
            addPanel(document, "Immunologie : Phénotypage lymphocytaire",
                    "(Analyseur : BD FACSCanto II /BD FASCalibur- Becton Dickinson) Echantillon : Sang total/Tube EDTA)",
                    "Méthode: Cytométrie de flux", images.immunology());
            document.add(sampleState(order.getImmunologyQaEvent()));
            addGrid(document, new String[][] { { "CD4(%)", order.getCd4per() }, { "CD4#(cell/µl)", order.getCd4() },
                    { null, null }, { null, null }, { null, null }, { null, null }, { null, null } });
        }
        if (order.getCreatininemie() != null) {
            addPanel(document, "Biochimie:",
                    "(Analyseur : Cobas C311/ Cobas Intégra 400 plus- Roche) Echantillon : Sérum/Tube sec)",
                    "Méthode: Jaffe sans compensation", images.biochemistry());
            document.add(sampleState(order.getBiochemistryQaEvent()));
            addGrid(document,
                    new String[][] { { "Créatinine(mg/l)", order.getCreatininemie() },
                            { "SGPT(UI/L)", order.getSgpt() }, { "SGOT(UI/L)", order.getSgot() },
                            { "Glycémie(g/l)", order.getGlyc() }, { null, null }, { null, null } });
        }
        if (order.getVih() != null && !order.getVih().contains("En cours")) {
            document.add(new Paragraph("Sérologie", HEADING_FONT));
            addPanel(document, "Sérologie VIH :",
                    "(Analyseur : Evolis twin plus/PR3100- Biorad) Echantillon : Sérum/Tube sec ou Plasma/Tube EDTA)",
                    "Méthodes: ELISA, Test en ligne", images.serology());
            document.add(new Paragraph(order.getVih(), TEXT_FONT));
            document.add(sampleState(order.getSerologyQaEvent()));
        }
    }

    /**
     * A section's heading, then its reference image beside the analyzer and method.
     */
    private static void addPanel(Document document, String heading, String analyzer, String method, byte[] image) {
        Paragraph title = new Paragraph(heading, SECTION_FONT);
        title.setSpacingBefore(6);
        title.setSpacingAfter(4);
        document.add(title);
        PdfPTable row = new PdfPTable(new float[] { 140, 412 });
        row.setWidthPercentage(100);
        PdfPCell imageCell = plain("", SMALL_FONT);
        Image scaled = image(image, 130, 24);
        if (scaled != null) {
            imageCell = new PdfPCell(scaled, false);
            imageCell.setBorder(Rectangle.NO_BORDER);
        }
        row.addCell(imageCell);
        row.addCell(plain(analyzer + "\n" + method, SMALL_FONT));
        document.add(row);
    }

    private static Paragraph sampleState(String qaEvent) {
        Paragraph state = new Paragraph("Etat de l'échantillon:" + (qaEvent == null ? "Normal" : qaEvent), TEXT_FONT);
        state.setAlignment(Element.ALIGN_CENTER);
        state.setSpacingAfter(4);
        return state;
    }

    /**
     * A row of results under their labels; a missing result leaves its column
     * empty.
     */
    private static void addGrid(Document document, String[][] results) {
        PdfPTable grid = new PdfPTable(results.length);
        grid.setWidthPercentage(100);
        for (String[] result : results) {
            grid.addCell(plain(result[1] == null ? "" : result[0], TEXT_FONT));
        }
        for (String[] result : results) {
            grid.addCell(plain(result[1] == null ? "" : result[1], TEXT_FONT));
        }
        grid.setSpacingAfter(4);
        document.add(grid);
    }

    private static Image image(byte[] bytes, float width, float height) {
        if (bytes == null) {
            return null;
        }
        try {
            Image image = Image.getInstance(bytes);
            image.scaleToFit(width, height);
            return image;
        } catch (Exception e) {
            LogEvent.logError(StudyArvResultsPdf.class.getSimpleName(), "image",
                    "Unreadable report image: " + e.getMessage());
            return null;
        }
    }

    /**
     * The laboratory's reference values drawn at the foot of every version one
     * page.
     */
    private static PdfPTable referenceFooter(float width) {
        PdfPTable footer = new PdfPTable(new float[] { 160, 392 });
        footer.setTotalWidth(width);
        footer.setLockedWidth(true);
        PdfPTable left = new PdfPTable(1);
        left.addCell(new PdfPCell(new Phrase("Biochimie\nCréatinine : 7.0-16.0 mg/l(H) / 7.0-15.0mg/l(F)\n"
                + "ALTL: 6-26 UI/L (H) / 4-24 UI/L (F)\nASTL: 7-32 UI/L (H) / 5-30 UI/L (F)\n"
                + "Glycémie : 0.70-1.10 g/l (H) / 0.75-1.10 g/l (F)", SMALL_FONT)));
        left.addCell(new PdfPCell(
                new Phrase("Phénotypage Lymphocytaire:\nCD4% :33-58%\nCD4# : 404-1612 cellules/µl", SMALL_FONT)));
        left.addCell(new PdfPCell(new Phrase("Charge Virale : <LL : Charge virale indétectable ou inférieure "
                + "à la limite de détection (20 copies/ml)", SMALL_FONT)));
        PdfPCell leftCell = new PdfPCell(left);
        leftCell.setPadding(0);
        footer.addCell(leftCell);

        PdfPTable ranges = new PdfPTable(2);
        ranges.addCell(plain("GB = 3 – 15 .10^3/µl\nLY = 20.0 – 50.0 (%) 1.0 – 3.70 (10^3/µl)\n"
                + "Ne = 37.0 – 72.0 (%) 1.5 – 7.0 (10^3/µl)\nMo = 0.0 – 14.0 (%) 0.0 – 0.7 (10^3/µl)\n"
                + "Eo = 0.0 – 6.0 (%) 0.0 – 0.4 (10^3/µl)\nBo = 0.0 – 0.1 (%) 0.0 – 0.1 (10^3/µl)\n"
                + "PLT = 50 - 400 (10^3/µl)", SMALL_FONT));
        ranges.addCell(plain("GR = 2.5-5.5 (10^6/µl)\nHG = 8.0 -17.0 (g/dL)\nHCT = 26.0-50.0 (%)\n"
                + "VGM = 86.0 -110.0 (fL)\nTCMH =26.0 – 38.0 (pg)\nCCMH = 31.0 – 37.0 (g/dL)", SMALL_FONT));
        PdfPCell right = new PdfPCell();
        right.addElement(new Phrase("Hématologie (Hémogramme : NFS)\nHomme/Femme", SMALL_FONT));
        right.addElement(ranges);
        right.addElement(new Phrase("Enfant: Se référer aux guides des valeurs de référence transmises", SMALL_FONT));
        footer.addCell(right);
        return footer;
    }

    /** The date the report was printed, at the foot of every page. */
    private static final class ReportDateFooter extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(DateUtil.getCurrentDateAsText(), TEXT_FONT), document.right(), document.bottom() - 20,
                    0);
        }
    }

    private static final class ReferenceFooter extends PdfPageEventHelper {
        private final PdfPTable footer;

        ReferenceFooter(PdfPTable footer) {
            this.footer = footer;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float top = FOOTER_BOTTOM + footer.getTotalHeight();
            Phrase title = new Phrase("Valeurs de Référence", TEXT_FONT);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, title, document.left(), top + 4,
                    0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(DateUtil.getCurrentDateAsText(), TEXT_FONT), document.right(), top + 4, 0);
            footer.writeSelectedRows(0, -1, document.left(), top, writer.getDirectContent());
        }
    }

    static byte[] versionTwo(List<ARVReportData> orders, Settings settings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 30, 30, 30, 40);
        ReportHeaderPdf.open(document, out).setPageEvent(new ReportDateFooter());
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                document.newPage();
            }
            ARVReportData order = orders.get(index);
            ReportHeaderPdf.add(document, settings.studyName(), ReportHeaderPdf.siteNameLines());
            StudyPatientBlockPdf.add(document, order);
            document.add(versionTwoResults(order, settings));
        }
        document.close();
        return out.toByteArray();
    }

    private static PdfPTable versionTwoResults(ARVReportData order, Settings settings) {
        PdfPTable table = new PdfPTable(new float[] { 60, 170, 90, 100, 100 });
        table.setWidthPercentage(100);
        table.addCell(blank(3));
        PdfPTable heading = new PdfPTable(new float[] { 20, 140, 20 });
        heading.addCell(centred("H", TEXT_FONT));
        heading.addCell(centred("Valeurs de référence", UNDERLINED_FONT));
        heading.addCell(centred("F", TEXT_FONT));
        PdfPCell headingCell = new PdfPCell(heading);
        headingCell.setColspan(2);
        headingCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(headingCell);

        addSection(table, "Hémogramme", "(Réalisé sur Sysmex XT 4000 I/Sysmes XT 2000 I - Sysmex)");
        addGroup(table, "Numération globulaire");
        addResult(table, "Globules blancs (10^3/ul)", order.getGb(), "4 - 10", "4 - 10");
        addResult(table, "Globules rouge (10^6/ul)", order.getGr(), "4,2 - 5,7", "4 - 5,3");
        addResult(table, "Hémoglobine(g/dl)", order.getHb(), "13 - 18", "11 - 16");
        addResult(table, "Hématocrite(%)", order.getHct(), "40 - 52", "35 - 45");
        addResult(table, "VGM(fl)", order.getVgm(), "80 - 95", "80 - 95");
        addResult(table, "TCMH(pg)", order.getTcmh(), "25 - 27", "25 - 27");
        addResult(table, "CCMH(%)", order.getCcmh(), "32 - 36", "32 - 36");
        addResult(table, "Plaquettes(10^3/ul)", order.getPlq(), "150 450", "150 - 450");
        addGroup(table, "Formule sanguine");
        addResult(table, "Neutrophiles(%)", order.getNper(), "40 - 75", "35 - 70");
        addResult(table, "Lymphocytes(%)", order.getLper(), "20 45", "20 - 40");
        addResult(table, "Monocytes(%)", order.getMper(), "2 - 8", "2 - 8");
        addResult(table, "Eosinophiles(%)", order.getEoper(), "1 - 4", "1 - 4");
        addResult(table, "Basophiles(%)", order.getBper(), "0 - 1", "0 - 1%");

        addSection(table, "Biochimie", "(Réalisé sur Cobas C311/Cobas Integra 400 Plus - Roche)");
        addResult(table, "Glycémie(g/l)", order.getGlyc(), "0.60 - 1.1 g/l", "0.60 – 1.1 g/l");
        addResult(table, "Créat.(mg/l)", order.getCreatininemie(), "6.0 - 12.0 mg/l", "6.0 – 12.0 mg/l");
        addResult(table, "SGPT(UI/L)", order.getSgpt(), atMost("41 UI/l"), atMost("41 UI/l"));
        addResult(table, "SGOT(UI/L)", order.getSgot(), atMost("37 UI/l"), atMost("37 UI/l"));

        addSection(table, "Phénotypage Lymphocytaire",
                "(Réalisé sur BD FACSCanto II/ BD FACSCalibur- Becton Dickinson)");
        addResult(table, "CD4(%)", order.getCd4per(), new Phrase("20 - 40", TEXT_FONT), null);
        addResult(table, "CD4#(cel/ul)", order.getCd4(), new Phrase("500 - 1600", TEXT_FONT), null);

        if (Boolean.TRUE.equals(order.getShowSerologie())) {
            addSection(table, "Sérologie", "(Réalisé sur Evolis twin - Biorad)");
            addResult(table, "Statut sérologique", order.getVih(), new Phrase("", TEXT_FONT), null);
        }

        PdfPCell molecular = plain("Diagnostic moléculaire", HEADING_FONT);
        molecular.setColspan(5);
        molecular.setPaddingTop(8);
        table.addCell(molecular);
        addSection(table, "Charge virale", "(Réalisé sur Cobas Taqman - Roche)");
        addResult(table, "Ampli2", order.getAmpli2(), "copies/ml", "<LL");
        addResult(table, "Ampli2lo", order.getAmpli2lo(), "logcopies/ml", "<LL");
        if (Boolean.TRUE.equals(order.getShowPCR())) {
            table.addCell(blank(1));
            PdfPCell label = plain("PCR (Réalisé sur Cobas Taqman - Roche) :", TEXT_FONT);
            label.setColspan(2);
            table.addCell(label);
            PdfPCell value = plain(missingAsX(order.getPcr()), TEXT_FONT);
            value.setColspan(2);
            table.addCell(value);
        }
        return table;
    }

    private static void addSection(PdfPTable table, String name, String analyzer) {
        Phrase phrase = new Phrase(name, SECTION_FONT);
        phrase.add(new Chunk("  " + analyzer, SMALL_FONT));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setColspan(5);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingTop(8);
        table.addCell(cell);
    }

    private static void addGroup(PdfPTable table, String name) {
        PdfPCell cell = plain(name, TEXT_FONT);
        cell.setColspan(5);
        table.addCell(cell);
    }

    private static void addResult(PdfPTable table, String name, String value, String men, String women) {
        addResult(table, name, value, new Phrase(men, TEXT_FONT), new Phrase(women, TEXT_FONT));
    }

    /**
     * A result row; a reference shared by men and women (women null) spans both
     * columns.
     */
    private static void addResult(PdfPTable table, String name, String value, Phrase men, Phrase women) {
        table.addCell(blank(1));
        table.addCell(plain(name, TEXT_FONT));
        table.addCell(plain(missingAsX(value), TEXT_FONT));
        PdfPCell menCell = new PdfPCell(men);
        menCell.setBorder(Rectangle.NO_BORDER);
        menCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        if (women == null) {
            menCell.setColspan(2);
            table.addCell(menCell);
            return;
        }
        table.addCell(menCell);
        PdfPCell womenCell = new PdfPCell(women);
        womenCell.setBorder(Rectangle.NO_BORDER);
        womenCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(womenCell);
    }

    /** The study printed "at most" as an underlined "<". */
    private static Phrase atMost(String limit) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk("<", UNDERLINED_FONT));
        phrase.add(new Chunk(" " + limit, TEXT_FONT));
        return phrase;
    }

    private static String missingAsX(String value) {
        return value == null ? "X" : value;
    }

    private static PdfPCell plain(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private static PdfPCell centred(String text, Font font) {
        PdfPCell cell = plain(text, font);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell blank(int columns) {
        PdfPCell cell = plain("", TEXT_FONT);
        cell.setColspan(columns);
        return cell;
    }
}
