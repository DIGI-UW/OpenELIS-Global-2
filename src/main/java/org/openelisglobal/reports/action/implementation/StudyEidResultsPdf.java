package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.reports.action.implementation.reportBeans.EIDReportData;
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
 * The study (RetroCI) early infant diagnosis results. Version one describes the
 * PCR examination, its result and how to interpret it, with the client
 * non-conformity checklist on its own page when the order has an EID
 * non-conformity; version two gives the PCR result with the child's clinic and
 * a short glossary. The texts are the study's, in French in every language.
 */
final class StudyEidResultsPdf {

    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 12);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9);
    private static final Font UNDERLINED_FONT = new Font(Font.HELVETICA, 9, Font.UNDERLINE);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8);

    private static final String INTERPRETATIONS = "1- Si la PCR sur ADN est positive sur le premier prélèvement "
            + "(PCR1), refaire un deuxième prélèvement (PCR2) pour une deuxième PCR dont le but est de confirmer le "
            + "premier résultat. Et commencer immédiatement le traitement antirétroviral\n\n"
            + "2- Si la PCR sur ADN est négative, l’enfant n’est pas infecté par le VIH\n"
            + "a. Prélever l’enfant pour un autre test VIH à l’âge de trois mois si le premier test a été fait chez "
            + "un enfant âgé de moins de trois mois\n"
            + "b. Si l’enfant est sous allaitement maternel, répéter la PCR 6 semaines après l’arrêt de l’allaitement "
            + "pour avoir le statut définitif de l’enfant\n\n"
            + "3- En cas de résultats discordants entre la PCR1 et la PCR2, faire un troisième prélèvement pour avoir "
            + "le statut définitif de l’enfant";

    private static final String PRECAUTIONS = "Les tests sont réalisés en présence de contrôles internes positif et "
            + "négatif qui permettent de valider le résultat\nLe test est validé sur DBS ou sang total prélevé sur "
            + "EDTA. L’utilisation d’autres types de support peut engendrer des faux positifs ou faux négatifs ; par "
            + "exemple l’héparine inhibe la PCR\nLa performance de ce test a été évaluée uniquement sur HIV-1 group "
            + "O et N, et non sur HIV-2";

    private static final String GLOSSARY = "LEXIQUE\nSi Résultat PCR = Positif : Faire un second prélèvement pour "
            + "confirmation, s’il s’agit d’une première PCR\nSi Résultat PCR = indéterminé : Faire un second "
            + "prélèvement pour confirmation";

    /**
     * The section heading images and laboratory reference, when the site has them.
     */
    record Images(byte[] examination, byte[] results, byte[] interpretations, byte[] precautions,
            byte[] laboratoryReference) {
        static final Images NONE = new Images(null, null, null, null, null);
    }

    record Settings(String studyName, Images images) {
    }

    private StudyEidResultsPdf() {
    }

    static byte[] versionOne(List<EIDReportData> orders, Settings settings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 30, 30, 30, 48);
        PdfWriter writer = ReportHeaderPdf.open(document, out);
        writer.setPageEvent(new ReportDateFooter());
        Images images = settings.images();
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                document.newPage();
            }
            EIDReportData order = orders.get(index);
            addVersionOneHeading(document, order, settings);
            addImage(document, images.laboratoryReference(), 260, 24, Element.ALIGN_RIGHT);
            addImage(document, images.examination(), 476, 35, Element.ALIGN_LEFT);
            PdfPTable examination = new PdfPTable(new float[] { 120, 430 });
            examination.setWidthPercentage(100);
            addExaminationRow(examination, "Type d’Examen :",
                    "Recherche de l’ADN du virus HIV-1 par amplification génétique (PCR)");
            addExaminationRow(examination, "Type de prélèvement :",
                    "Sang total sur EDTA ou sur Papier Buvard Type Whatman 903");
            addExaminationRow(examination, "Etat de l’échantillon :",
                    (order.getVirologyEidQaEvent() == null ? "Normal" : order.getVirologyEidQaEvent())
                            + " (3 spots de sang collectés selon les procédures nationales)");
            addExaminationRow(examination, "Analyseur :", "Cobas Ampliprep/Cobas Taqman");
            addExaminationRow(examination, "Trousse commerciale:",
                    "Cobas Ampliprep/Cobas Taqman HIV 1 Qualitative (HI2QCAP)");
            document.add(examination);

            addHeading(document, images.results(), "RÉSULTATS");
            PdfPTable results = new PdfPTable(new float[] { 155, 195 });
            results.setWidthPercentage(64);
            results.setHorizontalAlignment(Element.ALIGN_LEFT);
            for (String[] row : new String[][] { { "Virologie", "Résultats" }, { "HIV-1", hivStatus(order) },
                    { "Rang de la PCR", rank(order) } }) {
                results.addCell(new Phrase(row[0], TEXT_FONT));
                results.addCell(new Phrase(row[1], TEXT_FONT));
            }
            document.add(results);

            addHeading(document, images.interpretations(), "INTERPRÉTATIONS");
            document.add(new Paragraph(INTERPRETATIONS, SMALL_FONT));
            addHeading(document, images.precautions(), "Précautions et Limites de la Procédure");
            document.add(new Paragraph(PRECAUTIONS, SMALL_FONT));

            if (order.getVirologyEidQaEvent() != null) {
                document.newPage();
                addVersionOneHeading(document, order, settings);
                Paragraph title = new Paragraph("RAPPORT DE NON-CONFORMITE CLIENT", HEADING_FONT);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(8);
                document.add(title);
                StudyNonConformityPdf.ChecklistMarks marks = StudyNonConformityPdf.ChecklistMarks
                        .of(order.getAllQaEvents());
                document.add(StudyNonConformityPdf.dbsCardReasons(marks));
                document.add(StudyNonConformityPdf.otherReasons(marks, true));
                StudyNonConformityPdf.addSections(document, new StudyNonConformityPdf.SectionMarks(
                        order.getReceptionQaEvent() != null, false, false, false, true, false, false));
                document.add(StudyNonConformityPdf.conclusion(true));
            }
        }
        document.close();
        return out.toByteArray();
    }

    /** The site header and the child's block, repeated on every page. */
    private static void addVersionOneHeading(Document document, EIDReportData order, Settings settings) {
        ReportHeaderPdf.add(document, settings.studyName(), ReportHeaderPdf.siteNameLines());
        PdfPTable child = new PdfPTable(new float[] { 220, 150, 180 });
        child.setWidthPercentage(100);
        child.setSpacingBefore(8);
        child.setSpacingAfter(8);
        child.addCell(field("Numéro DBS :", order.getSubjectno()));
        child.addCell(field("Labno :", order.getAccession_number()));
        child.addCell(field("Date Prél. :", day(order.getCollectiondate())));
        child.addCell(field("Numéro Enfant Site :", order.getSitesubjectno()));
        child.addCell(field(null, null));
        child.addCell(field("Date de réception :", day(order.getReceptiondate())));
        child.addCell(field("Age :", age(order)));
        child.addCell(field(null, null));
        child.addCell(field("Date de Réalisation du test :", order.getCompleationdate()));
        child.addCell(field("Date de Naissance :", order.getBirth_date()));
        PdfPCell doctor = new PdfPCell();
        doctor.setColspan(2);
        doctor.setRowspan(2);
        doctor.addElement(labelled("Médecin :", order.getDoctor()));
        doctor.addElement(labelled("Service :", order.getServicename()));
        child.addCell(doctor);
        child.addCell(field("Sexe :", order.getGender()));
        document.add(child);
    }

    static byte[] versionTwo(List<EIDReportData> orders, Settings settings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 30, 30, 30, 80);
        PdfWriter writer = ReportHeaderPdf.open(document, out);
        writer.setPageEvent(new GlossaryFooter());
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                document.newPage();
            }
            EIDReportData order = orders.get(index);
            ReportHeaderPdf.add(document, settings.studyName(), ReportHeaderPdf.siteNameLines());
            PdfPTable child = new PdfPTable(2);
            child.setWidthPercentage(100);
            child.setSpacingBefore(8);
            child.setSpacingAfter(10);
            child.addCell(field("Numero DBS Enfant:", order.getSubjectno()));
            child.addCell(field("Labno:", order.getAccession_number()));
            child.addCell(field("Numero Enfant Site:", order.getSitesubjectno()));
            child.addCell(field("Date Prél. :", order.getCollectiondate()));
            child.addCell(field("Numéro CDV ou PTME:", order.getPTME()));
            child.addCell(field("Date de réception :", order.getReceptiondate()));
            child.addCell(field("Age:", age(order)));
            child.addCell(field("Date de Réalisation du Test:", order.getCompleationdate()));
            child.addCell(field("Date de Naissance:", order.getBirth_date()));
            child.addCell(field("District sanitaire:", order.getClinicDistrict()));
            child.addCell(field("Sexe:", order.getGender()));
            child.addCell(field("Structure sanitaire:", order.getClinic()));
            PdfPCell service = field("Service:", order.getServicename());
            service.setColspan(2);
            child.addCell(service);
            document.add(child);

            document.add(new Paragraph("Résultats PCR – dépistage Précoce Enfant VIH :", HEADING_FONT));
            for (String[] row : new String[][] { { "Technique utilisée :", "DNA PCR" },
                    { "Résultat du diagnostic PCR :", hivStatus(order) }, { "Rang de la PCR:", rank(order) } }) {
                Paragraph line = new Paragraph(row[0] + " " + row[1], TEXT_FONT);
                line.setSpacingBefore(8);
                document.add(line);
            }
            Paragraph comment = new Paragraph("Commentaire Laboratoire:", HEADING_FONT);
            comment.setSpacingBefore(14);
            document.add(comment);
        }
        document.close();
        return out.toByteArray();
    }

    /** A result recorded without a status was negative. */
    private static String hivStatus(EIDReportData order) {
        return order.getHiv_status() == null ? "Négatif" : order.getHiv_status();
    }

    private static String rank(EIDReportData order) {
        String type = order.getPcr_type();
        if (type == null) {
            return "Inconnu";
        }
        return switch (type) {
        case "First PCR" -> "1";
        case "Second PCR" -> "2";
        default -> type;
        };
    }

    /**
     * The age in months or in weeks, whichever the order records; the other prints
     * as "--".
     */
    private static String age(EIDReportData order) {
        return Objects.toString(order.getAgeMonth(), "--") + " Mois / " + Objects.toString(order.getAgeWeek(), "--")
                + " Semaines";
    }

    /** A date and time without the time. */
    private static String day(String dateTime) {
        return dateTime != null && dateTime.length() > 10 ? dateTime.substring(0, 10) : dateTime;
    }

    /**
     * A section's heading image, or its title when the site has no image for it.
     */
    private static void addHeading(Document document, byte[] image, String title) {
        if (image != null) {
            Paragraph spacer = new Paragraph(" ", SMALL_FONT);
            document.add(spacer);
            addImage(document, image, 340, 20, Element.ALIGN_LEFT);
            return;
        }
        Paragraph heading = new Paragraph(title, HEADING_FONT);
        heading.setSpacingBefore(10);
        heading.setSpacingAfter(4);
        document.add(heading);
    }

    private static void addExaminationRow(PdfPTable table, String label, String value) {
        table.addCell(plain(label, UNDERLINED_FONT));
        table.addCell(plain(value, TEXT_FONT));
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
            LogEvent.logError(StudyEidResultsPdf.class.getSimpleName(), "addImage",
                    "Unreadable report image: " + e.getMessage());
        }
    }

    private static PdfPCell field(String label, String value) {
        PdfPCell cell = new PdfPCell(label == null ? new Phrase("") : labelled(label, value));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4);
        return cell;
    }

    /** The label carries its own colon, spaced as each version printed it. */
    private static Phrase labelled(String label, String value) {
        Phrase phrase = new Phrase(label + " ", LABEL_FONT);
        phrase.add(new Phrase(Objects.toString(value, ""), TEXT_FONT));
        return phrase;
    }

    private static PdfPCell plain(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(3);
        return cell;
    }

    /** The report date at the foot of every version one page. */
    private static final class ReportDateFooter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(DateUtil.getCurrentDateAsText(), SMALL_FONT), document.right(), document.bottom() - 24,
                    0);
        }
    }

    /** The glossary and the report date at the foot of every version two page. */
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
