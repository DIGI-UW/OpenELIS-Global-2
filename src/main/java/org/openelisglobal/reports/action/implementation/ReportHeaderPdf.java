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
import org.openelisglobal.image.service.ImageService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.spring.util.SpringContext;
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

    private ReportHeaderPdf() {
    }

    /**
     * Opens the document, with page numbers when the site's report settings ask for
     * them.
     */
    static void open(Document document, OutputStream out) {
        if ("true"
                .equals(ConfigurationProperties.getInstance().getPropertyValue(Property.USE_PAGE_NUMBERS_ON_REPORTS))) {
            PdfExportSupport.openWithPageNumbers(document, out, "report.label.page");
        } else {
            PdfWriter.getInstance(document, out);
            document.open();
        }
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
        PdfPTable header = new PdfPTable(new float[] { 1, 4, 1 });
        header.setWidthPercentage(100);
        header.addCell(logo("headerLeftImage", Element.ALIGN_LEFT));
        Paragraph centre = new Paragraph();
        for (String line : nameLines) {
            centre.add(new Phrase(line + "\n", NAME_FONT));
        }
        centre.add(new Phrase(title, TITLE_FONT));
        centre.setAlignment(Element.ALIGN_CENTER);
        PdfPCell centreCell = new PdfPCell();
        centreCell.addElement(centre);
        centreCell.setBorder(Rectangle.NO_BORDER);
        centreCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        header.addCell(centreCell);
        header.addCell(logo("headerRightImage", Element.ALIGN_RIGHT));
        document.add(header);

        String director = ConfigurationProperties.getInstance().getPropertyValue(Property.labDirectorName);
        if (!GenericValidator.isBlankOrNull(director)) {
            document.add(new Paragraph(MessageUtil.getMessage("report.labManager") + ": " + director, MANAGER_FONT));
        }
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
