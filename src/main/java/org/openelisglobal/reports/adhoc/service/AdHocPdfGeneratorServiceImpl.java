package org.openelisglobal.reports.adhoc.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.reports.adhoc.dto.AdHocReportDefinitionDTO;
import org.openelisglobal.reports.adhoc.dto.AdHocReportResultDTO;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.stereotype.Service;

@Service
public class AdHocPdfGeneratorServiceImpl implements AdHocPdfGeneratorService {

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Color HEADER_BG_COLOR = new Color(74, 144, 164);
    private static final Color HEADER_TEXT_COLOR = Color.WHITE;
    private static final Color ALT_ROW_COLOR = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, HEADER_TEXT_COLOR);
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

    @Override
    public byte[] generatePdf(AdHocReportDefinitionDTO definition, AdHocReportResultDTO reportData) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
            PdfWriter.getInstance(document, outputStream);

            document.open();
            addTitle(document, definition);
            addDataTable(document, reportData);
            addFooter(document, reportData);
            document.close();

            LogEvent.logInfo(this.getClass().getSimpleName(), "generatePdf",
                    "Generated PDF report with " + reportData.getRows().size() + " rows");

            return outputStream.toByteArray();

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "generatePdf",
                    "Error generating PDF: " + e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void addTitle(Document document, AdHocReportDefinitionDTO definition) throws Exception {
        String title = definition.getReportTitle();
        if (title == null || title.trim().isEmpty()) {
            title = MessageUtil.getMessage("adhoc.report.default.title");
            if (title == null || title.startsWith("adhoc.")) {
                title = "Ad-Hoc Patient Report";
            }
        }

        Paragraph titlePara = new Paragraph(title, TITLE_FONT);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(5);
        document.add(titlePara);

        String siteName = ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName);
        if (siteName != null && !siteName.trim().isEmpty()) {
            Paragraph sitePara = new Paragraph(siteName, SUBTITLE_FONT);
            sitePara.setAlignment(Element.ALIGN_CENTER);
            sitePara.setSpacingAfter(15);
            document.add(sitePara);
        }
    }

    private void addDataTable(Document document, AdHocReportResultDTO reportData) throws Exception {
        List<AdHocReportResultDTO.ColumnDefinition> columns = reportData.getColumns();
        List<List<Object>> rows = reportData.getRows();

        if (columns.isEmpty()) {
            document.add(new Paragraph("No columns selected", CELL_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        float[] columnWidths = calculateColumnWidths(columns);
        table.setWidths(columnWidths);

        for (AdHocReportResultDTO.ColumnDefinition col : columns) {
            PdfPCell headerCell = new PdfPCell(new Phrase(col.getDisplayName(), HEADER_FONT));
            headerCell.setBackgroundColor(HEADER_BG_COLOR);
            headerCell.setPadding(8);
            headerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerCell.setBorderColor(BORDER_COLOR);
            table.addCell(headerCell);
        }

        int rowIndex = 0;
        for (List<Object> row : rows) {
            Color rowBgColor = (rowIndex % 2 == 0) ? Color.WHITE : ALT_ROW_COLOR;

            for (int i = 0; i < columns.size(); i++) {
                Object cellValue = (i < row.size()) ? row.get(i) : null;
                String cellText = (cellValue != null) ? cellValue.toString() : "";

                PdfPCell dataCell = new PdfPCell(new Phrase(cellText, CELL_FONT));
                dataCell.setBackgroundColor(rowBgColor);
                dataCell.setPadding(6);
                dataCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                dataCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                dataCell.setBorderColor(BORDER_COLOR);
                table.addCell(dataCell);
            }
            rowIndex++;
        }

        if (rows.isEmpty()) {
            PdfPCell noDataCell = new PdfPCell(new Phrase("No data found", CELL_FONT));
            noDataCell.setColspan(columns.size());
            noDataCell.setPadding(10);
            noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            noDataCell.setBackgroundColor(ALT_ROW_COLOR);
            table.addCell(noDataCell);
        }

        document.add(table);
    }

    private float[] calculateColumnWidths(List<AdHocReportResultDTO.ColumnDefinition> columns) {
        float[] widths = new float[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            AdHocReportResultDTO.ColumnDefinition col = columns.get(i);
            String dataType = col.getDataType() != null ? col.getDataType().name() : "STRING";

            if ("DATE".equals(dataType) || "DATETIME".equals(dataType)) {
                widths[i] = 2.0f;
            } else if ("INTEGER".equals(dataType) || "DECIMAL".equals(dataType) || "BOOLEAN".equals(dataType)) {
                widths[i] = 1.0f;
            } else if (col.getFieldId().contains("Name") || col.getFieldId().contains("Reference")) {
                widths[i] = 2.0f;
            } else {
                widths[i] = 1.5f;
            }
        }

        return widths;
    }

    private void addFooter(Document document, AdHocReportResultDTO reportData) throws Exception {
        document.add(new Paragraph(" "));

        PdfPTable separatorTable = new PdfPTable(1);
        separatorTable.setWidthPercentage(100);
        PdfPCell separatorCell = new PdfPCell();
        separatorCell.setBorderWidthTop(1);
        separatorCell.setBorderWidthBottom(0);
        separatorCell.setBorderWidthLeft(0);
        separatorCell.setBorderWidthRight(0);
        separatorCell.setBorderColorTop(Color.GRAY);
        separatorCell.setFixedHeight(5);
        separatorTable.addCell(separatorCell);
        document.add(separatorTable);

        String generatedBy = getCurrentUserName();
        String generatedDate;
        synchronized (DATE_TIME_FORMAT) {
            generatedDate = DATE_TIME_FORMAT.format(new Date());
        }

        Paragraph footerPara = new Paragraph("Generated: " + generatedDate + " by " + generatedBy
                + "  |  Total Records: " + reportData.getTotalCount(), FOOTER_FONT);
        footerPara.setAlignment(Element.ALIGN_LEFT);
        footerPara.setSpacingBefore(5);
        document.add(footerPara);

        Paragraph endPara = new Paragraph("--- End of Report ---",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY));
        endPara.setAlignment(Element.ALIGN_CENTER);
        endPara.setSpacingBefore(15);
        document.add(endPara);
    }

    private String getCurrentUserName() {
        try {
            UserSessionData userSession = SpringContext.getBean(UserSessionData.class);
            if (userSession != null && userSession.getLoginName() != null) {
                return userSession.getLoginName();
            }
        } catch (Exception e) {
            // Return default
        }
        return "System";
    }
}
