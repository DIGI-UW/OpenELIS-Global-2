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
import org.openelisglobal.shipment.valueholder.BoxSample;
import org.openelisglobal.shipment.valueholder.ShippingBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of ManifestPDFService for generating shipping box manifest
 * PDFs
 */
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
    private BoxSampleService boxSampleService;

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

        // Get all samples in this box
        List<BoxSample> samples = boxSampleService.getBoxSamplesByShippingBoxId(id);

        return generatePDF(box, samples);
    }

    /**
     * Generate PDF manifest document
     */
    private ByteArrayOutputStream generatePDF(ShippingBox box, List<BoxSample> samples) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Title
            Paragraph title = new Paragraph(MessageUtil.getMessage("shipment.manifest.title", "Shipping Manifest"),
                    TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Box information section
            document.add(createBoxInfoSection(box));

            // Samples table
            document.add(createSamplesTable(samples));

            // Footer with summary
            document.add(createFooter(box, samples));

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
        addInfoRow(table, MessageUtil.getMessage("shipment.box.id", "Box ID:"), box.getBoxId());

        // Destination
        String destination = box.getDestinationFacility() != null ? box.getDestinationFacility().getOrganizationName()
                : "-";
        addInfoRow(table, MessageUtil.getMessage("shipment.box.destination", "Destination:"), destination);

        // State
        String state = box.getState() != null ? box.getState().toString() : "-";
        addInfoRow(table, MessageUtil.getMessage("shipment.box.state", "State:"), state);

        // Temperature
        String temperature = box.getTemperatureRequirement() != null ? box.getTemperatureRequirement() : "AMBIENT";
        addInfoRow(table, MessageUtil.getMessage("shipment.box.temperature", "Temperature:"), temperature);

        // Created date
        String createdDate = box.getCreatedDate() != null ? DATE_FORMAT.format(box.getCreatedDate()) : "-";
        addInfoRow(table, MessageUtil.getMessage("shipment.box.created", "Created:"), createdDate);

        // Created by
        String createdBy = box.getCreatedBy() != null ? box.getCreatedBy().getNameForDisplay() : "-";
        addInfoRow(table, MessageUtil.getMessage("shipment.box.createdBy", "Created By:"), createdBy);

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
    private PdfPTable createSamplesTable(List<BoxSample> samples) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[] { 10, 30, 30, 30 });
        table.setSpacingAfter(20f);

        // Header row
        addHeaderCell(table, MessageUtil.getMessage("shipment.manifest.number", "#"));
        addHeaderCell(table, MessageUtil.getMessage("sample.label.accessionNumber", "Accession Number"));
        addHeaderCell(table, MessageUtil.getMessage("shipment.label.referralTest", "Referral Test"));
        addHeaderCell(table, MessageUtil.getMessage("shipment.label.addedDate", "Added Date"));

        // Sample rows
        int index = 1;
        for (BoxSample boxSample : samples) {
            addDataCell(table, String.valueOf(index++));

            String accessionNumber = boxSample.getSample() != null ? boxSample.getSample().getAccessionNumber() : "-";
            addDataCell(table, accessionNumber);

            // For now, we'll show a placeholder for referral test - this would need to be
            // fetched from the sample's analyses
            String referralTest = "-";
            addDataCell(table, referralTest);

            String addedDate = boxSample.getAddedDate() != null ? DATE_FORMAT.format(boxSample.getAddedDate()) : "-";
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
    private Paragraph createFooter(ShippingBox box, List<BoxSample> samples) {
        Paragraph footer = new Paragraph();
        footer.setSpacingBefore(20f);

        // Total samples
        footer.add(
                new Chunk(MessageUtil.getMessage("shipment.manifest.totalSamples", "Total Samples: ") + samples.size(),
                        HEADER_FONT));
        footer.add(Chunk.NEWLINE);
        footer.add(Chunk.NEWLINE);

        // Notes
        if (box.getNotes() != null && !box.getNotes().trim().isEmpty()) {
            footer.add(new Chunk(MessageUtil.getMessage("shipment.box.notes", "Notes: "), HEADER_FONT));
            footer.add(Chunk.NEWLINE);
            footer.add(new Chunk(box.getNotes(), NORMAL_FONT));
            footer.add(Chunk.NEWLINE);
            footer.add(Chunk.NEWLINE);
        }

        // Generated timestamp
        footer.add(new Chunk(MessageUtil.getMessage("shipment.manifest.generated", "Generated: ")
                + DATE_FORMAT.format(new java.util.Date()), SMALL_FONT));

        return footer;
    }
}
