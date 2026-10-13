package org.openelisglobal.reports.action.implementation;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.common.util.PdfReportLayout;
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

/**
 * The header of the code-built reports: the site's left and right logos, its
 * name lines and the report title, then the laboratory manager when one is
 * configured. Everything comes from site information.
 */
final class ReportHeaderPdf {

    private static final Font NAME_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font MANAGER_FONT = new Font(Font.HELVETICA, 9);
    private static final float LOGO_SIZE = 70;
    private static final float ACCREDITATION_LOGO_SIZE = 30;

    private ReportHeaderPdf() {
    }

    /**
     * Opens the document, with page numbers when the site's report settings ask for
     * them. A report adds its own page events to the writer returned.
     */
    static PdfWriter open(Document document, OutputStream out) {
        if ("true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_PAGE_NUMBERS_ON_REPORTS))) {
            return PdfExportSupport.openWithPageNumbers(document, out, "report.label.page");
        }
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();
        return writer;
    }

    /** The site name and its additional information, leaving out blank ones. */
    static List<String> siteNameLines() {
        ConfigurationProperties config = ConfigurationProperties.getInstance();
        List<String> lines = new ArrayList<>();
        for (Property property : new Property[] { Property.SiteName, Property.ADDITIONAL_SITE_INFO }) {
            String line = config.getPropertyValue(property);
            if (!GenericValidator.isBlankOrNull(line)) {
                lines.add(line);
            }
        }
        return lines;
    }

    static void add(Document document, String title, List<String> nameLines) {
        add(document, title, nameLines, List.of(), null);
    }

    /**
     * As above, with the accreditation logos and notes line (OGC-686) under the
     * title when the report's tests qualify.
     */
    static void add(Document document, String title, List<String> nameLines, List<byte[]> accreditationLogos,
            String accreditationNotesLine) {
        document.add(table(title, nameLines, accreditationLogos, accreditationNotesLine));
    }

    static PdfWriter openRepeating(Document document, OutputStream out, String title, List<String> nameLines,
            String... metaLines) {
        return openRepeatingWithFooter(document, out, title, nameLines, metaLines, new String[0]);
    }

    static PdfWriter openRepeatingWithFooter(Document document, OutputStream out, String title, List<String> nameLines,
            String[] metaLines, String... footerLines) {
        PdfPTable header = new PdfPTable(1);
        PdfPCell site = new PdfPCell(table(title, nameLines, List.of(), null));
        site.setBorder(Rectangle.NO_BORDER);
        header.addCell(site);
        for (String line : metaLines) {
            PdfReportLayout.addLine(header, line, MANAGER_FONT);
        }
        boolean numbers = "true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_PAGE_NUMBERS_ON_REPORTS));
        return PdfReportLayout.open(document, out, header, numbers, footerLines);
    }

    private static PdfPTable table(String title, List<String> nameLines, List<byte[]> accreditationLogos,
            String accreditationNotesLine) {
        PdfPTable header = new PdfPTable(new float[] { 1, 4, 1 });
        header.setWidthPercentage(100);
        header.addCell(logo("headerLeftImage", Element.ALIGN_LEFT));
        Paragraph centre = new Paragraph();
        for (String line : nameLines) {
            centre.add(new Phrase(line + "\n", NAME_FONT));
        }
        centre.add(new Phrase(title, TITLE_FONT));
        centre.setAlignment(Element.ALIGN_CENTER);
        for (byte[] logo : accreditationLogos) {
            try {
                Image image = Image.getInstance(logo);
                image.scaleToFit(ACCREDITATION_LOGO_SIZE, ACCREDITATION_LOGO_SIZE);
                centre.add(new Chunk(image, 0, 0, true));
                centre.add(new Chunk(" "));
            } catch (Exception e) {
                LogEvent.logError(ReportHeaderPdf.class.getSimpleName(), "add",
                        "Unreadable accreditation logo: " + e.getMessage());
            }
        }
        if (!GenericValidator.isBlankOrNull(accreditationNotesLine)) {
            centre.add(new Phrase("\n" + accreditationNotesLine, MANAGER_FONT));
        }
        PdfPCell centreCell = new PdfPCell();
        centreCell.addElement(centre);
        centreCell.setBorder(Rectangle.NO_BORDER);
        centreCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.addCell(centreCell);
        header.addCell(logo("headerRightImage", Element.ALIGN_RIGHT));

        String director = ConfigurationProperties.getInstance().getPropertyValue(Property.labDirectorName);
        if (!GenericValidator.isBlankOrNull(director)) {
            PdfPCell manager = new PdfPCell(
                    new Phrase(MessageUtil.getMessage("report.labManager") + ": " + director, MANAGER_FONT));
            manager.setColspan(3);
            manager.setBorder(Rectangle.NO_BORDER);
            header.addCell(manager);
        }
        return header;
    }

    private static PdfPCell logo(String siteInfoName, int alignment) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        Optional<org.openelisglobal.image.valueholder.Image> stored = SpringContext.getBean(ImageService.class)
                .getImageBySiteInfoName(siteInfoName);
        if (stored.isPresent() && stored.get().getImage() != null) {
            try {
                Image image = Image.getInstance(stored.get().getImage());
                image.scaleToFit(LOGO_SIZE, LOGO_SIZE);
                image.setAlignment(alignment);
                cell.addElement(image);
            } catch (Exception e) {
                LogEvent.logError(ReportHeaderPdf.class.getSimpleName(), "logo",
                        "Unreadable " + siteInfoName + ": " + e.getMessage());
            }
        }
        return cell;
    }
}
