package org.openelisglobal.reports.action.implementation;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.reports.action.implementation.reportBeans.ARVReportData;
import org.openelisglobal.reports.action.implementation.reportBeans.FollowupRequiredData;
import org.openelisglobal.reports.action.implementation.reportBeans.NonConformityReportData;
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
 * The study (RetroCI) non-conformity documents: the notification sent to the
 * client for each order, and the orders needing follow-up by service. Their
 * labels are French in every language, as the study printed them.
 */
final class StudyNonConformityPdf {

    private static final Font HEADING_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 9);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 8);

    private StudyNonConformityPdf() {
    }

    /**
     * One page per order, its non-conformities in arrival order; the items arrive
     * grouped by order.
     */
    static byte[] notification(List<NonConformityReportData> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 36, 36, 36, 90);
        PdfWriter writer = ReportHeaderPdf.open(document, out);
        writer.setPageEvent(new ClientFooter());
        int start = 0;
        while (start < items.size()) {
            int end = start;
            while (end < items.size()
                    && Objects.equals(items.get(end).getAccessionNumber(), items.get(start).getAccessionNumber())) {
                end++;
            }
            if (start > 0) {
                document.newPage();
            }
            addOrder(document, items.subList(start, end));
            start = end;
        }
        document.close();
        return out.toByteArray();
    }

    private static void addOrder(Document document, List<NonConformityReportData> events) {
        NonConformityReportData order = events.get(0);
        ReportHeaderPdf.add(document, "RAPPORT DE NON CONFORMITE: CLIENT", ReportHeaderPdf.siteNameLines());
        PdfPTable details = new PdfPTable(2);
        details.setWidthPercentage(100);
        details.setSpacingBefore(12);
        String subjectNumber = order.getSubjectNumber() != null ? order.getSubjectNumber()
                : order.getSiteSubjectNumber();
        addField(details, "Date de survenue", order.getNonConformityDate());
        addField(details, "Etude", order.getStudy());
        addField(details, "Date de prélèvement", order.getReceivedDate());
        addField(details, "Heure de prélèvement", order.getReceivedHour());
        addField(details, "SubjetNo", subjectNumber);
        addField(details, "Service", order.getService());
        addField(details, "Labno", order.getAccessionNumber());
        addField(details, "Prescripteur", order.getDoctor());
        document.add(details);

        Paragraph heading = new Paragraph("Motifs de non conformité", HEADING_FONT);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingBefore(12);
        heading.setSpacingAfter(6);
        document.add(heading);
        PdfPTable table = new PdfPTable(new float[] { 90, 70, 150, 110, 110 });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 3, "Section", "Date", "Motif du refus", "Type d'échantillon",
                "Biologiste");
        for (NonConformityReportData event : events) {
            for (String value : new String[] { event.getSection(), event.getNonConformityDate(),
                    event.getNonConformityReason(), event.getSampleType(), event.getBiologist() }) {
                table.addCell(new Phrase(Objects.toString(value, ""), CELL_FONT));
            }
            if (!GenericValidator.isBlankOrNull(event.getQaNote())) {
                PdfPCell note = new PdfPCell(labelled("Note", event.getQaNote(), CELL_FONT));
                note.setColspan(5);
                table.addCell(note);
            }
        }
        document.add(table);
        Paragraph comment = new Paragraph(labelled("Commentaire", order.getSampleNote(), TEXT_FONT));
        comment.setSpacingBefore(8);
        document.add(comment);
    }

    /**
     * The orders needing follow-up, by service; the items arrive sorted by service.
     */
    static byte[] followupRequired(String title, List<FollowupRequiredData> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 36, 36, 36, 48);
        ReportHeaderPdf.open(document, out).setPageEvent(new ReportDateFooter());
        ReportHeaderPdf.add(document, title, ReportHeaderPdf.siteNameLines());
        int start = 0;
        while (start < items.size()) {
            int end = start;
            while (end < items.size() && Objects.equals(items.get(end).getOrgname(), items.get(start).getOrgname())) {
                end++;
            }
            addService(document, items.subList(start, end));
            start = end;
        }
        document.close();
        return out.toByteArray();
    }

    private static void addService(Document document, List<FollowupRequiredData> orders) {
        PdfPTable table = new PdfPTable(new float[] { 98, 92, 112, 72, 72, 94 });
        table.setWidthPercentage(100);
        table.setSpacingBefore(14);
        table.setHeaderRows(2);
        PdfPCell service = PdfExportSupport.headerCell("Service: " + Objects.toString(orders.get(0).getOrgname(), ""),
                LABEL_FONT, 4);
        service.setColspan(6);
        service.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(service);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 3, "Date de prélèvement", "Date de réception", "Lab No",
                "Sujet No", "Site Sujet No", "Nom du médecin");
        for (FollowupRequiredData order : orders) {
            for (String value : new String[] { order.getCollectiondate(), order.getReceivedDate(), order.getLabNo(),
                    order.getSubjectNumber(), order.getSiteSubjectNumber(), order.getDoctor() }) {
                table.addCell(new Phrase(Objects.toString(value, "").trim(), CELL_FONT));
            }
            addNotes(table, "Non Conformité Remarque", order.getNonConformityNotes());
            addNotes(table, "Suivi Requis Remarque", order.getUnderInvestigationNotes());
        }
        document.add(table);
    }

    /**
     * The client non-conformity checklist, one page per order: the early infant
     * diagnosis (DBS card) form when the order has an EID non-conformity, the tube
     * form otherwise.
     */
    static byte[] checklist(List<ARVReportData> orders) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PdfExportSupport.pageSize(), 36, 36, 36, 80);
        PdfWriter writer = ReportHeaderPdf.open(document, out);
        writer.setPageEvent(new SignatureFooter());
        for (int index = 0; index < orders.size(); index++) {
            if (index > 0) {
                document.newPage();
            }
            ARVReportData order = orders.get(index);
            ReportHeaderPdf.add(document, "RAPPORT DE NON-CONFORMITE CLIENT", ReportHeaderPdf.siteNameLines());
            StudyPatientBlockPdf.add(document, order);
            if (order.getAllQaEvents() == null) {
                Paragraph none = new Paragraph("No QaEvent", HEADING_FONT);
                none.setAlignment(Element.ALIGN_CENTER);
                none.setSpacingBefore(120);
                document.add(none);
                continue;
            }
            ChecklistMarks marks = ChecklistMarks.of(order.getAllQaEvents());
            boolean earlyInfantDiagnosis = order.getVirologyEidQaEvent() != null;
            document.add(earlyInfantDiagnosis ? dbsCardReasons(marks) : tubeReasons(marks));
            document.add(otherReasons(marks, earlyInfantDiagnosis));
            addSections(document, SectionMarks.of(order));
            document.add(conclusion(earlyInfantDiagnosis));
        }
        document.close();
        return out.toByteArray();
    }

    private static final String[][] TUBE_REASONS = { { "Echantillon Coagulé", "coagulated" },
            { "Echantillon Insuffisant", "insufficient" }, { "Echantillon Hémolysé", "hemolytic" },
            { "Echantillon lactescent", "Sample_LA" }, { "Echantillon opalescent", "Sample_OP" },
            { "Echantillon mal ou non étiqueté", "mislabled" }, { "Elution du disque DBS impossible", "DBS_DI" } };

    private static final String[][] DBS_CARD_REASONS = { { "Age de l’enfant > 18 mois", "adult" },
            { "Date de prélèvement au-delà d’un (1) mois", "Date_1" }, { "DBS: Nombre de spot rempli < 3", "DBS_3" },
            { "Diamètre des spots < 5mm", "Diametre" }, { "DBS spot de sang coagulé", "DBS_C" },
            { "DBS spot de sang insuffisant", "DBS_I" }, { "Elution du disque DBS impossible", "DBS_DI" },
            { "DBS spot de sang dilué par l’alcool", "DBS_D" },
            { "Carte DBS sans enveloppe glassine (si plusieurs DBS dans un sachet Ziplock)", "DBS_E" },
            { "Carte DBS non conforme (différente du Whatman 903)", "DBS_NC" }, { "DBS non identifié", "DBS_NI" },
            { "DBS mal identifié (Identité discordante sur DBS et fiche)", "DBS_MI" }, { "DBS sans fiche", "DBS_SF" },
            { "Fiche de prélèvement entachée de sang", "bloodstained.form" },
            { "Fiche de prélèvement sans échantillon DBS", "Fiche_no_DBS" } };

    /** Reasons about the request form, two to a row on the tube form. */
    private static final String[][] FORM_REASONS = { { "Fiche sans Echantillon", "noSample" },
            { "Absence de l’identité du préleveur", "No_ID_Prev" }, { "Echantillon sans fiche", "noForm" },
            { "Absence de l’heure du prélèvement", "No_HR_Prev" }, { "Tube maculé de sang", "bloodstained.tube" },
            { "Echantillon pour charge virale de plus de 6h", "Sample_VL_Late" },
            { "Fiche entachée de sang", "bloodstained.form" }, { "Erreur de tube de prélèvement", "Error_Sample" } };

    static PdfPTable tubeReasons(ChecklistMarks marks) {
        PdfPTable table = new PdfPTable(new float[] { 170, 188, 193 });
        table.setWidthPercentage(100);
        addReasonHeader(table, 2);
        table.addCell(centred("Tube EDTA/Sang total/Plasma"));
        table.addCell(centred("Tube Sec/ Sérum"));
        for (String[] reason : TUBE_REASONS) {
            table.addCell(new Phrase(reason[0], CELL_FONT));
            table.addCell(centred(marks.onEdtaTube(reason[1]) ? "X" : ""));
            table.addCell(centred(marks.onDryTube(reason[1]) ? "X" : ""));
        }
        return table;
    }

    static PdfPTable dbsCardReasons(ChecklistMarks marks) {
        PdfPTable table = new PdfPTable(new float[] { 391, 160 });
        table.setWidthPercentage(100);
        addReasonHeader(table, 1);
        table.addCell(centred("Carte DBS Whatman 903"));
        for (String[] reason : DBS_CARD_REASONS) {
            table.addCell(new Phrase(reason[0], CELL_FONT));
            table.addCell(centred(marks.any(reason[1]) ? "X" : ""));
        }
        return table;
    }

    private static void addReasonHeader(PdfPTable table, int sampleColumns) {
        PdfPCell reason = new PdfPCell(new Phrase("MOTIF DU REFUS", LABEL_FONT));
        reason.setRowspan(2);
        reason.setPadding(6);
        table.addCell(reason);
        PdfPCell sample = new PdfPCell(new Phrase("Echantillon en tube : Type de tube/Type d’échantillon", TEXT_FONT));
        sample.setColspan(sampleColumns);
        table.addCell(sample);
    }

    /**
     * The request form reasons and the DBS box (tube form only), then the other
     * reasons, described by hand.
     */
    static PdfPTable otherReasons(ChecklistMarks marks, boolean earlyInfantDiagnosis) {
        PdfPTable table = new PdfPTable(new float[] { 190, 40, 280, 41 });
        table.setWidthPercentage(100);
        table.setSpacingBefore(earlyInfantDiagnosis ? 10 : 0);
        if (!earlyInfantDiagnosis) {
            addWideReason(table, "Fiche de prélèvement mal renseignée", marks.any("formNotCorrect"));
            addWideReason(table, "Discordance d’information entre Fiche de prélèvement et Fiche démographique",
                    marks.any("Error_Prev_Demo"));
            for (String[] reason : FORM_REASONS) {
                table.addCell(plain(reason[0], CELL_FONT));
                table.addCell(box(marks.any(reason[1])));
            }
            addBoxRow(table, "DBS");
        }
        addBoxRow(table, "AUTRES MOTIFS");
        PdfPCell describe = plain("A décrire : ________________________________________________________________",
                CELL_FONT);
        describe.setColspan(4);
        table.addCell(describe);
        return table;
    }

    private static void addWideReason(PdfPTable table, String label, boolean ticked) {
        PdfPCell cell = plain(label, CELL_FONT);
        cell.setColspan(3);
        table.addCell(cell);
        table.addCell(box(ticked));
    }

    /** A heading with an empty box, ticked by hand. */
    private static void addBoxRow(PdfPTable table, String label) {
        table.addCell(plain(label, LABEL_FONT));
        table.addCell(box(false));
        PdfPCell rest = plain("", CELL_FONT);
        rest.setColspan(2);
        table.addCell(rest);
    }

    /** The sections an order's non-conformities came from. */
    record SectionMarks(boolean reception, boolean biochemistry, boolean immunology, boolean viralLoad,
            boolean earlyInfantDiagnosis, boolean serology, boolean hematology) {
        static SectionMarks of(ARVReportData order) {
            return new SectionMarks(order.getReceptionQaEvent() != null, order.getBiochemistryQaEvent() != null,
                    order.getImmunologyQaEvent() != null, order.getVirologyVlQaEvent() != null,
                    order.getVirologyEidQaEvent() != null, order.getSerologyQaEvent() != null,
                    order.getHematologyQaEvent() != null);
        }
    }

    static void addSections(Document document, SectionMarks sections) {
        PdfPTable first = new PdfPTable(new float[] { 52, 40, 18, 62, 18, 58, 18, 100, 18, 72, 18 });
        first.setWidthPercentage(100);
        first.setSpacingBefore(8);
        first.addCell(plain("Section:", LABEL_FONT));
        addSection(first, "Saisie", false);
        addSection(first, "Réception", sections.reception());
        addSection(first, "Biochimie", sections.biochemistry());
        addSection(first, "Immunologie(CD4)", sections.immunology());
        addSection(first, "Charge virale", sections.viralLoad());
        document.add(first);
        PdfPTable second = new PdfPTable(new float[] { 52, 130, 18, 80, 18, 80, 18, 78 });
        second.setWidthPercentage(100);
        second.setSpacingBefore(4);
        second.addCell(plain("", CELL_FONT));
        addSection(second, "Diagnostic précoce (EID)", sections.earlyInfantDiagnosis());
        addSection(second, "Sérologie VIH", sections.serology());
        addSection(second, "Hématologie", sections.hematology());
        second.addCell(plain("", CELL_FONT));
        document.add(second);
    }

    private static void addSection(PdfPTable table, String label, boolean ticked) {
        table.addCell(plain(label, CELL_FONT));
        table.addCell(box(ticked));
    }

    /** The early infant diagnosis form asks for a new DBS card. */
    static PdfPTable conclusion(boolean earlyInfantDiagnosis) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        Paragraph text = new Paragraph();
        text.add(new Phrase("CONCLUSION : ", LABEL_FONT));
        text.add(new Phrase("L’échantillon ne peut être traité ou analysé ce jour.", TEXT_FONT));
        text.add(new Phrase("\nPrière refaire le prélèvement sur : Tube EDTA Tube sec Carte DBS Whatman 903"
                + (earlyInfantDiagnosis ? " X" : ""), TEXT_FONT));
        PdfPCell cell = new PdfPCell(text);
        cell.setPadding(5);
        table.addCell(cell);
        return table;
    }

    private static PdfPCell box(boolean ticked) {
        PdfPCell cell = centred(ticked ? "X" : "");
        cell.setFixedHeight(14);
        return cell;
    }

    private static PdfPCell plain(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private static PdfPCell centred(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, CELL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    /**
     * An order's non-conformities as "sample type:reason" pairs of display keys; a
     * sample type of "-1" stands for all of the order's samples.
     */
    static final class ChecklistMarks {
        private static final Set<String> EDTA_TUBE_TYPES = Set.of("sample.type.edtaTube", "sample.type.Sang",
                "sample.type.Plasma");
        private static final Set<String> DRY_TUBE_TYPES = Set.of("sample.type.dryTube", "sample.type.Serum");

        private final List<String[]> pairs;

        private ChecklistMarks(List<String[]> pairs) {
            this.pairs = pairs;
        }

        static ChecklistMarks of(String allQaEvents) {
            if (GenericValidator.isBlankOrNull(allQaEvents)) {
                return new ChecklistMarks(List.of());
            }
            return new ChecklistMarks(Arrays.stream(allQaEvents.split(";")).map(pair -> pair.split(":", 2))
                    .filter(pair -> pair.length == 2).toList());
        }

        boolean any(String reason) {
            return pairs.stream().anyMatch(pair -> pair[1].equals("qa_event." + reason));
        }

        boolean onEdtaTube(String reason) {
            return on(EDTA_TUBE_TYPES, reason);
        }

        boolean onDryTube(String reason) {
            return on(DRY_TUBE_TYPES, reason);
        }

        private boolean on(Set<String> sampleTypes, String reason) {
            return pairs.stream()
                    .anyMatch(pair -> sampleTypes.contains(pair[0]) && pair[1].equals("qa_event." + reason));
        }
    }

    // notes are stored with <br/> between their lines
    private static void addNotes(PdfPTable table, String heading, String notes) {
        if (notes == null) {
            return;
        }
        Paragraph text = new Paragraph(heading, LABEL_FONT);
        Arrays.stream(notes.split("<br\\s*/?>")).map(String::trim).filter(line -> !line.isEmpty())
                .forEach(line -> text.add(new Phrase("\n" + line, CELL_FONT)));
        PdfPCell cell = new PdfPCell(text);
        cell.setColspan(6);
        cell.setPaddingLeft(12);
        table.addCell(cell);
    }

    private static void addField(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell(labelled(label, value, TEXT_FONT));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4);
        table.addCell(cell);
    }

    private static Phrase labelled(String label, String value, Font valueFont) {
        Phrase phrase = new Phrase(label + ": ", LABEL_FONT);
        phrase.add(new Phrase(Objects.toString(value, ""), valueFont));
        return phrase;
    }

    /** The date the report was printed, at the foot of every page. */
    private static final class ReportDateFooter extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(DateUtil.getCurrentDateAsText(), TEXT_FONT), document.right(), document.bottom() - 24,
                    0);
        }
    }

    /**
     * The laboratory's signature line and the date the report was printed, at the
     * foot of every checklist page.
     */
    private static final class SignatureFooter extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Signature, date (jj/mm/aaaa), et cachet du Laboratoire/Biologiste", TEXT_FONT),
                    document.right(), document.bottom() - 34, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase(DateUtil.getCurrentDateAsText(), TEXT_FONT), document.right(), document.bottom() - 60,
                    0);
        }
    }

    /** The client's signature and transmission lines at the foot of every page. */
    private static final class ClientFooter extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float signatureLine = document.bottom() - 30;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("Nom et signature du client: ____________________", TEXT_FONT), document.left(),
                    signatureLine, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Date de transmission: ____________", TEXT_FONT), document.right(), signatureLine, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("Conserver une copie au secrétariat du laboratoire", TEXT_FONT), document.left(),
                    signatureLine - 16, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Date d'effet: " + DateUtil.getCurrentDateAsText(), TEXT_FONT), document.right(),
                    signatureLine - 16, 0);
        }
    }
}
