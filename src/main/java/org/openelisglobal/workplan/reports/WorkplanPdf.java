package org.openelisglobal.workplan.reports;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.PdfExportSupport;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openpdf.text.Document;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPTable;

/**
 * The printed workplan: one row per test with the order's lab number, and the
 * columns the site configuration turns on. With results on the workplan, the
 * lab number drops its fixed prefix and blank results and technician columns
 * are added.
 */
final class WorkplanPdf {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 14, Font.BOLD);
    private static final Font META_FONT = new Font(Font.HELVETICA, 9);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9);

    private WorkplanPdf() {
    }

    /**
     * @param bySection        list each row's test name; consecutive rows of one
     *                         order show the order's columns once
     * @param patientNameTitle header of a patient name column, or null for none
     */
    static byte[] render(String title, List<TestResultItem> items, boolean bySection, String patientNameTitle) {
        ConfigurationProperties config = ConfigurationProperties.getInstance();
        boolean withResults = !config.isPropertyValueEqual(Property.RESULTS_ON_WORKPLAN, "false");
        boolean subjectNumber = config.isPropertyValueEqual(Property.SUBJECT_ON_WORKPLAN, "true");
        boolean nextVisit = !withResults && config.isPropertyValueEqual(Property.NEXT_VISIT_DATE_ON_WORKPLAN, "true");
        boolean patientName = withResults && patientNameTitle != null;
        String prefix = AccessionNumberUtil.getMainAccessionNumberGenerator().getPrefix();
        int prefixLength = AccessionNumberUtil.getMainAccessionNumberGenerator().getInvarientLength();

        List<String> headers = new ArrayList<>();
        String labNumberTitle = MessageUtil.getContextualMessage("quick.entry.accession.number");
        headers.add(withResults && !GenericValidator.isBlankOrNull(prefix) ? labNumberTitle + " (" + prefix + ")"
                : labNumberTitle);
        if (subjectNumber) {
            headers.add(MessageUtil.getContextualMessage("patient.subject.number"));
        }
        if (patientName) {
            headers.add(patientNameTitle);
        }
        headers.add(MessageUtil.getMessage("report.receptionDate"));
        if (bySection) {
            headers.add(MessageUtil.getMessage("report.testName"));
        }
        if (nextVisit) {
            headers.add(MessageUtil.getMessage("sample.entry.nextVisit.date"));
        }
        if (withResults) {
            headers.add(MessageUtil.getMessage("report.results"));
            headers.add(MessageUtil.getMessage("report.techId"));
        }

        List<List<String>> rows = new ArrayList<>();
        String previousAccession = null;
        for (TestResultItem item : items) {
            boolean sameOrder = bySection && Objects.equals(item.getAccessionNumber(), previousAccession);
            previousAccession = item.getAccessionNumber();
            List<String> row = new ArrayList<>();
            row.add(sameOrder ? "" : labNumber(item.getAccessionNumber(), withResults, prefixLength));
            if (subjectNumber) {
                row.add(sameOrder ? "" : item.getPatientInfo());
            }
            if (patientName) {
                row.add(sameOrder ? "" : item.getPatientName());
            }
            row.add(sameOrder ? "" : item.getReceivedDate());
            if (bySection) {
                row.add(item.getTestName() == null ? "" : item.getTestName().replace("&rarr;", "-->"));
            }
            if (nextVisit) {
                row.add(sameOrder ? "" : item.getNextVisitDate());
            }
            if (withResults) {
                row.add("");
                row.add("");
            }
            rows.add(row);
        }

        List<String> metaLines = new ArrayList<>();
        String labName = config.getPropertyValue(Property.SiteName);
        if (withResults && !GenericValidator.isBlankOrNull(labName)) {
            metaLines.add(labName);
        }
        metaLines.add(DateUtil.getCurrentDateAsText());
        return layout(title, metaLines, headers, rows);
    }

    private static String labNumber(String accessionNumber, boolean withResults, int prefixLength) {
        if (accessionNumber == null) {
            return "";
        }
        return withResults && accessionNumber.length() > prefixLength ? accessionNumber.substring(prefixLength)
                : accessionNumber;
    }

    private static byte[] layout(String title, List<String> metaLines, List<String> headers, List<List<String>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 48);
        PdfExportSupport.openWithPageNumbers(document, out, "report.label.page");
        PdfExportSupport.addHeading(document, title, TITLE_FONT, META_FONT,
                metaLines.stream().map(line -> line + "\n").toArray(String[]::new));
        document.add(new Phrase("\n", META_FONT));

        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        PdfExportSupport.addHeaderRow(table, HEADER_FONT, 4, headers.toArray(String[]::new));
        for (List<String> row : rows) {
            for (String value : row) {
                table.addCell(new Phrase(value == null ? "" : value, CELL_FONT));
            }
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }
}
