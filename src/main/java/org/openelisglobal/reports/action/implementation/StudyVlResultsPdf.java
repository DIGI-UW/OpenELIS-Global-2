package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.reports.action.implementation.reportBeans.VLReportData;
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
 * The study (RetroCI) viral load results: the RNA PCR examination, the viral
 * load in copies and in log, how to interpret it, and the detection thresholds
 * at the foot of every page, with the client non-conformity checklist on its
 * own page when the order has a viral load non-conformity. The texts are the
 * study's, in French in every language.
 */
final class StudyVlResultsPdf {

    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 12);
    private static final Font UNDERLINED_FONT = new Font(Font.HELVETICA, 9, Font.UNDERLINE);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8);

    private static final String INTERPRETATIONS = "1-Si le titre HIV-1<=1000 copies/mL, suppression virale selon les "
            + "directives nationales de suivi biologique des patients sous traitement antiretroviral\n\n"
            + "2-Si le titre HIV-1>1000 copies/mL, le virus est détectable au niveau du sang périphérique et le "
            + "patient est dit en échec virologique. Refaire un autre prélèvement trois mois après le conseil à "
            + "l’observance, pour un autre examen de charge virale pour confirmer et infirmer l’échec virologique";

    private static final String[] THRESHOLDS = { "Seuil de détection de la technique CV/PL : 20 copies /mL",
            "Seuil de détection de la technique CV/DBS : 400 copies /mL",
            "Seuil de détection de la technique CV/PSC : 599 copies/mL",
            "Valeur charge virale : < LL=virus HIV-1 indétectable dans le sang périphérique" };

    /**
     * The section heading images and laboratory reference, when the site has them.
     */
    record Images(byte[] laboratoryReference, byte[] examination, byte[] results, byte[] interpretations,
            byte[] referenceValues) {
        static final Images NONE = new Images(null, null, null, null, null);
    }

    record Settings(String studyName, Images images) {
    }

    private StudyVlResultsPdf() {
    }

    static byte[] render(List<VLReportData> orders, Settings settings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 30, 30, 30, 120);
        PdfWriter writer = ReportHeaderPdf.open(document, out);
        writer.setPageEvent(new ThresholdsFooter(settings.images().referenceValues()));
        Images images = settings.images();
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                document.newPage();
            }
            VLReportData order = orders.get(index);
            addHeading(document, order, settings);
            addImage(document, images.laboratoryReference(), 260, 24, Element.ALIGN_RIGHT);
            addSectionHeading(document, images.examination(), "EXAMEN DE CHARGE VIRALE");
            PdfPTable examination = new PdfPTable(new float[] { 122, 428 });
            examination.setWidthPercentage(100);
            addExaminationRow(examination, "Type d’Examen :",
                    "Recherche de l’ARN du virus HIV-1 par amplification génétique (PCR) et par quantification");
            addExaminationRow(examination, "Type de prélèvement :", order.getSampleTypeName());
            addExaminationRow(examination, "Etat de l’échantillon :",
                    order.getVirologyVlQaEvent() == null ? "Normal" : order.getVirologyVlQaEvent());
            addExaminationRow(examination, "Automate :",
                    "COBAS Ampliprep / Cobas Taqman Series / Cobas 6800 Roche Diagnostic / Cobas 4800");
            addExaminationRow(examination, "Trousse commerciale:",
                    "COBAS Taqman/Ampliprep HIV-1 tests,V2.0 Quantitative (HI2CAP) / C4800 HIV-1 AMP/DET\n"
                            + "COBAS 6800/8800 HIV-1 P/N");
            document.add(examination);

            addSectionHeading(document, images.results(), "RÉSULTATS");
            PdfPTable results = new PdfPTable(new float[] { 150, 210, 140 });
            results.setWidthPercentage(90);
            results.setHorizontalAlignment(Element.ALIGN_LEFT);
            for (String[] row : new String[][] { { "Virologie", "Résultats nombre de copies /mL", "Résultats Log /mL" },
                    { order.getvih(), missingAsX(order.getAmpli2()), missingAsX(order.getAmpli2lo()) } }) {
                for (String value : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, TEXT_FONT));
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setPaddingBottom(4);
                    results.addCell(cell);
                }
            }
            document.add(results);

            addSectionHeading(document, images.interpretations(), "INTERPRÉTATIONS");
            document.add(new Paragraph(INTERPRETATIONS, SMALL_FONT));

            if (order.getVirologyVlQaEvent() != null) {
                document.newPage();
                addHeading(document, order, settings);
                Paragraph title = new Paragraph("RAPPORT DE NON-CONFORMITE CLIENT", HEADING_FONT);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(8);
                document.add(title);
                StudyNonConformityPdf.ChecklistMarks marks = StudyNonConformityPdf.ChecklistMarks
                        .of(order.getAllQaEvents());
                document.add(StudyNonConformityPdf.tubeReasons(marks));
                document.add(StudyNonConformityPdf.otherReasons(marks, false));
                StudyNonConformityPdf.addSections(document, new StudyNonConformityPdf.SectionMarks(
                        order.getReceptionQaEvent() != null, false, false, true, false, false, false));
                document.add(StudyNonConformityPdf.conclusion(false));
            }
        }
        document.close();
        return out.toByteArray();
    }

    /** The site header and the patient block, repeated on every page. */
    private static void addHeading(Document document, VLReportData order, Settings settings) {
        ReportHeaderPdf.add(document, settings.studyName(), ReportHeaderPdf.siteNameLines());
        StudyPatientBlockPdf.add(document, new StudyPatientBlockPdf.Patient(
                order.getSubjectno() == null ? order.getSitesubjectno() : order.getSubjectno(),
                order.getAccession_number(), order.getGender(), afterKey(order.getVlPregnancy()), order.getBirth_date(),
                order.getAge(), afterKey(order.getVlSuckle()), order.getCollectiondate(), order.getReceptiondate(),
                order.getDoctor(), order.getServicename(), order.getCompleationdate(), order.getReleasedate()));
    }

    /** Pregnancy and breastfeeding arrive as "key=answer". */
    private static String afterKey(String value) {
        if (value == null) {
            return null;
        }
        int equals = value.indexOf('=');
        return equals < 0 ? value : value.substring(equals + 1);
    }

    /** The log value is formatted with a trailing line break. */
    private static String missingAsX(String value) {
        return value == null ? "X" : value.strip();
    }

    /**
     * A section's heading image, or its title when the site has no image for it.
     */
    private static void addSectionHeading(Document document, byte[] image, String title) {
        if (image != null) {
            document.add(new Paragraph(" ", SMALL_FONT));
            addImage(document, image, 340, 20, Element.ALIGN_LEFT);
            return;
        }
        Paragraph heading = new Paragraph(title, HEADING_FONT);
        heading.setSpacingBefore(10);
        heading.setSpacingAfter(4);
        document.add(heading);
    }

    private static void addExaminationRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, UNDERLINED_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value == null ? "" : value, TEXT_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(3);
        table.addCell(valueCell);
    }

    private static void addImage(Document document, byte[] bytes, float width, float height, int alignment) {
        if (bytes == null) {
            return;
        }
        try {
            Image image = Image.getInstance(bytes);
            image.scaleToFit(width, height);
            image.setAlignment(alignment);
            document.add(image);
        } catch (Exception e) {
            LogEvent.logError(StudyVlResultsPdf.class.getSimpleName(), "addImage",
                    "Unreadable report image: " + e.getMessage());
        }
    }

    /**
     * The reference values heading and the detection thresholds at the foot of
     * every page.
     */
    private static final class ThresholdsFooter extends PdfPageEventHelper {
        private final byte[] heading;

        ThresholdsFooter(byte[] heading) {
            this.heading = heading;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText column = new ColumnText(writer.getDirectContent());
            column.setSimpleColumn(document.left(), 20, document.right(), document.bottom() - 16);
            Image image = null;
            if (heading != null) {
                try {
                    image = Image.getInstance(heading);
                    image.scaleToFit(317, 16);
                } catch (Exception e) {
                    LogEvent.logError(StudyVlResultsPdf.class.getSimpleName(), "onEndPage",
                            "Unreadable report image: " + e.getMessage());
                }
            }
            if (image != null) {
                column.addElement(image);
            } else {
                column.addElement(new Paragraph("Valeurs de Référence", TEXT_FONT));
            }
            column.addElement(new Paragraph(String.join("\n", THRESHOLDS), SMALL_FONT));
            column.go();
        }
    }
}
