package org.openelisglobal.audittrail.controller.rest;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import org.openelisglobal.audittrail.action.workers.AuditTrailItem;
import org.openelisglobal.audittrail.action.workers.AuditTrailViewWorker;
import org.openelisglobal.audittrail.form.AuditTrailViewForm;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AuditTrailReportRestController {

    private static final String ACCESSION_PATTERN = "^[a-zA-Z0-9\\-]+$";

    @GetMapping("/rest/AuditTrailReport")
    public ResponseEntity<AuditTrailViewForm> getAuditTrailReport(@RequestParam String accessionNumber) {
        AuditTrailViewForm response = new AuditTrailViewForm();
        if (!accessionNumber.matches(ACCESSION_PATTERN)) {
            return ResponseEntity.badRequest().build();
        }

        AuditTrailViewWorker worker = SpringContext.getBean(AuditTrailViewWorker.class);
        worker.setAccessionNumber(accessionNumber);
        List<AuditTrailItem> items = worker.getAuditTrail();

        if (items.isEmpty()) {
            return ResponseEntity.ok(response);
        }

        response.setAccessionNumber(accessionNumber);
        response.setLog(items);
        response.setSampleOrderItems(worker.getSampleOrderSnapshot());
        response.setPatientProperties(worker.getPatientSnapshot());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rest/AuditTrailReport/exportCsv")
    public void exportCsv(@RequestParam String accessionNumber, HttpServletResponse response) throws IOException {
        if (!accessionNumber.matches(ACCESSION_PATTERN)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid accession number format");
            return;
        }
        AuditTrailViewWorker worker = SpringContext.getBean(AuditTrailViewWorker.class);
        worker.setAccessionNumber(accessionNumber);
        List<AuditTrailItem> items = worker.getAuditTrail();

        String safeFilename = accessionNumber.replaceAll("[^a-zA-Z0-9\\-]", "");
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"order-audit-trail-" + safeFilename + ".csv\"");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        PrintWriter writer = response.getWriter();
        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n", MessageUtil.getMessage("auditTrail.export.header.timestamp"),
                MessageUtil.getMessage("auditTrail.export.header.action"),
                MessageUtil.getMessage("auditTrail.export.header.user"),
                MessageUtil.getMessage("auditTrail.export.header.item"),
                MessageUtil.getMessage("auditTrail.export.header.identifier"),
                MessageUtil.getMessage("auditTrail.export.header.attribute"),
                MessageUtil.getMessage("auditTrail.export.header.oldValue"),
                MessageUtil.getMessage("auditTrail.export.header.newValue"));
        for (AuditTrailItem item : items) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csvEscape(item.getTimeStamp() != null ? sdf.format(item.getTimeStamp()) : ""),
                    csvEscape(item.getAction()), csvEscape(item.getUser()), csvEscape(item.getItem()),
                    csvEscape(item.getIdentifier()), csvEscape(item.getAttribute()), csvEscape(item.getOldValue()),
                    csvEscape(item.getNewValue()));
        }
        writer.flush();
    }

    @GetMapping("/rest/AuditTrailReport/exportPdf")
    public void exportPdf(@RequestParam String accessionNumber, HttpServletResponse response) throws IOException {
        if (!accessionNumber.matches(ACCESSION_PATTERN)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid accession number format");
            return;
        }
        AuditTrailViewWorker worker = SpringContext.getBean(AuditTrailViewWorker.class);
        worker.setAccessionNumber(accessionNumber);
        List<AuditTrailItem> items = worker.getAuditTrail();

        String safeFilename = accessionNumber.replaceAll("[^a-zA-Z0-9\\-]", "");
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"order-audit-trail-" + safeFilename + ".pdf\"");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 8);

            document.add(new Phrase(MessageUtil.getMessage("auditTrail.export.title.orderAuditTrail") + " - "
                    + accessionNumber + "\n\n", titleFont));

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2.5f, 1.5f, 2f, 2f, 1.5f, 2f, 2f, 2f });

            String[] headers = { MessageUtil.getMessage("auditTrail.export.header.timestamp"),
                    MessageUtil.getMessage("auditTrail.export.header.action"),
                    MessageUtil.getMessage("auditTrail.export.header.user"),
                    MessageUtil.getMessage("auditTrail.export.header.item"),
                    MessageUtil.getMessage("auditTrail.export.header.identifier"),
                    MessageUtil.getMessage("auditTrail.export.header.attribute"),
                    MessageUtil.getMessage("auditTrail.export.header.oldValue"),
                    MessageUtil.getMessage("auditTrail.export.header.newValue") };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(new BaseColor(51, 102, 179));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(4);
                table.addCell(cell);
            }

            for (AuditTrailItem item : items) {
                table.addCell(new Phrase(item.getTimeStamp() != null ? sdf.format(item.getTimeStamp()) : "", cellFont));
                table.addCell(new Phrase(item.getAction() != null ? item.getAction() : "", cellFont));
                table.addCell(new Phrase(item.getUser() != null ? item.getUser() : "", cellFont));
                table.addCell(new Phrase(item.getItem() != null ? item.getItem() : "", cellFont));
                table.addCell(new Phrase(item.getIdentifier() != null ? item.getIdentifier() : "", cellFont));
                table.addCell(new Phrase(item.getAttribute() != null ? item.getAttribute() : "", cellFont));
                table.addCell(new Phrase(item.getOldValue() != null ? item.getOldValue() : "", cellFont));
                table.addCell(new Phrase(item.getNewValue() != null ? item.getNewValue() : "", cellFont));
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            LogEvent.logError(e);
            throw new IOException("Error generating PDF", e);
        }
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        // Prevent CSV formula injection (CWE-1236): prefix dangerous leading chars
        if (!value.isEmpty()) {
            char first = value.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@') {
                value = "'" + value;
            }
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
