package org.openelisglobal.shipment.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.shipment.valueholder.BoxSampleItem;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ManifestPDFService for generating shipping box manifest
 * PDFs
 *
 * @deprecated Backend PDF generation is deprecated. Use frontend PDF generation
 *             via /manifest-data endpoint and pdfGenerator.js instead.
 */
@Deprecated(since = "3.3.x", forRemoval = true)
@Service
public class ManifestPDFServiceImpl implements ManifestPDFService {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    // Fonts
    private static final Font TITLE_FONT = new Font(FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);

    @Autowired
    private ShippingBoxService shippingBoxService;

    @Autowired
    private BoxSampleItemService boxSampleItemService;

    @Override
    @Transactional(readOnly = true)
    public ByteArrayOutputStream generateManifestPDF(String boxId) {
        // Parse boxId as Integer
        Integer id;
        try {
            id = Integer.parseInt(boxId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid box ID format: " + boxId);
        }

        ShippingBox box = shippingBoxService.getBoxById(id);

        if (box == null) {
            throw new IllegalArgumentException("Shipping box not found: " + boxId);
        }

        // Initialize lazy associations within transaction
        if (box.getDestinationFacility() != null) {
            box.getDestinationFacility().getOrganizationName(); // Force initialization
        }
        if (box.getCreatedBy() != null) {
            box.getCreatedBy().getNameForDisplay(); // Force initialization
        }

        // Get all sample items in this box
        List<BoxSampleItem> sampleItems = boxSampleItemService.getBoxSampleItemsByShippingBoxId(id);

        // Initialize sample item associations
        for (BoxSampleItem item : sampleItems) {
            if (item.getSampleItem() != null && item.getSampleItem().getSample() != null) {
                item.getSampleItem().getSample().getAccessionNumber(); // Force initialization
            }
        }

        return generatePDF(box, sampleItems);
    }

    /**
     * Generate PDF manifest document
     */
    private ByteArrayOutputStream generatePDF(ShippingBox box, List<BoxSampleItem> sampleItems) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Title
            String titleText = "Shipping Manifest";
            try {
                String msg = MessageUtil.getMessage("shipment.manifest.title");
                if (msg != null && !msg.isEmpty()) {
                    titleText = msg;
                }
            } catch (Exception ex) {
                // Use default title
            }
            Paragraph title = new Paragraph(titleText, TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Box information section
            document.add(createBoxInfoSection(box));

            // Samples table
            document.add(createSamplesTable(sampleItems));

            // Footer with summary
            document.add(createFooter(box, sampleItems));

            document.close();

            return outputStream;
        } catch (DocumentException e) {
            LogEvent.logError("ManifestPDFServiceImpl", "generatePDF",
                    "Exception during PDF generation: " + e.getClass().getName() + " - " + e.getMessage());
            LogEvent.logError(e);
            throw new RuntimeException("Failed to generate manifest PDF", e);
        }
    }

    /**
     * Create box information section
     */
    private PdfPTable createBoxInfoSection(ShippingBox box) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 30, 70 });
        table.setSpacingAfter(20f);

        // Box ID
        addInfoRow(table, "Box ID:", box.getBoxId() != null ? box.getBoxId() : "-");

        // Destination
        String destination = "-";
        try {
            if (box.getDestinationFacility() != null) {
                destination = box.getDestinationFacility().getOrganizationName();
                if (destination == null)
                    destination = "-";
            }
        } catch (Exception e) {
            LogEvent.logDebug("ManifestPDFServiceImpl", "createBoxInfoSection",
                    "Could not get destination facility name");
        }
        addInfoRow(table, "Destination:", destination);

        // State
        String state = box.getState() != null ? box.getState().toString() : "-";
        addInfoRow(table, "State:", state);

        // Temperature
        String temperature = box.getTemperatureRequirement() != null ? box.getTemperatureRequirement() : "AMBIENT";
        addInfoRow(table, "Temperature:", temperature);

        // Created date
        String createdDate = box.getCreatedDate() != null ? DATE_FORMAT.format(box.getCreatedDate()) : "-";
        addInfoRow(table, "Created:", createdDate);

        // Created by
        String createdBy = "-";
        try {
            if (box.getCreatedBy() != null) {
                createdBy = box.getCreatedBy().getNameForDisplay();
                if (createdBy == null)
                    createdBy = "-";
            }
        } catch (Exception e) {
            LogEvent.logDebug("ManifestPDFServiceImpl", "createBoxInfoSection", "Could not get created by name");
        }
        addInfoRow(table, "Created By:", createdBy);

        return table;
    }

    /**
     * Add a row to the info table
     */
    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_FONT));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPadding(5f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, NORMAL_FONT));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPadding(5f);
        table.addCell(valueCell);
    }

    /**
     * Create samples table
     */
    private PdfPTable createSamplesTable(List<BoxSampleItem> sampleItems) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 10, 30, 30, 30 });
        table.setSpacingAfter(20f);

        // Header row
        addHeaderCell(table, "#");
        addHeaderCell(table, "Accession Number");
        addHeaderCell(table, "Referral Test");
        addHeaderCell(table, "Added Date");

        // Sample rows
        int index = 1;
        for (BoxSampleItem item : sampleItems) {
            addDataCell(table, String.valueOf(index++));

            String accessionNumber = "-";
            try {
                if (item.getSampleItem() != null && item.getSampleItem().getSample() != null) {
                    accessionNumber = item.getSampleItem().getSample().getAccessionNumber();
                    if (accessionNumber == null)
                        accessionNumber = "-";
                }
            } catch (Exception e) {
                LogEvent.logDebug("ManifestPDFServiceImpl", "createSamplesTable", "Could not get accession number");
            }
            addDataCell(table, accessionNumber);

            // For now, we'll show a placeholder for referral test - this would need to be
            // fetched from the sample item's analyses
            String referralTest = "-";
            addDataCell(table, referralTest);

            String addedDate = item.getAddedDate() != null ? DATE_FORMAT.format(item.getAddedDate()) : "-";
            addDataCell(table, addedDate);
        }

        return table;
    }

    /**
     * Add a header cell to table
     */
    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new BaseColor(200, 200, 200));
        cell.setPadding(8f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    /**
     * Add a data cell to table
     */
    private void addDataCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    /**
     * Create footer with summary
     */
    private Paragraph createFooter(ShippingBox box, List<BoxSampleItem> sampleItems) {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(20f);

        // Total samples
        footer.add(new Chunk("Total Sample Items: " + sampleItems.size(), HEADER_FONT));
        footer.add(Chunk.NEWLINE);
        footer.add(Chunk.NEWLINE);

        // Notes
        if (box.getNotes() != null && !box.getNotes().trim().isEmpty()) {
            footer.add(new Chunk("Notes: ", HEADER_FONT));
            footer.add(Chunk.NEWLINE);
            footer.add(new Chunk(box.getNotes(), NORMAL_FONT));
            footer.add(Chunk.NEWLINE);
            footer.add(Chunk.NEWLINE);
        }

        // Generated timestamp
        footer.add(new Chunk("Generated: " + DATE_FORMAT.format(new java.util.Date()), SMALL_FONT));

        return footer;
    }
}
